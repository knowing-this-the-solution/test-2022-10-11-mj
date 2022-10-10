#!/bin/bash
set -o pipefail
set -e
set -x

CC=clang++
if [ -f /usr/bin/g++ ]; then
    CC=g++
fi

trap 'rm -f /tmp/test*.csv spreadcalc' EXIT

$CC -std=c++17 -o spreadcalc spreadcalc.cpp -lm

./spreadcalc < provided/testinput1.csv > /tmp/testoutput1.csv
diff -u /tmp/testoutput1.csv provided/testoutput1.csv

./spreadcalc < provided/testinput2.csv > /tmp/testoutput2.csv
diff -u /tmp/testoutput2.csv provided/testoutput2.csv

./spreadcalc < provided/testinput3.csv > /tmp/testoutput3.csv
diff -u /tmp/testoutput3.csv provided/testoutput3.csv

./spreadcalc < provided/testinput4.csv > /tmp/testoutput4.csv
diff -u /tmp/testoutput4.csv provided/testoutput4.csv

echo 'success!'

