package ir.typed

trait ArithmeticOperator extends IR {
  def typ: Type = VoidType
}

case object Add extends ArithmeticOperator {
  override def toString: String = s"Add"
}
case object Divide extends ArithmeticOperator {
  override def toString: String = s"Divide"
}
case object Modulo extends ArithmeticOperator {
  override def toString: String = s"Modulo"
}
case object Multiply extends ArithmeticOperator {
  override def toString: String = s"Multiply"
}
case object Subtract extends ArithmeticOperator {
  override def toString: String = s"Subtract"
}