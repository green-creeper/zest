package zest.api

import cats.effect.IO
import munit.CatsEffectSuite
import zest.core.context.Context
import zest.errors.ParseError

class TemplateEngineSpec extends CatsEffectSuite {

  val engine: TemplateEngine[IO] = new TemplateEngineImpl[IO]

  test("should compile and render a simple template") {
    val templateString = "Hello, {{ name }}!"
    val context = Context.fromMap(Map("name" -> "Alice"))
    for {
      template <- engine.compile(templateString)
      result <- template.render(context)
    } yield assertEquals(result, "Hello, Alice!")
  }

  test("should return a parse error for invalid syntax") {
    val templateString = "Hello, {{ name !"
    engine.compile(templateString).intercept[ParseError]
  }
}
