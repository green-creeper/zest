package zest.core.context

import scala.collection.immutable.ListMap

/** Represents a value in the template context */
sealed trait Value {
  def asString: Either[String, String] = this match {
    case StringValue(s) => Right(s)
    case NumberValue(n) => Right(n.toString)
    case BooleanValue(b) => Right(b.toString)
    case _ => Left(s"Cannot convert $this to String")
  }

  def asNumber: Either[String, Double] = this match {
    case NumberValue(n) => Right(n)
    case _ => Left(s"Cannot convert $this to Number")
  }

  def asBoolean: Either[String, Boolean] = this match {
    case BooleanValue(b) => Right(b)
    case NumberValue(n) => Right(n != 0)
    case StringValue(s) => Right(s.nonEmpty)
    case _ => Left(s"Cannot convert $this to Boolean")
  }

  def asList: Either[String, List[Value]] = this match {
    case ListValue(items) => Right(items)
    case _ => Left(s"Cannot convert $this to List")
  }

  def asObject: Either[String, ListMap[String, Value]] = this match {
    case ObjectValue(props) => Right(props)
    case _ => Left(s"Cannot convert $this to Object")
  }
}

case object NullValue extends Value

case class StringValue(value: String) extends Value

case class NumberValue(value: Double) extends Value

case class BooleanValue(value: Boolean) extends Value

case class ListValue(items: List[Value]) extends Value

case class ObjectValue(properties: ListMap[String, Value]) extends Value

object Value {
  def fromAny(value: Any): Value = value match {
    case null => NullValue
    case s: String => StringValue(s)
    case n: Number => NumberValue(n.doubleValue())
    case b: Boolean => BooleanValue(b)
    case l: List[_] => ListValue(l.map(fromAny))
    case s: Seq[_] => ListValue(s.toList.map(fromAny))
    case m: Map[_, _] =>
      val props = m.map { case (k, v) => k.toString -> fromAny(v) }
      ObjectValue(ListMap(props.toSeq: _*))
    case m: java.util.Map[_, _] =>
      val props = m.asScala.map { case (k, v) => k.toString -> fromAny(v) }
      ObjectValue(ListMap(props.toSeq: _*))
    case _ =>
      // Try to extract case class fields
      try {
        val fields = value.getClass.getDeclaredFields
        val props = fields.map { field =>
          field.setAccessible(true)
          field.getName -> fromAny(field.get(value))
        }
        ObjectValue(ListMap(props: _*))
      } catch {
        case _: Exception => StringValue(value.toString)
      }
  }

  def fromString(value: String): Value = StringValue(value)
  def fromNumber(value: Double): Value = NumberValue(value)
  def fromBoolean(value: Boolean): Value = BooleanValue(value)
  def fromList(value: List[Value]): Value = ListValue(value)
  def fromObject(value: ListMap[String, Value]): Value = ObjectValue(value)
}
