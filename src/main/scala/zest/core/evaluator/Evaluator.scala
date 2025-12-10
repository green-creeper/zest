package zest.core.evaluator

import cats.MonadError
import cats.syntax.all._
import zest.core.ast.{BooleanLiteral, Expression, Variable}
import zest.core.context.{Context, Value}
import zest.errors.EvaluationError

trait Evaluator[F[_]]:
  def evaluate(expr: Expression, ctx: Context): F[Value]
  def evaluateBoolean(expr: Expression, ctx: Context): F[Boolean]

object Evaluator:
  def apply[F[_]](using me: MonadError[F, Throwable]): Evaluator[F] = new EvaluatorImpl[F]

private class EvaluatorImpl[F[_]](using me: MonadError[F, Throwable]) extends Evaluator[F]:
  def evaluate(expr: Expression, ctx: Context): F[Value] = expr match {
    case Variable(path) =>
      me.pure(ctx.get(path).getOrElse(Value.NullValue))
    case BooleanLiteral(value) =>
      me.pure(Value.BooleanValue(value))
  }

  def evaluateBoolean(expr: Expression, ctx: Context): F[Boolean] =
    evaluate(expr, ctx).map(isTruthy)

  private def isTruthy(value: Value): Boolean = value match {
    case Value.BooleanValue(b) => b
    case Value.StringValue(s) => s.nonEmpty
    case Value.NumberValue(n) => n != 0
    case Value.ObjectValue(fields) => fields.nonEmpty
    case Value.ArrayValue(items) => items.nonEmpty
    case Value.NullValue => false
  }
