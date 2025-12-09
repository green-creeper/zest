package zest.core.evaluator

import munit.*
import zest.core.ast.*
import zest.core.context.*
import zest.core.errors.TemplateError

class ExpressionEvaluatorSpec extends FunSuite {

  private val evaluator = ExpressionEvaluator()
  private val location = SourceLocation("test", 1, 1)

  private def createContext(data: Map[String, Any]): Context = {
    Context.fromMap(data)
  }

  test("evaluate variable reference") {
    val context = createContext(Map("name" -> "World"))
    val expr = VariableReference("name", location)

    val result = evaluator.evaluate(expr, context).unsafeRunSync()
    assert(result.isRight)
    result match {
      case Right(Value.StringValue("World")) => // Success
      case _ => fail(s"Expected StringValue('World'), got $result")
    }
  }

  test("evaluate missing variable") {
    val context = createContext(Map.empty)
    val expr = VariableReference("name", location)

    val result = evaluator.evaluate(expr, context).unsafeRunSync()
    assert(result.isLeft)
    result match {
      case Left(TemplateError.ContextError(_, _, _)) => // Success
      case _ => fail(s"Expected ContextError, got $result")
    }
  }

  test("evaluate string literal") {
    val context = createContext(Map.empty)
    val expr = StringLiteral("hello", location)

    val result = evaluator.evaluate(expr, context).unsafeRunSync()
    assert(result.isRight)
    result match {
      case Right(Value.StringValue("hello")) => // Success
      case _ => fail(s"Expected StringValue('hello'), got $result")
    }
  }

  test("evaluate number literal") {
    val context = createContext(Map.empty)
    val expr = NumberLiteral(42.0, location)

    val result = evaluator.evaluate(expr, context).unsafeRunSync()
    assert(result.isRight)
    result match {
      case Right(Value.NumberValue(42.0)) => // Success
      case _ => fail(s"Expected NumberValue(42.0), got $result")
    }
  }

  test("evaluate boolean literal") {
    val context = createContext(Map.empty)
    val expr = BooleanLiteral(true, location)

    val result = evaluator.evaluate(expr, context).unsafeRunSync()
    assert(result.isRight)
    result match {
      case Right(Value.BooleanValue(true)) => // Success
      case _ => fail(s"Expected BooleanValue(true), got $result")
    }
  }

  test("evaluate property access") {
    val context = createContext(Map(
      "user" -> Map("name" -> "Alice", "age" -> 30)
    ))
    val expr = PropertyAccess(VariableReference("user", location), "name", location)

    val result = evaluator.evaluate(expr, context).unsafeRunSync()
    assert(result.isRight)
    result match {
      case Right(Value.StringValue("Alice")) => // Success
      case _ => fail(s"Expected StringValue('Alice'), got $result")
    }
  }

  test("evaluate property access on string") {
    val context = createContext(Map("text" -> "hello"))
    val expr = PropertyAccess(VariableReference("text", location), "length", location)

    val result = evaluator.evaluate(expr, context).unsafeRunSync()
    assert(result.isRight)
    result match {
      case Right(Value.NumberValue(5.0)) => // Success
      case _ => fail(s"Expected NumberValue(5.0), got $result")
    }
  }

  test("evaluate index access on string") {
    val context = createContext(Map("text" -> "hello"))
    val indexExpr = NumberLiteral(0.0, location)
    val expr = IndexAccess(VariableReference("text", location), indexExpr, location)

    val result = evaluator.evaluate(expr, context).unsafeRunSync()
    assert(result.isRight)
    result match {
      case Right(Value.StringValue("h")) => // Success
      case _ => fail(s"Expected StringValue('h'), got $result")
    }
  }

  test("evaluate binary operation addition") {
    val context = createContext(Map("a" -> 10, "b" -> 20))
    val left = VariableReference("a", location)
    val right = VariableReference("b", location)
    val expr = BinaryOperation(left, "+", right, location)

    val result = evaluator.evaluate(expr, context).unsafeRunSync()
    assert(result.isRight)
    result match {
      case Right(Value.NumberValue(30.0)) => // Success
      case _ => fail(s"Expected NumberValue(30.0), got $result")
    }
  }

  test("evaluate binary operation comparison") {
    val context = createContext(Map("a" -> 10, "b" -> 20))
    val left = VariableReference("a", location)
    val right = VariableReference("b", location)
    val expr = BinaryOperation(left, ">", right, location)

    val result = evaluator.evaluate(expr, context).unsafeRunSync()
    assert(result.isRight)
    result match {
      case Right(Value.BooleanValue(false)) => // Success
      case _ => fail(s"Expected BooleanValue(false), got $result")
    }
  }

  test("evaluate unary operation") {
    val context = createContext(Map("value" -> true))
    val operand = VariableReference("value", location)
    val expr = UnaryOperation("!", operand, location)

    val result = evaluator.evaluate(expr, context).unsafeRunSync()
    assert(result.isRight)
    result match {
      case Right(Value.BooleanValue(false)) => // Success
      case _ => fail(s"Expected BooleanValue(false), got $result")
    }
  }

  test("evaluate function call length") {
    val context = createContext(Map("text" -> "hello"))
    val arg = VariableReference("text", location)
    val expr = FunctionCall("length", List(arg), location)

    val result = evaluator.evaluate(expr, context).unsafeRunSync()
    assert(result.isRight)
    result match {
      case Right(Value.NumberValue(5.0)) => // Success
      case _ => fail(s"Expected NumberValue(5.0), got $result")
    }
  }

  test("evaluate function call upper") {
    val context = createContext(Map("text" -> "hello"))
    val arg = VariableReference("text", location)
    val expr = FunctionCall("upper", List(arg), location)

    val result = evaluator.evaluate(expr, context).unsafeRunSync()
    assert(result.isRight)
    result match {
      case Right(Value.StringValue("HELLO")) => // Success
      case _ => fail(s"Expected StringValue('HELLO'), got $result")
    }
  }

  test("evaluate complex expression") {
    val context = createContext(Map(
      "user" -> Map("age" -> 25, "active" -> true)
    ))
    val ageExpr = PropertyAccess(VariableReference("user", location), "age", location)
    val ageComparison = BinaryOperation(ageExpr, ">", NumberLiteral(18.0, location), location)
    val activeExpr = PropertyAccess(VariableReference("user", location), "active", location)
    val expr = BinaryOperation(ageComparison, "&&", activeExpr, location)

    val result = evaluator.evaluate(expr, context).unsafeRunSync()
    assert(result.isRight)
    result match {
      case Right(Value.BooleanValue(true)) => // Success
      case _ => fail(s"Expected BooleanValue(true), got $result")
    }
  }
}
