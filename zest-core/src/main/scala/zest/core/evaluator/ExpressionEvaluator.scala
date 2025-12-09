package zest.core.evaluator

import cats.effect.IO
import zest.core.ast.*
import zest.core.context.*
import zest.core.errors.*

/** Evaluates expressions in the context of template rendering */
trait ExpressionEvaluator {
  def evaluate(expr: Expression, context: Context): IO[Either[TemplateError, Value]]
}

object ExpressionEvaluator {
  def apply(): ExpressionEvaluator = new ExpressionEvaluatorImpl

  private class ExpressionEvaluatorImpl extends ExpressionEvaluator {
    def evaluate(expr: Expression, context: Context): IO[Either[TemplateError, Value]] = {
      expr match {
        case VariableReference(name, location) =>
          IO.pure(context.getVariable(name).left.map { msg =>
            TemplateError.EvaluationError(msg, location.template, Some((location.line, location.column)))
          })

        case StringLiteral(value, _) =>
          IO.pure(Right(StringValue(value)))

        case NumberLiteral(value, _) =>
          IO.pure(Right(NumberValue(value)))

        case BooleanLiteral(value, _) =>
          IO.pure(Right(BooleanValue(value)))

        case PropertyAccess(target, property, location) =>
          evaluate(target, context).flatMap {
            case Left(error) => IO.pure(Left(error))
            case Right(targetValue) =>
              context.getProperty(targetValue, property) match {
                case Left(error) =>
                  IO.pure(Left(TemplateError.ContextError(
                    s"Property access error: $error",
                    location.template,
                    Some((location.line, location.column))
                  )))
                case Right(result) => IO.pure(Right(result))
              }
          }

        case IndexAccess(target, indexExpr, location) =>
          for {
            targetValue <- evaluate(target, context)
            indexValue <- evaluate(indexExpr, context)
          } yield {
            (targetValue, indexValue) match {
              case (Left(error), _) => Left(error)
              case (_, Left(error)) => Left(error)
              case (Right(targetVal), Right(indexVal)) =>
                context.getIndex(targetVal, indexVal) match {
                  case Left(error) =>
                    Left(TemplateError.ContextError(
                      s"Index access error: $error",
                      location.template,
                      Some((location.line, location.column))
                    ))
                  case Right(result) => Right(result)
                }
            }
          }

        case BinaryOperation(left, operator, right, location) =>
          for {
            leftValue <- evaluate(left, context)
            rightValue <- evaluate(right, context)
          } yield {
            (leftValue, rightValue) match {
              case (Left(error), _) => Left(error)
              case (_, Left(error)) => Left(error)
              case (Right(leftVal), Right(rightVal)) =>
                evaluateBinaryOperation(operator, leftVal, rightVal, location)
            }
          }

        case UnaryOperation(operator, operand, location) =>
          evaluate(operand, context).map {
            case Left(error) => Left(error)
            case Right(value) =>
              evaluateUnaryOperation(operator, value, location)
          }

        case FunctionCall(name, args, location) =>
          evaluateFunctionCall(name, args, context, location)

        case FilterExpression(expression, filters, location) =>
          evaluate(expression, context).flatMap {
            case Left(error) => IO.pure(Left(error))
            case Right(value) =>
              applyFilters(value, filters, context, location)
          }
      }
    }

