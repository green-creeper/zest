package zest.core

/** Abstract Syntax Tree definitions for Zest templates */
package object ast {
  type Variable = String
  type TemplateName = String
  type LineNumber = Int
  type ColumnNumber = Int

  case class SourceLocation(
    template: TemplateName,
    line: LineNumber,
    column: ColumnNumber
  )

  sealed trait TemplateNode {
    def location: SourceLocation
  }

  case class Template(
    name: TemplateName,
    nodes: List[TemplateNode]
  ) extends TemplateNode {
    def location: SourceLocation = SourceLocation(name, 1, 1)
  }

  sealed trait Expression extends TemplateNode {
    def location: SourceLocation
  }

  case class VariableReference(
    name: String,
    location: SourceLocation
  ) extends Expression

  case class StringLiteral(
    value: String,
    location: SourceLocation
  ) extends Expression

  case class BooleanLiteral(
    value: Boolean,
    location: SourceLocation
  ) extends Expression

  case class NumberLiteral(
    value: Double,
    location: SourceLocation
  ) extends Expression

  case class PropertyAccess(
    target: Expression,
    property: String,
    location: SourceLocation
  ) extends Expression

  case class IndexAccess(
    target: Expression,
    index: Expression,
    location: SourceLocation
  ) extends Expression

  case class BinaryOperation(
    left: Expression,
    operator: String,
    right: Expression,
    location: SourceLocation
  ) extends Expression

  case class UnaryOperation(
    operator: String,
    operand: Expression,
    location: SourceLocation
  ) extends Expression

  case class FunctionCall(
    name: String,
    arguments: List[Expression],
    location: SourceLocation
  ) extends Expression

  sealed trait Statement extends TemplateNode {
    def location: SourceLocation
  }

  case class TextNode(
    content: String,
    location: SourceLocation
  ) extends Statement

  case class Interpolation(
    expression: Expression,
    location: SourceLocation
  ) extends Statement

  case class IfStatement(
    condition: Expression,
    body: List[TemplateNode],
    elseBody: Option[List[TemplateNode]] = None,
    location: SourceLocation
  ) extends Statement

  case class Comment(
    content: String,
    location: SourceLocation
  ) extends Statement

  case class ForStatement(
    variable: String,
    collection: Expression,
    body: List[TemplateNode],
    location: SourceLocation
  ) extends Statement

  case class FilterExpression(
    expression: Expression,
    filters: List[FunctionCall],
    location: SourceLocation
  ) extends Expression
}
