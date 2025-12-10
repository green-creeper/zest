package zest.parser

import munit.FunSuite
import zest.core.ast._
import zest.core.parser.TemplateParser
import zest.errors.ParseError

class TemplateParserSpec extends FunSuite {
  val parser = TemplateParser()

  test("should parse simple text") {
    val input = "Hello, world!"
    val expected = Right(List(TextNode("Hello, world!")))
    assertEquals(parser.parse(input), expected)
  }

  test("should parse a single variable") {
    val input = "{{ name }}"
    val expected = Right(List(VariableNode(List("name"))))
    assertEquals(parser.parse(input), expected)
  }

  test("should parse a variable with surrounding text") {
    val input = "Hello, {{ name }}!"
    val expected = Right(List(
      TextNode("Hello, "),
      VariableNode(List("name")),
      TextNode("!")
    ))
    assertEquals(parser.parse(input), expected)
  }

  test("should parse a variable with a path") {
    val input = "{{ user.name }}"
    val expected = Right(List(VariableNode(List("user", "name"))))
    assertEquals(parser.parse(input), expected)
  }

  test("should return a parse error for unclosed braces") {
    val input = "{{ name"
    assert(parser.parse(input).isLeft)
  }

  test("should parse a simple if statement") {
    val input = "{% if condition %}hello{% endif %}"
    val expected = Right(List(
      IfNode(
        Variable(List("condition")),
        List(TextNode("hello")),
        None
      )
    ))
    assertEquals(parser.parse(input), expected)
  }

  test("should parse an if-else statement") {
    val input = "{% if condition %}hello{% else %}world{% endif %}"
    val expected = Right(List(
      IfNode(
        Variable(List("condition")),
        List(TextNode("hello")),
        Some(List(TextNode("world")))
      )
    ))
    assertEquals(parser.parse(input), expected)
  }

  test("should parse text with single curly braces") {
    val input = "This is {a text} with braces."
    val expected = Right(List(TextNode("This is {a text} with braces.")))
    assertEquals(parser.parse(input), expected)
  }
}