    private def evaluateBinaryOperation(
      operator: String,
      left: Value,
      right: Value,
      location: SourceLocation
    ): Either[TemplateError, Value] = {
      (left, right) match {
        case (StringValue(l), StringValue(r)) =>
          operator match {
            case "+" => Right(StringValue(l + r))
            case "==" => Right(BooleanValue(l == r))
            case "!=" => Right(BooleanValue(l != r))
            case _ => Left(makeError(s"Operator '$operator' not supported for strings", location))
          }

        case (NumberValue(l), NumberValue(r)) =>
          operator match {
            case "+" => Right(NumberValue(l + r))
            case "-" => Right(NumberValue(l - r))
            case "*" => Right(NumberValue(l * r))
            case "/" =>
              if (r == 0) Left(makeError("Division by zero", location))
              else Right(NumberValue(l / r))
            case "%" => Right(NumberValue(l % r))
            case "==" => Right(BooleanValue(l == r))
            case "!=" => Right(BooleanValue(l != r))
            case "<" => Right(BooleanValue(l < r))
            case ">" => Right(BooleanValue(l > r))
            case "<=" => Right(BooleanValue(l <= r))
            case ">=" => Right(BooleanValue(l >= r))
            case _ => Left(makeError(s"Operator '$operator' not supported for numbers", location))
          }

        case (BooleanValue(l), BooleanValue(r)) =>
          operator match {
            case "&&" => Right(BooleanValue(l && r))
            case "||" => Right(BooleanValue(l || r))
            case "==" => Right(BooleanValue(l == r))
            case "!=" => Right(BooleanValue(l != r))
            case _ => Left(makeError(s"Operator '$operator' not supported for booleans", location))
          }

        case _ =>
          // Try to convert to common type
          (left.asNumber, right.asNumber) match {
            case (Right(lNum), Right(rNum)) =>
              evaluateBinaryOperation(operator, NumberValue(lNum), NumberValue(rNum), location)
            case _ =>
              Left(makeError(s"Type mismatch in binary operation: cannot apply '$operator' to $left and $right", location))
          }
      }
    }

    private def evaluateUnaryOperation(
      operator: String,
      value: Value,
      location: SourceLocation
    ): Either[TemplateError, Value] = {
      operator match {
        case "!" =>
          value.asBoolean match {
            case Right(b) => Right(BooleanValue(!b))
            case Left(_) => Left(makeError(s"Cannot apply '!' to $value", location))
          }
        case "-" =>
          value.asNumber match {
            case Right(n) => Right(NumberValue(-n))
            case Left(_) => Left(makeError(s"Cannot apply '-' to $value", location))
          }
        case _ => Left(makeError(s"Unknown unary operator: $operator", location))
      }
    }

    private def evaluateFunctionCall(
      name: String,
      args: List[Expression],
      context: Context,
      location: SourceLocation
    ): IO[Either[TemplateError, Value]] = {
      // For now, implement basic built-in functions
      args match {
        case Nil =>
          name match {
            case "now" => IO.pure(Right(StringValue(java.time.Instant.now().toString)))
            case "random" => IO.pure(Right(NumberValue(math.random())))
            case _ => IO.pure(Left(makeError(s"Function '$name' not found or requires arguments", location)))
          }
        case _ =>
          name match {
            case "length" =>
              evaluate(args.head, context).map {
                case Left(error) => Left(error)
                case Right(value) =>
                  value match {
                    case StringValue(s) => Right(NumberValue(s.length))
                    case ListValue(items) => Right(NumberValue(items.size))
                    case _ => Left(makeError("length() function requires a string or list", location))
                  }
              }
            case "upper" | "toUpperCase" =>
              evaluate(args.head, context).map {
                case Left(error) => Left(error)
                case Right(StringValue(s)) => Right(StringValue(s.toUpperCase))
                case Right(value) => Left(makeError(s"upper() function requires a string, got $value", location))
              }
            case "lower" | "toLowerCase" =>
              evaluate(args.head, context).map {
                case Left(error) => Left(error)
                case Right(StringValue(s)) => Right(StringValue(s.toLowerCase))
                case Right(value) => Left(makeError(s"lower() function requires a string, got $value", location))
              }
            case _ => IO.pure(Left(makeError(s"Function '$name' not found", location)))
          }
      }
    }

    private def applyFilters(
      value: Value,
      filters: List[FunctionCall],
      context: Context,
      location: SourceLocation
    ): IO[Either[TemplateError, Value]] = {
      filters.foldLeft(IO.pure(Right(value))) {
        case (ioResult, filter) =>
          ioResult.flatMap {
            case Left(error) => IO.pure(Left(error))
            case Right(currentValue) =>
              evaluateFunctionCall(filter.name, filter.arguments, context, location)
          }
      }
    }

    private def makeError(message: String, location: SourceLocation): TemplateError = {
      TemplateError.EvaluationError(
        message,
        location.template,
        Some((location.line, location.column))
      )
    }
  }
}
