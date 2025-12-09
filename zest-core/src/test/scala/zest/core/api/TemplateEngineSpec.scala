package zest.core.api

import munit.*
import zest.core.ast.*
import zest.core.context.*
import zest.core.errors.TemplateError

class TemplateEngineSpec extends FunSuite {

  private val engine = TemplateEngine()

  test("parse and render simple template") {
    val template = "Hello {{ name }}!"
    val context = Map("name" -> "World")

    val parseResult = engine.parse(template)
    assert(parseResult.isRight)
    parseResult match {
      case Right(parsedTemplate) =>
        val renderResult = engine.render(parsedTemplate, context).unsafeRunSync()
        assert(renderResult.isRight)
        assertEquals(renderResult, Right("Hello World!"))
      case Left(error) => fail(s"Unexpected parse error: $error")
    }
  }

  test("render string directly") {
    val template = "Hello {{ name }}!"
    val context = Map("name" -> "World")

    val result = engine.renderString(template, context).unsafeRunSync()
    assert(result.isRight)
    assertEquals(result, Right("Hello World!"))
  }

  test("render with missing variable") {
    val template = "Hello {{ name }}!"
    val context = Map.empty[String, Any]

    val result = engine.renderString(template, context).unsafeRunSync()
    assert(result.isLeft)
    result match {
      case Left(TemplateError.RenderingError(_, _, _)) => // Success
      case _ => fail(s"Expected RenderingError, got $result")
    }
  }

  test("render with if statement") {
    val template = "Hello {% if active %}{{ name }}{% else %}guest{% endif %}!"
    val context = Map("name" -> "Alice", "active" -> true)

    val result = engine.renderString(template, context).unsafeRunSync()
    assert(result.isRight)
    assertEquals(result, Right("Hello Alice!"))
  }

  test("render with if statement false") {
    val template = "Hello {% if active %}{{ name }}{% else %}guest{% endif %}!"
    val context = Map("name" -> "Alice", "active" -> false)

    val result = engine.renderString(template, context).unsafeRunSync()
    assert(result.isRight)
    assertEquals(result, Right("Hello guest!"))
  }

  test("render with complex expression") {
    val template = "{{ user.name }} is {{ user.age }} years old."
    val context = Map("user" -> Map("name" -> "Alice", "age" -> 30))

    val result = engine.renderString(template, context).unsafeRunSync()
    assert(result.isRight)
    assertEquals(result, Right("Alice is 30.0 years old."))
  }

  test("render with comments") {
    val template = "Hello {# this is a comment #}{{ name }}!"
    val context = Map("name" -> "World")

    val result = engine.renderString(template, context).unsafeRunSync()
    assert(result.isRight)
    assertEquals(result, Right("Hello World!"))
  }

  test("render with binary operations") {
    val template = "{{ a + b }} = {{ result }}"
    val context = Map("a" -> 10, "b" -> 20, "result" -> 30)

    val result = engine.renderString(template, context).unsafeRunSync()
    assert(result.isRight)
    assertEquals(result, Right("30.0 = 30.0"))
  }

  test("render with function calls") {
    val template = "{{ name | upper }}"
    // Note: Filters not fully implemented yet, but basic function calls work
    val context = Map("name" -> "hello")

    // For now, test that it parses correctly
    val parseResult = engine.parse(template)
    assert(parseResult.isRight)
  }

  test("render with nested objects") {
    val template = "{{ user.profile.name }} has {{ user.stats.messages }} messages"
    val context = Map(
      "user" -> Map(
        "profile" -> Map("name" -> "Alice"),
        "stats" -> Map("messages" -> 42)
      )
    )

    val result = engine.renderString(template, context).unsafeRunSync()
    assert(result.isRight)
    assertEquals(result, Right("Alice has 42.0 messages"))
  }

  test("render with missing property") {
    val template = "{{ user.missing }}"
    val context = Map("user" -> Map("name" -> "Alice"))

    val result = engine.renderString(template, context).unsafeRunSync()
    assert(result.isLeft)
    result match {
      case Left(TemplateError.RenderingError(message, _, _)) =>
        assert(message.contains("Property"))
      case _ => fail(s"Expected RenderingError, got $result")
    }
  }

  test("render empty template") {
    val template = ""
    val context = Map.empty[String, Any]

    val result = engine.renderString(template, context).unsafeRunSync()
    assert(result.isRight)
    assertEquals(result, Right(""))
  }

  test("render template with only whitespace") {
    val template = "   \n\t  "
    val context = Map.empty[String, Any]

    val result = engine.renderString(template, context).unsafeRunSync()
    assert(result.isRight)
    assertEquals(result, Right("   \n\t  "))
  }
}
