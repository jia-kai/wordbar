#!/usr/bin/env python2
# -*- coding: utf-8 -*-
# $File: mkaudio.py
# $Date: Sun Sep 22 21:47:10 2013 +0800
# $Author: jiakai <jia.kai66@gmail.com>

from libphonetic import get_phonetic

from scipy.io import wavfile
import numpy as np

import json
import tempfile
import os
import sys
import re

def system_with_exc(cmd):
    if os.system(cmd):
        raise RuntimeError('failed to exec {}'.format(cmd))

class AudioMaker(object):
    TTS_BAD_CHAR_RE = re.compile('[a-zA-Z,.]+')
    temp_wav = None
    samplerate = 22050
    normalize_var = 2000

    def __init__(self, wordlist, fpath_map, fpath_audio):
        fd, self.temp_wav = tempfile.mkstemp(suffix = '.wav')
        os.close(fd)
        sep = np.zeros((self.samplerate * 0.2,), dtype = 'int16')
        audio = np.zeros((0, ), dtype = 'int16')
        audio_map = dict()  # word => (start time, end time)
        for word_text, definition_text in wordlist:
            print word_text
            word = self.load_word_audio(word_text)
            definition = self.run_tts(definition_text)
            cur = np.concatenate([word, sep, definition])
            start = len(audio)
            audio.resize(len(audio) + len(cur))
            audio[-len(cur):] = cur
            audio_map[word_text] = (start / float(self.samplerate),
                    len(audio) / float(self.samplerate))
        wavfile.write(fpath_audio, self.samplerate, audio)

        with open(fpath_map, 'w') as fout:
            fout.write(json.dumps(audio_map))


    def __del__(self):
        os.unlink(self.temp_wav)

    def load_word_audio(self, word):
        text, fpath = get_phonetic(word)
        system_with_exc('mpg123 -q --single0 -r {} -w {} {}'.format(
            self.samplerate, self.temp_wav, fpath))
        return self.load_temp_wav()

    def load_temp_wav(self):
        fs, data = wavfile.read(self.temp_wav)
        assert len(data.shape) == 1 and fs == self.samplerate
        data -= np.average(data)
        return data * (self.normalize_var / np.sqrt(np.var(data)))

    def run_tts(self, text):
        text = u''.join(self.TTS_BAD_CHAR_RE.split(text))
        text = text.replace('\n', ' ')
        system_with_exc(u'espeak -v zh "{}" -w {} 2>/dev/null'.format(
            text, self.temp_wav).encode('utf-8'))
        return self.load_temp_wav()


if __name__ == '__main__':
    if len(sys.argv) != 4:
        sys.exit('usage: {} <.json wordlist> <output audio map> <output audio file>'
                .format(sys.argv[0]))

    wordlist = list()
    with open(sys.argv[1]) as fin:
        for i in fin.readlines():
            o = json.loads(i)
            wordlist.append((o['spell'], o['def']))

    AudioMaker(wordlist, sys.argv[2], sys.argv[3])
