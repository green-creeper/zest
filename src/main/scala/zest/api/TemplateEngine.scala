package zest.api

import cats.effect.Async
import cats.syntax.all._
import fs2.io.file.{Files, Path}
import zest.core.context.Context
import zest.core.parser.TemplateParser
import zest.errors.ParseError

trait TemplateEngine[F[_]]:
  def compile(source: String): F[Template[F]]
  def compileFile(path: Path): F[Template[F]]
  def render(template: Template[F], context: Context): F[String]
  def renderFile(path: Path, context: Context): F[String]

object TemplateEngine:
  def apply[F[_]: Async: Files]: TemplateEngine[F] = new TemplateEngineImpl[F]

private[api] class TemplateEngineImpl[F[_]](using F: Async[F]) extends TemplateEngine[F]:
  private val parser = TemplateParser()

  def compile(source: String): F[Template[F]] = {
    parser.parse(source) match {
      case Right(nodes) => F.pure(new TemplateImpl[F](nodes))
      case Left(error)  => F.raiseError(error)
    }
  }

  def compileFile(path: Path): F[Template[F]] = ??? // to be implemented in Phase 3

  def render(template: Template[F], context: Context): F[String] =
    template.render(context)

  def renderFile(path: Path, context: Context): F[String] =
    compileFile(path).flatMap(_.render(context))
