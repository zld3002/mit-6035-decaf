package optimization

import codegen._
import ir.components._

import scala.collection.mutable
import scala.collection.mutable.Set


/**
  * this part only takes out temp generated by cse and cp
  * and those variable names would terminate with "_cse_local_tmp", note that cp does not introduce any new temp.
  */

object DCE extends Optimization{
  val localTmpSuffix = "_cse_local_tmp"

  // if location is not a location then we do nothing.
  // if it's a variable, we add it to the needed set.
  // if it's an array, we add it and it's index to the set.
  def markRhs(needed: mutable.Set[FieldDeclaration], expr: Expression): Unit = {
    expr match {
      case loc: Location => {
        needed.add(loc.field.get)
        if (loc.index.isDefined)
          markRhs(needed, loc.index.get)
      }

      case _ =>
    }
  }

  // if it's a location on the left hand side, we only mark it's index.
  def markLhs(needed: mutable.Set[FieldDeclaration], expr: Expression): Unit = {
    expr match {
      case loc: Location => {
        if (loc.index.isDefined)
          markRhs(needed, loc.index.get)
      }

      case _ =>
    }
  }

  def isNotNeeded(needed: mutable.Set[FieldDeclaration], loc: Location): Boolean = {
    Helper.nameEndsWith(loc,localTmpSuffix) &&
      !needed.contains(loc.field.get)
  }

  def apply(cfg: CFG, isInit: Boolean=true): Unit = {
    if (isInit) { init }
    if (cfg.isOptimized(DCE)) {
      return
    }
    cfg.setOptimized(DCE)

    cfg match {
      case vCfg: VirtualCFG => {
        if (vCfg.next.isDefined) {
          DCE(vCfg.next.get, false)
        }
      }

      case block: CFGBlock => {
        if (block.next.isDefined) {
          DCE(block.next.get, false)
        }

        val needed = Set[FieldDeclaration]()

        //assuming each tmp var won't be assigned twice.
        for (idx <- block.statements.indices.reverse) {
          val statement = block.statements(idx)
          statement match {
            case assign: AssignmentStatements => {
              if (isNotNeeded(needed, assign.loc)) {
                setChanged
                block.statements.remove(idx)
              }
              else {
                markLhs(needed, assign.loc)
                markRhs(needed, assign.value)
              }
            }

            case inc: Increment => {
              if (isNotNeeded(needed, inc.loc)) {
                setChanged
                block.statements.remove(idx)
              }
              else {
                markRhs(needed, inc.loc)
              }
            }

            case dec: Decrement => {
              if (isNotNeeded(needed, dec.loc)) {
                setChanged
                block.statements.remove(idx)
              }
              else {
                markRhs(needed, dec.loc)
              }
            }

              // we should never take out a return value.
            case ret: Return => {
              if (!ret.value.isEmpty) {
                markRhs(needed, ret.value.get)
              }
            }

            case op: Operation => {
              op match {
                case ury: UnaryOperation => {
                  if (isNotNeeded(needed, ury.eval.get)) {
                    setChanged
                    block.statements.remove(idx)
                  }
                  else {
                    markLhs(needed, ury.eval.get)
                    markRhs(needed, ury.expression)
                  }
                }

                case binary: BinaryOperation => {
                  if (isNotNeeded(needed, binary.eval.get)) {
                    setChanged
                    block.statements.remove(idx)
                  }
                  else {
                    markLhs(needed, binary.eval.get)
                    markRhs(needed, binary.lhs) // note that binary lhs is still the rhs of a binary expression.
                    markRhs(needed, binary.rhs)
                  }
                }
              }
            }

            case _ => throw new NotImplementedError
          }
        }
      }

      case condition: CFGConditional => {
        if (condition.next.isDefined) {
          DCE(condition.next.get, false)
        }
        if (condition.ifFalse.isDefined) {
          DCE(condition.ifFalse.get, false)
        }
      }

      case method: CFGMethod => {
        DCE(method.block.get, false)
      }

      case call: CFGMethodCall => {
        if (call.next.isDefined) {
          DCE(call.next.get, false)
        }
      }

      case prog: CFGProgram => {
        prog.methods foreach {DCE(_, false)}
      }
    }
  }

}
