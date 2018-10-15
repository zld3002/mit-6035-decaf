package compile

import java.io._
import scala.Console

import edu.mit.compilers.grammar.{DecafParser, DecafScanner, DecafScannerTokenTypes}
import ir.{ASTtoIR, CommonASTWithLines, ScalarAST, TypeChecking}
import ir.miscellaneousCheck
import ir.components.IR
import util.CLI

object Compiler {
  val tokenMap = Map(
    DecafScannerTokenTypes.CHAR_LITERAL -> "CHARLITERAL",
    DecafScannerTokenTypes.DECIMAL -> "INTLITERAL",
    DecafScannerTokenTypes.HEXADECIMAL -> "INTLITERAL",
    DecafScannerTokenTypes.SC_ID -> "IDENTIFIER",
    DecafScannerTokenTypes.STR_LITERAL -> "STRINGLITERAL",
    DecafScannerTokenTypes.TK_false -> "BOOLEANLITERAL",
    DecafScannerTokenTypes.TK_true -> "BOOLEANLITERAL"
  )
  val outFile = if (CLI.outfile == null) Console.out else new PrintStream(new FileOutputStream(CLI.outfile))

  def main(args: Array[String]): Unit = {
    CLI.parse(args, Array[String]())
    if (CLI.target == CLI.Action.SCAN) {
      scan(CLI.infile)
    } else if (CLI.target == CLI.Action.PARSE) {
      if (parse(CLI.infile) == null) {
        System.exit(1)
      }
    } else if (CLI.target == CLI.Action.INTER) {
      if (inter(CLI.infile) == null) {
        System.exit(1)
      }
    }
    System.exit(0)
  }

  def scan(fileName: String, debugSwitch: Boolean = CLI.debug) {
    try {
      val inputStream: FileInputStream = new java.io.FileInputStream(fileName)
      val scanner = new DecafScanner(new DataInputStream(inputStream))
      scanner.setTrace(debugSwitch)
      var done = false
      while (!done) {
        try {
          val head = scanner.nextToken()
          if (head.getType() == DecafScannerTokenTypes.EOF) {
            done = true
          } else {
            val tokenType = tokenMap.getOrElse(head.getType(), "")
            outFile.println(head.getLine() + (if (tokenType ==  "") "" else " ") + tokenType + " " + head.getText())
          }
        } catch {
          case ex: Exception => {
            Console.err.println(CLI.infile + " " + ex)
            scanner.consume();
          }
        }
      }
    } catch {
      case ex: Exception => Console.err.println(ex)
    }
  }

  def parse(fileName: String, debugSwitch: Boolean = CLI.debug): ScalarAST = {
    /**
    Parse the file specified by the filename. Eventually, this method
    may return a type specific to your compiler.
    */
    var inputStream : java.io.FileInputStream = null
    try {
      inputStream = new java.io.FileInputStream(fileName)
    } catch {
      case f: FileNotFoundException => {
        Console.err.println("File " + fileName + " does not exist")
        return null
      }
    }

    try {
      val scanner = new DecafScanner(new DataInputStream(inputStream))
      val parser = new DecafParser(scanner);

      // convert to CommonASTWithLines: see ir.CommonASTWithLines for more info
      parser.setASTNodeClass("ir.CommonASTWithLines")
      parser.setTrace(debugSwitch)
      parser.program()
      val t = parser.getAST().asInstanceOf[CommonASTWithLines]
      if (parser.getError()) {
        println("[ERROR] Parse failed")
        return null
      } else if (debugSwitch) {
        println(t.toStringList)
        println()
      }

      // CommonASTWithLines to ScalarAST
      val ast = ScalarAST.fromCommonAST(t)
      ast.prettyPrint()
      ast
    } catch {
      case e: Exception => Console.err.println(CLI.infile + " " + e)
      null
    }
  }

  def inter(fileName: String, debugSwitch: Boolean = CLI.debug) : IR = {
    val optAST = Option(parse(fileName, false))

    // parsing failed
    if (optAST.isEmpty) {
      System.exit(1)
    }

    val ast = optAST.get
    if (debugSwitch) {
      ast.prettyPrint()
    }

    // change AST to IR
    val ir = ASTtoIR(ast)
    if (ASTtoIR.error) {
      System.exit(1)
    }

    TypeChecking(ir)
    if (TypeChecking.error) {
      System.exit(1)
    }

    miscellaneousCheck.apply
    if (miscellaneousCheck.error) {
      System.exit(1)
    }

    ir
  }
}
