import scala.io.Source
import java.io.File

def main(args: Array[String]): Unit =
  if args.size != 1 then
    Console.err.println("ERROR: require exactly one file argument to be passed to script")
  else
    val fileName = args(0)
    if !File(fileName).exists() then Console.err.println(s"ERROR: $fileName not found")
    else
      val csvArray = readCsv(fileName)
      if csvArray.size <= 0 then Console.err.println(s"ERROR: $fileName contains no rows")
      else
        val numCols = csvArray(0).size
        if numCols <= 0 then Console.err.println(s"ERROR: $fileName contains rows with 0 columns")
        else if csvArray.exists(row => row.size != numCols) then
          Console.err.println(s"ERROR: $fileName contains rows with mismatching number of columns")
        else
          parse(csvArray) match
            case e: Error =>
              Console.err.println(s"ERROR: $e")
            case s: Seq[Seq[Value]] =>
              println(prettySpreadSheet(eval(s)))

/** Reads a csv file. We assume the csv file is well-formed, i.e. all commas are separators and all
  * `\n` are linebreaks
  */
def readCsv(fileName: String): Seq[Seq[String]] =
  // we ensure that trailing empty spaces to the right of the last comma are included
  // cf. https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#split(java.lang.String,%20int)
  Source
    .fromFile(fileName, "utf8")
    .getLines()
    .map(line => line.split("""\s*,\s*""", -1).toSeq)
    .toSeq

/** Evaluates a spreadsheet
  *
  * FIXME: Dimension or cyclic errors are not detected
  */
def eval(spreadSheet: Seq[Seq[Value]]): Seq[Seq[Value]] =
  val spreadArray: Array[Array[Value]] = spreadSheet.map(_.toArray).toArray

  val res: Array[Array[Value]] = Array.ofDim(spreadArray.size, spreadArray(0).size)

  def evalSingle(colIdx: Int, rowIdx: Int): Value = res(rowIdx)(colIdx) match
    case null =>
      val evalled: Value = spreadArray(rowIdx)(colIdx) match
        case n: Value.Num  => n
        case s: Value.Lit  => s
        case Value.Form(f) => evalFormula(f)
        case v             => v

      res(rowIdx)(colIdx) = evalled
      evalled

    case v => v
  end evalSingle

  // FIXME - error out in case of division by zero
  def evalFormula(f: Formula): Value = f match
    case Formula.Cell(col, row) =>
      evalSingle(col - 'A', row - 1)
    case Formula.Num(d) => Value.Num(d)
    case Formula.Add(l, r) =>
      (evalFormula(l), evalFormula(r)) match
        case (Value.Num(a), Value.Num(b)) => Value.Num(a + b)
        case _                            => Value.Form(f)
    case Formula.Sub(l, r) =>
      (evalFormula(l), evalFormula(r)) match
        case (Value.Num(a), Value.Num(b)) => Value.Num(a - b)
        case _                            => Value.Form(f)
    case Formula.Mul(l, r) =>
      (evalFormula(l), evalFormula(r)) match
        case (Value.Num(a), Value.Num(b)) => Value.Num(a * b)
        case _                            => Value.Form(f)
    case Formula.Div(l, r) =>
      (evalFormula(l), evalFormula(r)) match
        case (Value.Num(a), Value.Num(b)) => Value.Num(a / b)
        case _                            => Value.Form(f)
  end evalFormula

  for colIdx <- 0 until spreadArray(0).size do
    for rowIdx <- 0 until spreadArray.size do evalSingle(colIdx, rowIdx)

  res.map(_.toSeq).toSeq

end eval

def parse(csvArray: Seq[Seq[String]]): Seq[Seq[Value]] | Error =
  csvArray.map { row =>
    row.map { cell =>
      parseCell(cell) match
        case e: Error => return e
        case v: Value => v
    }
  }

def parseCell(cell: String): Value | Error =
  import Value._
  if cell.isEmpty() then Value.None
  else
    cell.charAt(0) match
      case '=' =>
        parseFormula(cell.tail) match
          case e: Error   => e
          case f: Formula => Form(f)
      case '\'' => Lit(cell.tail)
      case _ =>
        try Num(cell.toDouble)
        catch case _: NumberFormatException => s"unable to parse cell with value $cell"

def parseFormula(s: String): Formula | Error = tokenize(s) match
  case e: Error        => e
  case ts: List[Token] => parseTokens(ts)

//format: off
/** Parsing tokens according to the following expression grammar
  *
  * E -> TE'
  * E' -> +TE' | -TE' | ε
  * T -> FT'
  * T' -> *FT' | /FT' | ε
  * F -> Num | (E)
  *
  * from
  * http://www.shivamkapoor.com/blogs/technology/2020/06/02/recursive-descent-parsers-in-scala-1-writing-context-free-grammar/
  */
