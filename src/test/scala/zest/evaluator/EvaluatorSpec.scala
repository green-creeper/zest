package zest.evaluator

import munit.FunSuite
import zest.core.ast.{BooleanLiteral, Variable}
import zest.core.context.{Context, Value}
import zest.core.evaluator.Evaluator
import zest.errors.EvaluationError

class EvaluatorSpec extends FunSuite {
  type F[A] = Either[Throwable, A]
  val evaluator = Evaluator[F]

  test("should evaluate a boolean literal") {
    val expr = BooleanLiteral(true)
    val result = evaluator.evaluate(expr, Context.empty)
    assertEquals(result, Right(Value.BooleanValue(true)))
  }

  test("should evaluate a simple variable") {
    val expr = Variable(List("name"))
    val context = Context.fromMap(Map("name" -> "Alice"))
    val result = evaluator.evaluate(expr, context)
    assertEquals(result, Right(Value.StringValue("Alice")))
  }

  test("should evaluate a nested variable") {
    val expr = Variable(List("user", "name"))
    val context = Context.fromMap(Map("user" -> Map("name" -> "Alice")))
    val result = evaluator.evaluate(expr, context)
    assertEquals(result, Right(Value.StringValue("Alice")))
  }

  test("should return NullValue for a missing variable") {
    val expr = Variable(List("name"))
    val context = Context.empty
    val result = evaluator.evaluate(expr, context)
    assertEquals(result, Right(Value.NullValue))
  }
}