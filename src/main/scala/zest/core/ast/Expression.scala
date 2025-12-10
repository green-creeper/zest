package zest.core.ast

sealed trait Expression

case class Variable(path: List[String]) extends Expression
case class BooleanLiteral(value: Boolean) extends Expression
