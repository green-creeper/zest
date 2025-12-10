package zest.core.renderer

import cats.MonadError
import cats.syntax.all._
import zest.core.ast.{Node, TextNode, VariableNode, IfNode, CommentNode, Variable}
import zest.core.context.{Context, Value}
import zest.core.evaluator.Evaluator
import zest.errors.RenderError

trait Renderer[F[_]]:
  def render(nodes: List[Node], ctx: Context): F[String]

object Renderer:
  def apply[F[_]](using me: MonadError[F, Throwable]): Renderer[F] = new RendererImpl[F]

private class RendererImpl[F[_]](using me: MonadError[F, Throwable]) extends Renderer[F]:
  private val evaluator = Evaluator[F]

  def render(nodes: List[Node], ctx: Context): F[String] = {
    nodes.traverse(node => renderNode(node, ctx)).map(_.mkString)
  }

  private def renderNode(node: Node, ctx: Context): F[String] = node match {
    case TextNode(content) => me.pure(content)
    case VariableNode(path) =>
      evaluator.evaluate(Variable(path), ctx).map(valueToString)
    case CommentNode(_) => me.pure("") // Comments are ignored
    case IfNode(condition, thenBranch, elseBranchOpt) =>
      evaluator.evaluateBoolean(condition, ctx).flatMap {
        case true => render(thenBranch, ctx)
        case false => elseBranchOpt.map(render(_, ctx)).getOrElse(me.pure(""))
      }
  }

  private def valueToString(value: Value): String = value match {
    case Value.StringValue(s) => s
    case Value.NumberValue(n) => n.toString
    case Value.BooleanValue(b) => b.toString
    case Value.ObjectValue(_) => "[object]"
    case Value.ArrayValue(_) => "[array]"
    case Value.NullValue => ""
  }
