#!/usr/bin/env python2
# -*- coding: utf-8 -*-
# $File: libtts.py
# $Date: Mon Sep 23 19:03:05 2013 +0800
# $Author: jiakai <jia.kai66@gmail.com>

__all__ = ['run_tts']

import gevent
import gevent.monkey
gevent.monkey.patch_all()

from gevent.pool import Pool

import urllib
import urllib2
import sys
import os.path
import hashlib
import os

def run_actual_tts(text):
    print 'fetching online:', text
    data = urllib.urlencode({'ie': 'UTF-8',
        'tl': 'zh-CN', 'q': text})
    request = urllib2.Request('https://translate.google.com/translate_tts?' +
            data)
    request.add_header('User-Agent', 'Mozilla/4.0 (X11; Linux x86_64) ')

    return urllib2.urlopen(request).read()


def run_tts(text):
    """:return: path to .mp3 speech file"""
    if isinstance(text, unicode):
        text = text.encode('utf-8')
    m = hashlib.md5()
    m.update(text)
    md5 = m.hexdigest()
    odir = os.path.join(os.path.dirname(__file__), 'tts', md5[:2])
    if not os.path.isdir(odir):
        os.makedirs(odir)
    ofile = os.path.join(odir, md5 + '.mp3')
    if os.path.isfile(ofile):
        return ofile
    data = run_actual_tts(text)
    with open(ofile, 'wb') as fout:
        fout.write(data)
    return ofile


if __name__ == '__main__':
    if len(sys.argv) != 2:
        sys.exit('usage: {} <sentence list>'.format(sys.argv[0]))

    pool = Pool(1)
    with open(sys.argv[1]) as fin:
        for i in fin.readlines():
            pool.spawn(run_tts, i.strip())

    pool.join()

