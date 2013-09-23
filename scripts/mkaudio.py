#!/usr/bin/env python2
# -*- coding: utf-8 -*-
# $File: mkaudio.py
# $Date: Mon Sep 23 11:16:38 2013 +0800
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
    TTS_RE_REMOVE = [re.compile(ur'[（(][^)）]*[)）]')]
    TTS_RE_SPACE = [re.compile(u'[\x01-~]+'), re.compile(u'\s+')]
    temp_wav = None
    temp_ogg = None
    samplerate = 22050
    normalize_var = 2000

    def __init__(self, wordlist, fpath_map, fpath_audio):
        fd, self.temp_wav = tempfile.mkstemp(suffix = '.wav')
        os.close(fd)
        fd, self.temp_ogg = tempfile.mkstemp(suffix = '.ogg')
        os.close(fd)
        sep = np.zeros((self.samplerate * 0.2,), dtype = 'int16')
        audio_map = dict()
        # word => (begin offset, length, duration in milliseconds)
        faudio_tot_len = 0
        with open(fpath_audio, 'w') as faudio:
            for word_text, definition_text in wordlist:
                print word_text
                word = self.load_word_audio(word_text)
                definition = self.run_tts(definition_text)
                audio_raw = np.concatenate([word, sep, definition])
                wavfile.write(self.temp_wav, self.samplerate, audio_raw)
                system_with_exc('oggenc {} -q -1 --resample 11000 -Q -o {}'.format(
                    self.temp_wav, self.temp_ogg))

                with open(self.temp_ogg) as fin:
                    audio = fin.read()
                faudio.write(audio)
                audio_map[word_text] = (faudio_tot_len, len(audio),
                        int(len(audio_raw) / float(self.samplerate) * 1000))
                faudio_tot_len += len(audio)

        with open(fpath_map, 'w') as fout:
            audio_map = [(i, j) for i, j in audio_map.iteritems()]
            audio_map.sort()
            for i, j in audio_map:
                print >>fout, i, j[0], j[1], j[2]


    def __del__(self):
        #os.unlink(self.temp_wav)
        os.unlink(self.temp_ogg)

    def load_word_audio(self, word):
        text, fpath = get_phonetic(word)
        system_with_exc('mpg123 -q --single0 -r {} -w {} {}'.format(
            self.samplerate, self.temp_wav, fpath))
        return self.load_temp_wav()

    def load_temp_wav(self):
        fs, data = wavfile.read(self.temp_wav)
        assert len(data.shape) == 1 and fs == self.samplerate and len(data)
        data -= np.average(data)
        assert np.var(data)
        return np.int16(data * (self.normalize_var / np.sqrt(np.var(data))))

    @classmethod
    def get_tts_text(cls, text):
        for i in cls.TTS_RE_REMOVE:
            text = i.sub('', text)
        for i in cls.TTS_RE_SPACE:
            text = i.sub(' ', text)
        return text.strip().replace(' ', u'；')

    def run_tts(self, text):
        text = self.get_tts_text(text)
        assert text
        print text
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
