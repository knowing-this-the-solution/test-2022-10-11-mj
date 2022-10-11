#!/bin/bash
set -o pipefail
set -e
set -x

# PLEASE FOLLOW INSTRUCTIONS AT https://docs.scala-lang.org/getting-started/index.html
# to setup scala on your laptop

SCALA=scala

trap 'rm -f /tmp/test*.csv' EXIT

$SCALA SpreadCalc.scala provided/testinput1.csv > /tmp/testoutput1.csv
diff -u /tmp/testoutput1.csv provided/testoutput1.csv

$SCALA SpreadCalc.scala  provided/testinput2.csv > /tmp/testoutput2.csv
diff -u /tmp/testoutput2.csv provided/testoutput2.csv

$SCALA SpreadCalc.scala provided/testinput3.csv > /tmp/testoutput3.csv
diff -u /tmp/testoutput3.csv provided/testoutput3.csv

$SCALA SpreadCalc.scala provided/testinput4.csv > /tmp/testoutput4.csv
diff -u /tmp/testoutput4.csv provided/testoutput4.csv

echo 'success!'

