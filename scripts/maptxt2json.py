#!/usr/bin/env python2
# -*- coding: utf-8 -*-
# $File: maptxt2json.py
# $Date: Mon Sep 23 14:56:55 2013 +0800
# $Author: jiakai <jia.kai66@gmail.com>

import json
import sys

if __name__ == '__main__':
    if len(sys.argv) != 2:
        sys.exit('usage: {} <.txt word map file>'.format(sys.argv[0]))
    rst = list()
    with open(sys.argv[1]) as fin:
        for i in fin.readlines():
            i = i.split()
            word = i[0]
            data = map(int, i[1:])
            rst.append([word] + data)

    print json.dumps(rst)
