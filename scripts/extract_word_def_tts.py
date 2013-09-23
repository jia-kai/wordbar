#!/usr/bin/env python2
# -*- coding: utf-8 -*-
# $File: extract_word_def_tts.py
# $Date: Mon Sep 23 13:27:38 2013 +0800
# $Author: jiakai <jia.kai66@gmail.com>

from mkaudio import AudioMaker

import sys
import json

if __name__ == '__main__':
    if len(sys.argv) != 2:
        sys.exit('usage: {} <.json wordlist>'.format(sys.argv[0]))
    with open(sys.argv[1]) as fin:
        for i in fin.readlines():
            i = json.loads(i)
            print AudioMaker.get_tts_text(i['def']).encode('utf-8')


