package ir.components


trait Literal extends Expression

case class IntLiteral(line: Int, col: Int, value: Long) extends Literal {
  def typ: Option[Type] = Option(IntType)
  override def toString: String = s"[IntLiteral] ${value}  (${line}:${col})"
}

case class BoolLiteral(line: Int, col: Int, value: Boolean) extends Literal {
  def typ: Option[Type] = Option(BoolType)
  override def toString: String = s"[BoolLiteral] ${value}  (${line}:${col})"
}

case class CharLiteral(line: Int, col: Int, value: Char) extends Literal {
  def typ: Option[Type] = Option(CharType)
  override def toString: String = s"[CharLiteral] ${value}  (${line}:${col})"
}

case class StringLiteral(line: Int, col: Int, value: String) extends Literal {
  def typ: Option[Type] = Option(StringType)
  override def toString: String = s"[StringLiteral] ${value}  (${line}:${col})"
}