//format: on
def parseTokens(tokens: List[Token]): Formula | Error =
  import Token._

  def parseFactor(ts: List[Token]): (Formula, List[Token]) | Error = ts match
    case Num(d) :: tss         => (Formula.Num(d), tss)
    case Cell(col, row) :: tss => (Formula.Cell(col, row), tss)
    case OPENBR :: tss =>
      parseExpr(tss) match
        case e: Error => e
        case (expr: Formula, ts3) =>
          ts3 match
            case CLOSEBR :: ts4 => (expr, ts4)
            case _              => "can't close parentheses during parsing"
    case _ => s"unable to parse factor, found $ts"
  end parseFactor

  def parseTerm(ts: List[Token]): (Formula, List[Token]) | Error = parseFactor(ts) match
    case e: Error => e
    case (l, MUL :: rest) =>
      parseTerm(rest) match
        case e: Error  => e
        case (r, rest) => (Formula.Mul(l, r), rest)
    case (l, DIV :: rest) =>
      parseTerm(rest) match
        case e: Error  => e
        case (r, rest) => (Formula.Div(l, r), rest)
    case (f, rest) => (f, rest)

  def parseExpr(ts: List[Token]): (Formula, List[Token]) | Error = parseTerm(ts) match
    case e: Error => e
    case (l, PLUS :: rest) =>
      parseExpr(rest) match
        case e: Error  => e
        case (r, rest) => (Formula.Add(l, r), rest)
    case (l, MINUS :: rest) =>
      parseExpr(rest) match
        case e: Error  => e
        case (r, rest) => (Formula.Sub(l, r), rest)
    case (f, rest) => (f, rest)

  parseExpr(tokens) match
    case e: Error => e
    case (f, Nil) => f
    case (_, xs)  => s"unable to parse $tokens, still have $xs remaining"

end parseTokens

def tokenize(s: String): List[Token] | Error =
  import scala.util.matching.Regex
  import Token._

  val num = """(^[\-\+]?[0-9]*\.?[0-9]+([eE][\-\+]?[0-9]+)?)(.*)""".r
  val plus = """\+(.*)""".r
  val minus = """\-(.*)""".r
  val mul = """\*(.*)""".r
  val div = """/(.*)""".r
  val openbr = """\((.*)""".r
  val closebr = """\)(.*)""".r
  val cell = """(([A-Z])(\d{1,2}))(.*)""".r

  // in the inner method we look for arithmetic operators first
  // so as to avoid ambiguities with parsing numbers
  def tokenizeRest(t: String): List[Token] | Error = t match
    case "" => Nil
    case plus(rest) =>
      tokenizeRest(rest) match
        case e: Error        => e
        case ls: List[Token] => PLUS :: ls
    case minus(rest) =>
      tokenizeRest(rest) match
        case e: Error        => e
        case ls: List[Token] => MINUS :: ls
    case mul(rest) =>
      tokenizeRest(rest) match
        case e: Error        => e
        case ls: List[Token] => MUL :: ls
    case div(rest) =>
      tokenizeRest(rest) match
        case e: Error        => e
        case ls: List[Token] => DIV :: ls
    case openbr(rest) =>
      tokenizeRest(rest) match
        case e: Error        => e
        case ls: List[Token] => OPENBR :: ls
    case closebr(rest) =>
      tokenizeRest(rest) match
        case e: Error        => e
        case ls: List[Token] => CLOSEBR :: ls
    case num(num, _, rest) =>
      tokenizeRest(rest) match
        case e: Error        => e
        case ls: List[Token] => Num(num.toDouble) :: ls
    case cell(_, col, row, rest) =>
      tokenizeRest(rest) match
        case e: Error        => e
        case ls: List[Token] => Cell(col(0), row.toInt) :: ls
    case _ => s"unrecognized token $t"
  end tokenizeRest

  s match
    case num(num, _, rest) =>
      tokenizeRest(rest) match
        case e: Error        => e
        case ls: List[Token] => Num(num.toDouble) :: ls
    case cell(_, col, row, rest) =>
      tokenizeRest(rest) match
        case e: Error        => e
        case ls: List[Token] => Cell(col(0), row.toInt) :: ls
    case openbr(rest) =>
      tokenizeRest(rest) match
        case e: Error        => e
        case ls: List[Token] => OPENBR :: ls
    case "" => Nil
    case _  => s"unrecognized start of formula: $s"

end tokenize

def prettySpreadSheet(spreadSheet: Seq[Seq[Value]]): String =
  spreadSheet.map(row => row.map(prettyValue).mkString(",")).mkString("\n")

def prettyValue(v: Value): String = v match
  case Value.Num(d)  => f"$d%.2f"
  case Value.Lit(s)  => s"'$s"
  case Value.Form(f) => s"=${prettyFormula(f)}"
  case Value.None    => ""

def prettyFormula(f: Formula, surround: Boolean = false): String =
  import Formula._
  f match
    case Cell(col, row) => s"${col}${row}"
    case Num(d)         => f"$d%.2f"
    case Add(l, r) =>
      val res = s"${prettyFormula(l)}+${prettyFormula(r)}"
      if surround then s"($res)" else res
    case Sub(l, r) =>
      val res = s"${prettyFormula(l)}-${prettyFormula(r)}"
      if surround then s"($res)" else res
    case Mul(l, r) => s"${prettyFormula(l, true)}*${prettyFormula(r, true)}"
    case Div(l, r) => s"${prettyFormula(l, true)}/${prettyFormula(r, true)}"

enum Token:
  case PLUS
  case MINUS
  case MUL
  case DIV
  case OPENBR
  case CLOSEBR
  case Cell(col: Char, row: Int)
  case Num(d: Double)

enum Value:
  case Num(d: Double)
  case Lit(s: String)
  case Form(f: Formula)
  case None

enum Formula:
  case Cell(col: Char, row: Int)
  case Num(d: Double)
  case Add(l: Formula, r: Formula)
  case Sub(l: Formula, r: Formula)
  case Mul(l: Formula, r: Formula)
  case Div(l: Formula, r: Formula)

type Error = String
