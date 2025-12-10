package zest.renderer

import munit.FunSuite
import zest.core.ast.{IfNode, TextNode, Variable, VariableNode}
import zest.core.context.Context
import zest.core.renderer.Renderer

class RendererSpec extends FunSuite {
  type F[A] = Either[Throwable, A]
  val renderer = Renderer[F]

  test("should render simple text") {
    val nodes = List(TextNode("Hello, world!"))
    val result = renderer.render(nodes, Context.empty)
    assertEquals(result, Right("Hello, world!"))
  }

  test("should render a variable") {
    val nodes = List(VariableNode(List("name")))
    val context = Context.fromMap(Map("name" -> "Alice"))
    val result = renderer.render(nodes, context)
    assertEquals(result, Right("Alice"))
  }

  test("should render a mix of text and variables") {
    val nodes = List(
      TextNode("Hello, "),
      VariableNode(List("name")),
      TextNode("!")
    )
    val context = Context.fromMap(Map("name" -> "Alice"))
    val result = renderer.render(nodes, context)
    assertEquals(result, Right("Hello, Alice!"))
  }

  test("should render nothing for a missing variable") {
    val nodes = List(VariableNode(List("name")))
    val context = Context.empty
    val result = renderer.render(nodes, context)
    assertEquals(result, Right(""))
  }

  test("should render if block when condition is true") {
    val nodes = List(
      IfNode(
        Variable(List("condition")),
        List(TextNode("hello")),
        None
      )
    )
    val context = Context.fromMap(Map("condition" -> true))
    val result = renderer.render(nodes, context)
    assertEquals(result, Right("hello"))
  }

  test("should not render if block when condition is false") {
    val nodes = List(
      IfNode(
        Variable(List("condition")),
        List(TextNode("hello")),
        None
      )
    )
    val context = Context.fromMap(Map("condition" -> false))
    val result = renderer.render(nodes, context)
    assertEquals(result, Right(""))
  }

  test("should render else block when condition is false") {
    val nodes = List(
      IfNode(
        Variable(List("condition")),
        List(TextNode("hello")),
        Some(List(TextNode("world")))
      )
    )
    val context = Context.fromMap(Map("condition" -> false))
    val result = renderer.render(nodes, context)
    assertEquals(result, Right("world"))
  }
}