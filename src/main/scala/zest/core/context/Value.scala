package zest.core.context

import scala.deriving.Mirror
import scala.compiletime.{erasedValue, summonInline}

enum Value:
  case StringValue(s: String)
  case NumberValue(n: Double)
  case BooleanValue(b: Boolean)
  case ObjectValue(fields: Map[String, Value])
  case ArrayValue(items: List[Value])
  case NullValue

object Value:
  given Conversion[String, Value] = StringValue(_)
  given Conversion[Int, Value] = n => NumberValue(n.toDouble)
  given Conversion[Double, Value] = NumberValue(_)
  given Conversion[Boolean, Value] = BooleanValue(_)
  given [A](using conv: Conversion[A, Value]): Conversion[Option[A], Value] with
    def apply(o: Option[A]): Value = o match {
      case Some(value) => conv(value)
      case None        => NullValue
    }
