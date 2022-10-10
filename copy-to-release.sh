#!/bin/bash
if [ -z "$1" ]; then
    echo "Usage: copy-to-release.sh path/to/github/checkout"
    exit 1
fi
if [ -d "$1/challenge" ] || [ -f "$1/INTERVIEWER-NOTES.md" ]; then
    echo "Do not release the interviewer notes / test files on github."
    exit 1
fi
cp -pRdv README.md run*.sh spreadcalc* SpreadCalc* provided "$1"
echo "Files copied -- time to make a commit and push to github from $1"
