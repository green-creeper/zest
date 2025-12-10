package zest.core.context

import scala.deriving.Mirror
import scala.compiletime.{constValue, erasedValue, summonInline}

trait Context:
  def get(path: List[String]): Option[Value]
  def set(path: List[String], value: Value): Context

private class MapContext(private val data: Map[String, Value]) extends Context:
  def get(path: List[String]): Option[Value] = path match {
    case Nil => None
    case head :: tail =>
      data.get(head).flatMap {
        case v: Value.ObjectValue if tail.nonEmpty => new MapContext(v.fields).get(tail)
        case v if tail.isEmpty => Some(v)
        case _ => None
      }
  }

  def set(path: List[String], value: Value): Context = {
    // Basic implementation, a more robust one would handle nested sets
    path match {
      case Nil => this
      case head :: Nil => new MapContext(data + (head -> value))
      case _ => this // Not implemented for nested paths yet
    }
  }

object Context:
  def empty: Context = new MapContext(Map.empty)

  def fromMap(data: Map[String, Any]): Context = {
    val valueMap = data.map { (k, v) =>
      k -> anyToValue(v)
    }
    new MapContext(valueMap)
  }

  // A simplified anyToValue, we can make it more robust later
  private def anyToValue(any: Any): Value = any match {
    case s: String => Value.StringValue(s)
    case i: Int => Value.NumberValue(i.toDouble)
    case d: Double => Value.NumberValue(d)
    case b: Boolean => Value.BooleanValue(b)
    case null => Value.NullValue
    case m: Map[_, _] =>
      val stringKeyMap = m.collect { case (k: String, v) => k -> anyToValue(v) }
      Value.ObjectValue(stringKeyMap.toMap)
    case l: List[_] => Value.ArrayValue(l.map(anyToValue))
    case _ => Value.StringValue(any.toString) // Fallback
  }

  // fromCaseClass will be implemented later
  // def fromCaseClass[A](instance: A)(using Mirror.Of[A]): Context = ???
