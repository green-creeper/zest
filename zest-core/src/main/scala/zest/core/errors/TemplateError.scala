package zest.core.errors

/** Base trait for all template errors */
sealed trait TemplateError {
  def message: String
  def template: String
  def location: Option[(Int, Int)]
}

object TemplateError {
  case class ParseError(
    message: String,
    template: String,
    location: Option[(Int, Int)] = None
  ) extends TemplateError

  case class EvaluationError(
    message: String,
    template: String,
    location: Option[(Int, Int)] = None
  ) extends TemplateError

  case class RenderingError(
    message: String,
    template: String,
    location: Option[(Int, Int)] = None
  ) extends TemplateError

  case class ContextError(
    message: String,
    template: String,
    location: Option[(Int, Int)] = None
  ) extends TemplateError

  case class ValidationError(
    message: String,
    template: String,
    location: Option[(Int, Int)] = None
  ) extends TemplateError

  def formatError(error: TemplateError): String = {
    val locationStr = error.location match {
      case Some((line, col)) => s" (line $line, column $col)"
      case None => ""
    }
    s"${error.template}$locationStr: ${error.message}"
  }
}
