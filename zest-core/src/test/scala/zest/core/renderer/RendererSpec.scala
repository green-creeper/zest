package zest.core.renderer

import munit.*
import zest.core.ast.*
import zest.core.context.*
import zest.core.errors.TemplateError

class RendererSpec extends FunSuite {

  private val renderer = Renderer()

  private def createContext(data: Map[String, Any]): Context = {
    Context.fromMap(data)
  }

  test("render simple text") {
    val template = Template("test", List(
      TextNode("Hello world", SourceLocation("test", 1, 1))
    ))
    val context = createContext(Map.empty)

    val result = renderer.render(template, context).unsafeRunSync()
    assert(result.isRight)
    assertEquals(result, Right("Hello world"))
  }

  test("render variable interpolation") {
    val template = Template("test", List(
      TextNode("Hello ", SourceLocation("test", 1, 1)),
      Interpolation(VariableReference("name", SourceLocation("test", 1, 1)), SourceLocation("test", 1, 1)),
      TextNode("!", SourceLocation("test", 1, 1))
    ))
    val context = createContext(Map("name" -> "World"))

    val result = renderer.render(template, context).unsafeRunSync()
    assert(result.isRight)
    assertEquals(result, Right("Hello World!"))
  }

  test("render if statement true") {
    val template = Template("test", List(
      IfStatement(
        VariableReference("condition", SourceLocation("test", 1, 1)),
        List(TextNode("true content", SourceLocation("test", 1, 1))),
        None,
        SourceLocation("test", 1, 1)
      )
    ))
    val context = createContext(Map("condition" -> true))

    val result = renderer.render(template, context).unsafeRunSync()
    assert(result.isRight)
    assertEquals(result, Right("true content"))
  }

  test("render if statement false") {
    val template = Template("test", List(
      IfStatement(
        VariableReference("condition", SourceLocation("test", 1, 1)),
        List(TextNode("true content", SourceLocation("test", 1, 1))),
        None,
        SourceLocation("test", 1, 1)
      )
    ))
    val context = createContext(Map("condition" -> false))

    val result = renderer.render(template, context).unsafeRunSync()
    assert(result.isRight)
    assertEquals(result, Right(""))
  }

  test("render if-else statement") {
    val template = Template("test", List(
      IfStatement(
        VariableReference("condition", SourceLocation("test", 1, 1)),
        List(TextNode("true content", SourceLocation("test", 1, 1))),
        Some(List(TextNode("false content", SourceLocation("test", 1, 1)))),
        SourceLocation("test", 1, 1)
      )
    ))
    val context = createContext(Map("condition" -> false))

    val result = renderer.render(template, context).unsafeRunSync()
    assert(result.isRight)
    assertEquals(result, Right("false content"))
  }

  test("render comment") {
    val template = Template("test", List(
      Comment(" this is a comment ", SourceLocation("test", 1, 1)),
      TextNode("Hello", SourceLocation("test", 1, 1))
    ))
    val context = createContext(Map.empty)

    val result = renderer.render(template, context).unsafeRunSync()
    assert(result.isRight)
    assertEquals(result, Right("Hello"))
  }

  test("render complex template") {
    val template = Template("test", List(
      TextNode("Hello ", SourceLocation("test", 1, 1)),
      Interpolation(VariableReference("name", SourceLocation("test", 1, 1)), SourceLocation("test", 1, 1)),
      TextNode(", you have ", SourceLocation("test", 1, 1)),
      Interpolation(VariableReference("count", SourceLocation("test", 1, 1)), SourceLocation("test", 1, 1)),
      TextNode(" messages.", SourceLocation("test", 1, 1))
    ))
    val context = createContext(Map("name" -> "Alice", "count" -> 5))

    val result = renderer.render(template, context).unsafeRunSync()
    assert(result.isRight)
    assertEquals(result, Right("Hello Alice, you have 5 messages."))
  }

  test("render with missing variable") {
    val template = Template("test", List(
      Interpolation(VariableReference("missing", SourceLocation("test", 1, 1)), SourceLocation("test", 1, 1))
    ))
    val context = createContext(Map.empty)

    val result = renderer.render(template, context).unsafeRunSync()
    assert(result.isLeft)
    result match {
      case Left(TemplateError.RenderingError(_, _, _)) => // Success
      case _ => fail(s"Expected RenderingError, got $result")
    }
  }

  test("render if with non-boolean condition") {
    val template = Template("test", List(
      IfStatement(
        StringLiteral("not a boolean", SourceLocation("test", 1, 1)),
        List(TextNode("true content", SourceLocation("test", 1, 1))),
        None,
        SourceLocation("test", 1, 1)
      )
    ))
    val context = createContext(Map.empty)

    val result = renderer.render(template, context).unsafeRunSync()
    assert(result.isLeft)
    result match {
      case Left(TemplateError.RenderingError(message, _, _)) =>
        assert(message.contains("Condition must evaluate to boolean"))
      case _ => fail(s"Expected RenderingError, got $result")
    }
  }
}
