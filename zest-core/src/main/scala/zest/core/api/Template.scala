package zest.core.api

import cats.effect.IO
import zest.core.ast.Template
import zest.core.context.{Context, Value}
import zest.core.errors.TemplateError

/** High-level template API with type-safe context binding */
trait Template {
  def render(context: Map[String, Any]): IO[Either[TemplateError, String]]
  def render(context: Context): IO[Either[TemplateError, String]]
}

object Template {
  def apply(template: String): TemplateBuilder = new TemplateBuilder(template)

  class TemplateBuilder(template: String) {
    def build(): Either[TemplateError, SimpleTemplate] = {
      val engine = TemplateEngine()
      engine.parse(template) match {
        case Left(error) => Left(error)
        case Right(parsedTemplate) => Right(new SimpleTemplate(parsedTemplate))
      }
    }
  }

  class SimpleTemplate(private val template: Template) {
    def render(context: Map[String, Any]): IO[Either[TemplateError, String]] = {
      val engine = TemplateEngine()
      engine.render(template, context)
    }

    def render(context: Context): IO[Either[TemplateError, String]] = {
      val engine = TemplateEngine()
      engine.render(template, context)
    }
  }
}
