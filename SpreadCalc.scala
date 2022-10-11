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
              Console.err.println(
                s"ERROR: $fileName contains rows with mismatching number of columns"
              )
            case s: Seq[Seq[Value]] => println(prettyString(s))

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
  if cell.isEmpty() then Num(0.0)
  else
    cell.charAt(0) match
      case '='  => Form(parseFormula(cell.tail))
      case '\'' => Lit(cell.tail)
      case _ =>
        try Num(cell.toDouble)
        catch case _: NumberFormatException => s"unable to parse cell with value $cell"

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
        case ls: List[Token] => Cell(col, row.toInt) :: ls
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
        case ls: List[Token] => Cell(col, row.toInt) :: ls
    case openbr(rest) =>
      tokenizeRest(rest) match
        case e: Error        => e
        case ls: List[Token] => OPENBR :: ls
    case "" => Nil
    case _  => s"unrecognized start of formula: $s"

end tokenize

def prettyString(spreadSheet: Seq[Seq[Value]]): String =
  spreadSheet.map(row => row.map(prettyString).mkString(",")).mkString("\n")

def prettyString(v: Value): String = v match
  case Value.Num(d)  => f"$d%.2f"
  case Value.Lit(s)  => s"'$s"
enum Token:
  case PLUS
  case MINUS
  case MUL
  case DIV
  case OPENBR
  case CLOSEBR
  case Cell(col: String, row: Int)
  case Num(d: Double)

enum Value:
  case Num(d: Double)
  case Lit(s: String)
  case Form(f: Formula)

enum Formula:
  case Cell(col: String, row: Int)
  case Num(d: Double)
  case Add(l: Formula, r: Formula)
  case Sub(l: Formula, r: Formula)
  case Mul(l: Formula, r: Formula)
  case Div(l: Formula, r: Formula)

type Error = String
