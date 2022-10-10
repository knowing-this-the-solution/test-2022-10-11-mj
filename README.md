Simple Calculation Tool
=======================

The goal is to read a comma-separated values file, describing a simple set of
formulas (like a very stripped-down spreadsheet,) evaluate the cells, and print
out the result of the evaluations as a new CSV file.

This challenge is intended to contain a lot of sub-steps, and the goal is to
get as far as you can, rather than to finish all of them. Leave a comment for
what remains to do when you reach the end of the time. The exercise is about
writing normal, working, code, for a normal, working input -- there are no
"tricks" or "gotchas," and the problem is not intended to require any special
deep insight or "a-ha!" moment. Just solve the problem as you would solve a
general programming problem for yourself, given the time constraints. If there
are questions or ambiguities, document your question and what you chose the
answer to be, and keep going.

Input File Format
-----------------

The file format of the file to read is an ASCII text file (or, equivalently, a
valid UTF-8 file without BOM marker.) The file is divided into rows, which are
divided into cells.

Each cell is guaranteed to not contain the ',' character nor the newline
character. Each column is separated with the ',' character, and each row is
terminated with the newline character. Each cell may have leading or trailing
whitespace, that should be trimmed on reading and not be conidered part of the
cell contents.

Each cell contains one of four kinds of values:

1. Empty cell. This cell contains no characters, other than perhaps whitespace.
   This example contains one row with three empty cells:

    ,,

2. A number literal. This is a valid floating point value, rendered in US
   locale (decimal point, not comma, and possible exponents.) Each number will
   fit and be well behaved in a double precision floating point variable. This
   example contains one row with three literals that all evaluate to one:

    1,1.000,10e-1

3. A string literal. This cell starts with a single-quote `'` character. This
   row contains three string literals named `One`, `Two` and `Three`:

    'One,'Two,'Three

4. A formula. This cell starts with an equals sign `=` and contains a valid
   formula (see below.) This row contains three formulas:

    =A1,=2+5,=B5/(1+C4)

Columns are numbered from A on the left, followed by B, C, ... Z. Input is not
allowed to have a column greater than Z. Column numbers are all uppercase.

Rows are numbered from 1, 2, ... 99. Input is not allowed to have a row greater
than 99, or a row less than 1.

Output File Format
------------------

The output file should similarly be a CSV file, using the same conventions as
input, with some additional requirements:

* Surrounding whitespace should be trimmed.
* Formulas should be printed as the result of their evaluation as a number.
* Numbers should be printed with the result of the %.02f formatter in C, go, or
  Java.

Formula Evaluation
------------------

Terms in a formula are either cell references (like `A1` for the top-left
cell,) or numbers. Formulas support the four basic arithmetic operators, `+`,
`-`, `*`, `/` with standard precedence. Formulas also support parentheses for
changing order of evaluation. Formulas can reference cells that contain other
formulas.

Program Compilation and Invocation
----------------------------------

The program should be written in one single source file, using either C++17
with GCC, or go (version 1.16 or 1.17.) (Versions are current as of October
2021.)

The program can use any part of the standard library for each language, but
should not use any third party modules. "apt install" or "go get" or similar
should not be required to build and run the program. There are sample test
scripts included that would build and run an executable and test it against the
provided test cases; it's OK to change these scripts to add/remove appropriate
compiler options.

The program should read the input CSV file from the standard input, and should
evaluate and format the output CSV file to standard output, and terminate
normally. If there is an error during execution, it should instead print a
single line error message, starting with the text `ERROR:`, and terminate with
error. (If using go, if invoked with 'go run', the go runner will also print
the exit status of the program; this is OK, because your program is not
responsible for this.)

Example Inputs and Outputs
--------------------------

Example inputs and outputs are found in the provided/ directory, and consists
of the files:

- testinput1.csv: basic file parsing, expected output in testoutput1.csv
- testinput2.csv: formula evaluation, expected output in testoutput2.csv
- testinput3.csv: Some additional test cases, expected output in testoutput3.csv
- testinput4.csv: Some additional test cases, expected output in testoutput4.csv

It is probably a good idea to work through the inputs in order, e g, if you
solve the first file and are working on the second file when you reach end of
time, write down where you're at, and what would be your next steps at that
point.

Intellectual Property
---------------------

The contents of this directory, and the test cases, are property of Observe,
Inc.  Observe, Inc, reserves all rights, and you may not make unauthorized
copies of the contents, or transmit them to people outside Observe, Inc. No
warranty, expressed or implied, is made of any particular performance or
fitness for a particular purpose.
Copyright 2021, Observe, Inc, San Mateo, California
