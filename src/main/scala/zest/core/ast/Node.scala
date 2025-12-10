package zest.core.ast

sealed trait Node

case class TextNode(content: String) extends Node
case class VariableNode(path: List[String]) extends Node
case class IfNode(
  condition: Expression,
  thenBranch: List[Node],
  elseBranch: Option[List[Node]]
) extends Node
case class CommentNode(content: String) extends Node
