#!/bin/bash
set -o pipefail
set -e
set -x

trap 'rm -f /tmp/test*.csv spreadcalc' EXIT

go run spreadcalc.go < provided/testinput1.csv > /tmp/testoutput1.csv
diff -u /tmp/testoutput1.csv provided/testoutput1.csv

go run spreadcalc.go < provided/testinput2.csv > /tmp/testoutput2.csv
diff -u /tmp/testoutput2.csv provided/testoutput2.csv

go run spreadcalc.go < provided/testinput3.csv > /tmp/testoutput3.csv
diff -u /tmp/testoutput3.csv provided/testoutput3.csv

go run spreadcalc.go < provided/testinput4.csv > /tmp/testoutput4.csv
diff -u /tmp/testoutput4.csv provided/testoutput4.csv

echo 'success!'
