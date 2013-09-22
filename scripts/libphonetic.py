#!/usr/bin/env python2
# -*- coding: utf-8 -*-
# $File: libphonetic.py
# $Date: Sun Sep 22 22:01:54 2013 +0800
# $Author: jiakai <jia.kai66@gmail.com>

import gevent
import gevent.monkey
gevent.monkey.patch_all()

from gevent.pool import Pool

import sqlite3

import sys
import urllib2
import re
import os
import os.path

DATADIR = os.path.join(os.path.dirname(__file__), 'phonetic')
RE_PHONETIC = [
        re.compile(ur'美\s*<span class="phonetic".*\[([^\]]*)\]'),
        re.compile(ur'英\s*<span class="phonetic".*\[([^\]]*)\]'),
        re.compile(ur'<span class="phonetic".*\[([^\]]*)\]')]

class FailLog(object):
    fout = None
    def __init__(self):
        ensure_dir(DATADIR)
        self.fout = open(os.path.join(DATADIR, 'fail.txt'), 'w')

    def write(self, word):
        self.fout.write(word + '\n')
        self.fout.flush()

def ensure_dir(path):
    if not os.path.isdir(path):
        os.makedirs(path)


def urldownload(url):
    return urllib2.urlopen(url).read()

write_fail = FailLog().write

def fetchdata(word, fout_audio):
    print 'fetching phonetic data', word
    webpage = \
            urldownload('http://dict.youdao.com/search?q={}'.format(word)).decode('utf-8')
    rst = None
    for i in RE_PHONETIC:
        match = i.search(webpage)
        if match is not None:
            rst = match.group(1)
            break
    if rst is None:
        rst = ''
    with open(fout_audio, 'wb') as fout:
        fout.write(urldownload(
            'http://dict.youdao.com/dictvoice?type=2&audio={}'.format(word)))
    return rst

class LibPhonetic(object):
    sql_conn = None

    def __init__(self):
        ensure_dir(DATADIR)
        dbfile = os.path.join(DATADIR, 'text.db')
        need_init = not os.path.isfile(dbfile)
        self.sql_conn = sqlite3.connect(dbfile)
        if need_init:
            self.init_db()

    def init_db(self):
        c = self.sql_conn.cursor()
        c.execute('CREATE TABLE `phonetic` (`word` TEXT, `phonetic` TEXT)')
        c.execute('CREATE UNIQUE INDEX `word_idx` ON `phonetic` (`word`)')

    def get_phonetic(self, word):
        """:return: <phonetic text>, <path to audio file>, or None if no data"""
        odir = os.path.join(DATADIR, word[0])
        ensure_dir(odir)
        c = self.sql_conn.cursor()
        rst = c.execute('SELECT `phonetic` FROM `phonetic` WHERE `word`=?',
                (word, )).fetchone()
        audio = os.path.join(odir, '{}.mp3'.format(word))
        if rst:
            return rst[0], audio
        text = fetchdata(word, audio)
        if not text:
            return
        c.execute('INSERT INTO `phonetic` VALUES (?, ?)', (word, text))
        self.sql_conn.commit()
        return text, audio


get_phonetic = LibPhonetic().get_phonetic

if __name__ == '__main__':
    if len(sys.argv) != 2:
        sys.exit('usage: {} <.json wordlist>'.format(sys.argv[0]))

    pool = Pool(100)
    with open(sys.argv[1]) as fin:
        for i in fin.readlines():
            pool.spawn(get_phonetic, i.strip())

    pool.join()

