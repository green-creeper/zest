package zest.core.parser

import munit.*
import zest.core.ast.*
import zest.core.errors.TemplateError

class TemplateParserSpec extends FunSuite {

  test("parse simple text") {
    val template = "Hello world"
    val result = TemplateParser.parse(template)
    assert(result.isRight)
    result match {
      case Right(template) =>
        assertEquals(template.nodes.length, 1)
        template.nodes.head match {
          case TextNode(content, _) =>
            assertEquals(content, "Hello world")
          case _ => fail("Expected TextNode")
        }
      case Left(error) => fail(s"Unexpected error: $error")
    }
  }

  test("parse variable interpolation") {
    val template = "Hello {{ name }}"
    val result = TemplateParser.parse(template)
    assert(result.isRight)
    result match {
      case Right(template) =>
        assertEquals(template.nodes.length, 2)
        template.nodes match {
          case List(TextNode("Hello ", _), Interpolation(VariableReference("name", _), _)) =>
            // Success
          case _ => fail("Unexpected node structure")
        }
      case Left(error) => fail(s"Unexpected error: $error")
    }
  }

  test("parse string literal") {
    val template = "{{ \"hello\" }}"
    val result = TemplateParser.parse(template)
    assert(result.isRight)
    result match {
      case Right(template) =>
        template.nodes.head match {
          case Interpolation(StringLiteral("hello", _), _) =>
            // Success
          case _ => fail("Expected StringLiteral")
        }
      case Left(error) => fail(s"Unexpected error: $error")
    }
  }

  test("parse number literal") {
    val template = "{{ 42 }}"
    val result = TemplateParser.parse(template)
    assert(result.isRight)
    result match {
      case Right(template) =>
        template.nodes.head match {
          case Interpolation(NumberLiteral(42.0, _), _) =>
            // Success
          case _ => fail("Expected NumberLiteral")
        }
      case Left(error) => fail(s"Unexpected error: $error")
    }
  }

  test("parse boolean literal") {
    val template = "{{ true }}"
    val result = TemplateParser.parse(template)
    assert(result.isRight)
    result match {
      case Right(template) =>
        template.nodes.head match {
          case Interpolation(BooleanLiteral(true, _), _) =>
            // Success
          case _ => fail("Expected BooleanLiteral")
        }
      case Left(error) => fail(s"Unexpected error: $error")
    }
  }

  test("parse property access") {
    val template = "{{ user.name }}"
    val result = TemplateParser.parse(template)
    assert(result.isRight)
    result match {
      case Right(template) =>
        template.nodes.head match {
          case Interpolation(PropertyAccess(VariableReference("user", _), "name", _), _) =>
            // Success
          case _ => fail("Expected PropertyAccess")
        }
      case Left(error) => fail(s"Unexpected error: $error")
    }
  }

  test("parse if statement") {
    val template = "{% if condition %}content{% endif %}"
    val result = TemplateParser.parse(template)
    assert(result.isRight)
    result match {
      case Right(template) =>
        template.nodes.head match {
          case IfStatement(VariableReference("condition", _), body, None, _) =>
            assertEquals(body.length, 1)
            body.head match {
              case TextNode("content", _) =>
                // Success
              case _ => fail("Expected TextNode in if body")
            }
          case _ => fail("Expected IfStatement")
        }
      case Left(error) => fail(s"Unexpected error: $error")
    }
  }

  test("parse if-else statement") {
    val template = "{% if condition %}true{% else %}false{% endif %}"
    val result = TemplateParser.parse(template)
    assert(result.isRight)
    result match {
      case Right(template) =>
        template.nodes.head match {
          case IfStatement(VariableReference("condition", _), trueBody, Some(falseBody), _) =>
            assertEquals(trueBody.length, 1)
            assertEquals(falseBody.length, 1)
            trueBody.head match {
              case TextNode("true", _) => // Success
              case _ => fail("Expected 'true' text")
            }
            falseBody.head match {
              case TextNode("false", _) => // Success
              case _ => fail("Expected 'false' text")
            }
          case _ => fail("Expected IfStatement")
        }
      case Left(error) => fail(s"Unexpected error: $error")
    }
  }

  test("parse comment") {
    val template = "{# this is a comment #}"
    val result = TemplateParser.parse(template)
    assert(result.isRight)
    result match {
      case Right(template) =>
        template.nodes.head match {
          case Comment(" this is a comment ", _) =>
            // Success
          case _ => fail("Expected Comment")
        }
      case Left(error) => fail(s"Unexpected error: $error")
    }
  }

  test("parse mixed content") {
    val template = "Hello {{ name }}, you have {{ count }} messages."
    val result = TemplateParser.parse(template)
    assert(result.isRight)
    result match {
      case Right(template) =>
        assertEquals(template.nodes.length, 5)
        template.nodes match {
          case List(
            TextNode("Hello ", _),
            Interpolation(VariableReference("name", _), _),
            TextNode(", you have ", _),
            Interpolation(VariableReference("count", _), _),
            TextNode(" messages.", _)
          ) =>
            // Success
          case _ => fail("Unexpected node structure")
        }
      case Left(error) => fail(s"Unexpected error: $error")
    }
  }

  test("parse binary operation") {
    val template = "{{ a + b }}"
    val result = TemplateParser.parse(template)
    assert(result.isRight)
    result match {
      case Right(template) =>
        template.nodes.head match {
          case Interpolation(BinaryOperation(VariableReference("a", _), "+", VariableReference("b", _), _), _) =>
            // Success
          case _ => fail("Expected BinaryOperation")
        }
      case Left(error) => fail(s"Unexpected error: $error")
    }
  }

  test("parse complex expression") {
    val template = "{{ user.age > 18 && user.active }}"
    val result = TemplateParser.parse(template)
    assert(result.isRight)
    result match {
      case Right(template) =>
        template.nodes.head match {
          case Interpolation(
            BinaryOperation(
              BinaryOperation(
                PropertyAccess(VariableReference("user", _), "age", _),
                ">",
                NumberLiteral(18.0, _),
                _
              ),
              "&&",
              PropertyAccess(VariableReference("user", _), "active", _),
              _
            ),
            _
          ) =>
            // Success
          case _ => fail("Expected complex expression")
        }
      case Left(error) => fail(s"Unexpected error: $error")
    }
  }
}
