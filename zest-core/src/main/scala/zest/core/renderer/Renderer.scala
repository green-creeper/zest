package zest.core.renderer

import cats.effect.IO
import zest.core.ast.*
import zest.core.context.Context
import zest.core.errors.TemplateError

/** Renders templates to final output */
trait Renderer {
  def render(template: Template, context: Context): IO[Either[TemplateError, String]]
}

object Renderer {
  def apply(): Renderer = new RendererImpl

  private class RendererImpl extends Renderer {
    private val evaluator = zest.core.evaluator.ExpressionEvaluator()

    def render(template: Template, context: Context): IO[Either[TemplateError, String]] = {
      renderNodes(template.nodes, context)
    }

    private def renderNodes(nodes: List[TemplateNode], context: Context): IO[Either[TemplateError, String]] = {
      nodes match {
        case Nil => IO.pure(Right(""))
        case head :: tail =>
          renderNode(head, context).flatMap {
            case Left(error) => IO.pure(Left(error))
            case Right(headResult) =>
              renderNodes(tail, context).map {
                case Left(error) => Left(error)
                case Right(tailResult) => Right(headResult + tailResult)
              }
          }
      }
    }

    private def renderNode(node: TemplateNode, context: Context): IO[Either[TemplateError, String]] = {
      node match {
        case TextNode(content, _) =>
          IO.pure(Right(content))

        case Interpolation(expression, location) =>
          evaluator.evaluate(expression, context).map {
            case Left(error) => Left(error)
            case Right(value) =>
              value.asString match {
                case Right(s) => Right(s)
                case Left(error) =>
                  Left(TemplateError.RenderingError(
                    s"Cannot convert value to string: $error",
                    location.template,
                    Some((location.line, location.column))
                  ))
              }
          }

        case IfStatement(condition, body, elseBody, location) =>
          evaluator.evaluate(condition, context).flatMap {
            case Left(error) => IO.pure(Left(error))
            case Right(value) =>
              value.asBoolean match {
                case Right(true) => renderNodes(body, context)
                case Right(false) =>
                  elseBody match {
                    case Some(elseNodes) => renderNodes(elseNodes, context)
                    case None => IO.pure(Right(""))
                  }
                case Left(error) =>
                  IO.pure(Left(TemplateError.RenderingError(
                    s"Condition must evaluate to boolean: $error",
                    location.template,
                    Some((location.line, location.column))
                  )))
              }
          }

        case Comment(_, _) =>
          IO.pure(Right(""))

        case ForStatement(_, _, _, _) =>
          // Not implemented in Phase 1
          IO.pure(Left(TemplateError.RenderingError(
            "For loops are not implemented in this version",
            node.location.template,
            Some((node.location.line, node.location.column))
          )))

        case _ =>
          IO.pure(Left(TemplateError.RenderingError(
            s"Unknown node type: ${node.getClass.getSimpleName}",
            node.location.template,
            Some((node.location.line, node.location.column))
          )))
      }
    }
  }
}
