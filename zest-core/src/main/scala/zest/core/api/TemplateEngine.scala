package zest.core.api

import cats.effect.IO
import zest.core.ast.Template
import zest.core.context.{Context, Value}
import zest.core.errors.TemplateError
import zest.core.parser.TemplateParser
import zest.core.renderer.Renderer

/** Main API for the Zest templating engine */
trait TemplateEngine {
  def parse(template: String): Either[TemplateError, Template]
  def render(template: Template, context: Map[String, Any]): IO[Either[TemplateError, String]]
  def render(template: Template, context: Context): IO[Either[TemplateError, String]]
  def renderString(template: String, context: Map[String, Any]): IO[Either[TemplateError, String]]
  def renderString(template: String, context: Context): IO[Either[TemplateError, String]]
}

object TemplateEngine {
  def apply(): TemplateEngine = new TemplateEngineImpl

  private class TemplateEngineImpl extends TemplateEngine {
    private val parser = TemplateParser
    private val renderer = Renderer()

    def parse(template: String): Either[TemplateError, Template] = {
      parser.parse(template)
    }

    def render(template: Template, context: Map[String, Any]): IO[Either[TemplateError, String]] = {
      val ctx = Context.fromMap(context)
      render(template, ctx)
    }

    def render(template: Template, context: Context): IO[Either[TemplateError, String]] = {
      renderer.render(template, context)
    }

    def renderString(template: String, context: Map[String, Any]): IO[Either[TemplateError, String]] = {
      parse(template) match {
        case Left(error) => IO.pure(Left(error))
        case Right(parsedTemplate) => render(parsedTemplate, context)
      }
    }

    def renderString(template: String, context: Context): IO[Either[TemplateError, String]] = {
      parse(template) match {
        case Left(error) => IO.pure(Left(error))
        case Right(parsedTemplate) => render(parsedTemplate, context)
      }
    }
  }
}
