package codegen

import ir.components._

import scala.collection.mutable.ArrayBuffer

object TranslateIR {
  val aryIdxReg1: String = "%r10" // use %r10 for array indexing
  val aryIdxReg2: String = "%r11" // use %r11 when both sides are assignments

  def apply(ir: IR): Vector[String] = { // assuming here we only have
    val res: ArrayBuffer[String] = ArrayBuffer()
    //println(ir.getClass.toString)

    ir match {
      case assign: AssignStatement => { // assume that
        // index checking
        res ++= assign.loc.indexCheck
        assign.value match {
          case loc: Location => {
            res ++= loc.asInstanceOf[Location].indexCheck
          }
          case _ =>
        }
        val (repVecLoc: Vector[String], repStrLoc: String) = assign.loc.getRep(aryIdxReg1)
        val (repVecValue: Vector[String], repStrValue: String) = assign.value.getRep(aryIdxReg2)
        res ++= repVecLoc
        res ++= repVecValue
        res ++= Helper.outputMov(repStrValue, repStrLoc)
      }

      case compAsg: CompoundAssignStatement => {
        res ++= compAsg.loc.indexCheck
        compAsg.value match {
          case loc: Location => {
            res ++= loc.asInstanceOf[Location].indexCheck
          }
          case _ =>
        }
        val (repVecLoc: Vector[String], repStrLoc: String) = compAsg.loc.getRep(aryIdxReg1)
        val (repVecValue: Vector[String], repStrValue: String) = compAsg.value.getRep(aryIdxReg1)
        res ++= repVecLoc
        res ++= Helper.outputMov(repStrLoc, "%rax")
        res ++= repVecValue
        compAsg.operator match {
          case Add => {
            res += s"\taddq ${repStrValue}, %rax"
          }
          case Subtract => {
            res += s"\tsubq ${repStrValue}, %rax"
          }
        }
        res += s"\tmovq %rax, ${repStrLoc}"
      }

      case inc: Increment => {
        res ++= inc.loc.indexCheck
        val (repVec: Vector[String], repStr: String) = inc.loc.getRep(aryIdxReg1)
        res ++= repVec
        res += s"\tincq ${repStr}"
      }

      case dec: Decrement => {
        res ++= dec.loc.indexCheck
        val (repVec: Vector[String], repStr: String) = dec.loc.getRep(aryIdxReg1)
        res ++= repVec
        res += s"\tdecq ${repStr}"
      }

      case ret: Return => {
        if (ret.value.isDefined) {
          ret.value.get match {
            case loc: Location => {
              res ++= loc.indexCheck
            }
            case _ =>
          }
          val (repVec: Vector[String], repStr: String) = ret.value.get.getRep(aryIdxReg1)
          res ++= repVec
          res += s"\tmovq ${repStr}, %rax"
        }
        res += s"\tleave"
        res += s"\tret"
      }

      case variable: VariableDeclaration => {
        res += s"${variable.name}:"
        res += s"\t.zero 8"
      }

      case array: ArrayDeclaration => {
        res += s"${array.name}:"
        res += s"\t.zero ${8 * array.length.value}"
      }

      case op: Operation => {
        op match {
          case ury: UnaryOperation => {
            ury.expression match {
              case loc: Location => {
                res ++= loc.indexCheck
              }
              case _ =>
            }
            val (repVec: Vector[String], repStr: String) = ury.expression.getRep(aryIdxReg1)
            res ++= repVec
            res ++= Helper.outputMov(repStr, "%rax")

            ury match {
              case not: Not => {
                res += s"\tmovq $$1, ${aryIdxReg1}" // logical not workaround
                res += s"\txorq ${aryIdxReg1}, %rax"
              }

              case neg: Negate => {
                res += s"\tnegq %rax"
              }
            }
          }

          case ari: ArithmeticOperation => {
            ari.lhs match {
              case loc: Location => {
                res ++= loc.indexCheck
              }
              case _ =>
            }
            ari.rhs match {
              case loc: Location => {
                res ++= loc.indexCheck
              }
              case _ =>
            }
            val (repVecLHS: Vector[String], repStrLHS: String) = ari.lhs.getRep(aryIdxReg1)
            val (repVecRHS: Vector[String], repStrRHS: String) = ari.rhs.getRep(aryIdxReg2)
            res ++= repVecLHS
            res ++= repVecRHS
            res ++= Helper.outputMov(repStrLHS, "%rax")

            ari.operator match {
              case Add => {
                res += s"\taddq ${repStrRHS}, %rax"
              }

              case Subtract => {
                res += s"\tsubq ${repStrRHS}, %rax"
              }

              case Divide => {
                res ++= Helper.outputMov(repStrRHS, "%rsi")
                res += s"\tcqto"
                res += s"\tidivq %rsi"
              }

              case Modulo => {
                res ++= Helper.outputMov(repStrRHS, "%rsi")
                res += s"\tcqto"
                res += s"\tidivq %rsi"
                res += s"\tmovq %rdx, %rax"
              }

              case Multiply => {
                res += s"\timul ${repStrRHS}, %rax"
              }
            }
          }

          case log: LogicalOperation => {
            log.lhs match {
              case loc: Location => {
                res ++= loc.indexCheck
              }
              case _ =>
            }
            log.rhs match {
              case loc: Location => {
                res ++= loc.indexCheck
              }
              case _ =>
            }
            val (repVecLHS: Vector[String], repStrLHS: String) = log.lhs.getRep(aryIdxReg1)
            val (repVecRHS: Vector[String], repStrRHS: String) = log.rhs.getRep(aryIdxReg2)
            res ++= repVecLHS
            res ++= repVecRHS
            res ++= Helper.outputMov(repStrLHS, "%rdx")
            res ++= Helper.outputMov(repStrRHS, "%rsi")
            res += s"\txor %rax, %rax"
            res += s"\tcmpq %rsi, %rdx"

            log.operator match {
              case Equal => {
                res += s"\tsete %al"
              }

              case NotEqual => {
                res += s"\tsetne %al"
              }

              case GreaterThan => {
                res += s"\tsetg %al"
              }

              case GreaterThanOrEqual => {
                res += s"\tsetge %al"
              }

              case LessThan => {
                res += s"\tsetl %al"
              }

              case LessThanOrEqual => {
                res += s"\tsetle %al"
              }

              case _ => throw new NotImplementedError()
            }

            res += s"\tmovzbl %al, %eax"
          }
        }
        val (repVecEval: Vector[String], repStrEval: String) = op.eval.get.getRep(aryIdxReg1)
        res ++= repVecEval
        res ++= Helper.outputMov("%rax", repStrEval) // safe
      }

      // case loc: Location => {
      //   loc.index.get match {
      //     case indexLoc: Location => {
      //       res ++= indexLoc.indexCheck
      //     }
      //     case _ =>
      //   }
      //   val (repVec: Vector[String], repStr: String) = loc.index.get.getRep(aryIdxReg1)
      //   res ++= repVec
      //   res ++= Helper.outputMov(repStr, "%rax")
      // }

      case _ => throw new NotImplementedError
    }
    res.toVector
  }
}
