package zest.core.parser

import cats.parse.{Parser => P, Parser0 => P0}
import cats.syntax.all._
import zest.core.ast._
import zest.errors.ParseError

trait TemplateParser:
  def parse(input: String): Either[ParseError, List[Node]]

object TemplateParser:
  def apply(): TemplateParser = new TemplateParserImpl

private class TemplateParserImpl extends TemplateParser {

  private val openVar = P.string("{{")
  private val closeVar = P.string("}}")
  private val openTag = P.string("{%")
  private val closeTag = P.string("%}")

  private val ws: P0[Unit] = P.charIn(" \t\r\n").rep0.void

  private val identifier: P[String] =
    (P.charIn('a' to 'z') | P.charIn('A' to 'Z') | P.charIn('0' to '9') | P.char('_')).rep.string

  private val path: P[List[String]] =
    identifier.repSep(P.char('.')).map(_.toList)

  private val expression: P[Expression] =
    path.map(Variable.apply) |
    (P.string("true").as(true) | P.string("false").as(false)).map(BooleanLiteral.apply)

  private val variable: P[VariableNode] =
    (openVar.surroundedBy(ws) *> path <* ws <* closeVar).map(VariableNode.apply)

  private val text: P[TextNode] =
    P.until(openVar | openTag).string.map(TextNode.apply)

  private lazy val nodes: P0[List[Node]] = P.recursive { recurseNodes =>
    lazy val ifNode: P[IfNode] = {
      val ifTag = openTag.surroundedBy(ws) *> P.string("if") *> ws *> expression <* ws <* closeTag
      val elseTag = openTag.surroundedBy(ws) *> P.string("else") <* ws <* closeTag
      val endifTag = openTag.surroundedBy(ws) *> P.string("endif") <* ws <* closeTag

      (ifTag ~ recurseNodes ~ (elseTag *> recurseNodes).? <* endifTag).map {
        case ((cond, thenBranch), elseBranch) =>
          IfNode(cond, thenBranch, elseBranch)
      }
    }

    val node: P[Node] =
      (openTag.with1 *> ifNode) |
      (openVar.with1 *> variable) |
      text

    node.rep0
  }

  def parse(input: String): Either[ParseError, List[Node]] = {
    nodes.parseAll(input).leftMap(e => ParseError(e.show, e.failedAtOffset, 0))
  }
}