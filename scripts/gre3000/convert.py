#!/usr/bin/env python2
# -*- coding: utf-8 -*-
# $File: convert.py
# $Date: Mon Sep 23 13:25:24 2013 +0800
# $Author: jiakai <jia.kai66@gmail.com>

import re
import sys
import contextlib
import json
import os.path
import sys

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
from libphonetic import get_phonetic


class Loader(object):
    word_re = re.compile(ur'^([a-z-]+)\s+\[.*\].*$', re.MULTILINE  | re.UNICODE)
    single_definition_re = re.compile(ur'【考[^】]*】\s*(.*)$', re.MULTILINE)
    example_re = re.compile(ur'【例】(.*)$', re.MULTILINE)
    english_re = re.compile(ur"([a-zA-Z]+[a-zA-Z ;:,./()-_=+#$%，。'’]*)")

    word_list = None
    # dict word => definition

    brief_word_list = None
    # dict word => brief definition

    def __init__(self, text):
        parts = self.word_re.split(text)
        self.word_list = dict()
        self.brief_word_list = dict()
        for i, j in self.extrac_re_split_pair(parts):
            brief, detail = self.extract_definition(i, j)
            self.word_list[i] = detail
            self.brief_word_list[i] = brief

        if False:
            for i, j in self.word_list.iteritems():
                print i, j
                print '==='
            for i, j in self.brief_word_list.iteritems():
                print i, j
                print '==='
        print 'read {} words'.format(len(self.word_list))

    def extrac_re_split_pair(self, re_split_rst, start = 1):
        return [(re_split_rst[i], re_split_rst[i + 1])
                for i in range(start, len(re_split_rst) - 1, 2)]

    def extract_definition(self, word, text):
        """:return: brief, detailed"""
        text = u' '.join(text.split('\n'))
        text = u'\n【'.join(text.split(u'【'))
        to_remove_in_chn = re.compile(ur'[：；\s]+')
        def split_lexical_category(s):
            s = s.strip()
            p = 0
            while ord(s[p]) in range(0, 128):
                p += 1
            return s[:p], s[p:]

        brief = list()
        detail = list()
        parts = self.single_definition_re.split(text)
        idx = 0
        for idx, (definition, extra) in \
                enumerate(self.extrac_re_split_pair(
                    self.single_definition_re.split(text))):
            lex_cat, definition = split_lexical_category(definition)
            chn = list()
            eng = list()
            for i, j in self.extrac_re_split_pair(
                    self.english_re.split(definition), 0):
                i = u''.join(to_remove_in_chn.split(i))
                if not i:
                    continue
                j = j.replace(u'，', ',').replace(u'。', '.')
                j = j.strip()
                j = re.sub(r'\s+', ' ', j)
                chn.append(i)
                eng.append(j)
            chn = u'；'.join(chn)
            eng = u';'.join(eng)
            brief.append(lex_cat + chn)
            cur_detail = u'[{}:{}]{}'.format(idx + 1, chn, eng)
            s = self.example_re.search(extra)
            if s:
                s = s.group(1).strip()
                s = re.sub(r'\s+', ' ', s)
                cur_detail += u' 例:{}'.format(s)
            detail.append(cur_detail)

        if idx > 10:
            print u'warning: {} definitions'.format(idx)

        return u' '.join(brief), \
                u'\n'.join([u'[{}]'.format(get_phonetic(word)[0])] +
                        brief + detail)


def save_as_json(wordlist, fout):
    lst = list(wordlist.iteritems())
    lst.sort()
    for spell, definition in lst:
        fout.write(json.dumps({"spell": spell, "def": definition}) +
                "\n")

def save_as_youdao(wordlist, fout):
    def w(s):
        if not isinstance(s, basestring):
            s = str(s)
        elif isinstance(s, unicode):
            s = s.encode('utf-8')
        print >> fout, s

    @contextlib.contextmanager
    def tag(name):
        w(u'<{}>'.format(name))
        yield
        w(u'</{}>'.format(name))

    with tag('wordbook'):
        for word, definition in word_list.iteritems():
            with tag('item'):
                with tag('word'):
                    w(word)
                with tag('trans'):
                    w(u'<![CDATA[{}]]>'.format(definition))
                    

if __name__ == '__main__':
    if len(sys.argv) != 4:
        sys.exit('usage: {} <book text> <output file> <brief output file>'.
                format(sys.argv[0], sys.argv[1]))
    with open(sys.argv[1]) as fin:
        text = fin.read().decode('utf-8')
    l = Loader(text)
    with open(sys.argv[2], 'w') as fout:
        save_as_json(l.word_list, fout)
    with open(sys.argv[3], 'w') as fout:
        save_as_json(l.brief_word_list, fout)

