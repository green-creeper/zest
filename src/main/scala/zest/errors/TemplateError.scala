package zest.errors

import zest.core.ast.Node
import java.nio.file.Path

sealed trait TemplateError extends Throwable

case class ParseError(
  message: String,
  line: Int,
  column: Int,
  source: Option[String] = None
) extends TemplateError

case class EvaluationError(
  message: String,
  path: List[String],
  context: String
) extends TemplateError

case class RenderError(
  message: String,
  node: Node,
  cause: Option[Throwable] = None
) extends TemplateError

case class IOError(
  message: String,
  path: Path,
  cause: Throwable
) extends TemplateError
