package zest.core.context

import scala.collection.immutable.ListMap

/** Template context for variable lookup and evaluation */
trait Context {
  def getVariable(name: String): Either[String, Value]
  def getProperty(obj: Value, property: String): Either[String, Value]
  def getIndex(obj: Value, index: Value): Either[String, Value]
}

object Context {
  def empty: Context = new Context {
    def getVariable(name: String): Either[String, Value] =
      Left(s"Variable '$name' not found in context")

    def getProperty(obj: Value, property: String): Either[String, Value] =
      Left(s"Property '$property' not found")

    def getIndex(obj: Value, index: Value): Either[String, Value] =
      Left(s"Index access not supported for this type")
  }

  def fromMap(data: Map[String, Any]): Context = {
    val values = data.map { case (k, v) => k -> Value.fromAny(v) }
    fromValueMap(values.toMap)
  }

  def fromValueMap(data: Map[String, Value]): Context = new Context {
    private val variables = data

    def getVariable(name: String): Either[String, Value] =
      variables.get(name) match {
        case Some(value) => Right(value)
        case None => Left(s"Variable '$name' not found in context")
      }

    def getProperty(obj: Value, property: String): Either[String, Value] =
      obj match {
        case ObjectValue(props) =>
          props.get(property) match {
            case Some(value) => Right(value)
            case None => Left(s"Property '$property' not found")
          }
        case StringValue(s) =>
          property match {
            case "length" => Right(Value.fromNumber(s.length))
            case "toUpperCase" => Right(Value.fromString(s.toUpperCase))
            case "toLowerCase" => Right(Value.fromString(s.toLowerCase))
            case _ => Left(s"String property '$property' not found")
          }
        case NumberValue(n) =>
          property match {
            case "toInt" => Right(Value.fromNumber(n.toInt))
            case "toDouble" => Right(Value.fromNumber(n))
            case _ => Left(s"Number property '$property' not found")
          }
        case ListValue(items) =>
          property match {
            case "size" => Right(Value.fromNumber(items.size))
            case "isEmpty" => Right(Value.fromBoolean(items.isEmpty))
            case _ => Left(s"List property '$property' not found")
          }
        case _ => Left(s"Property access not supported for ${obj.getClass.getSimpleName}")
      }

    def getIndex(obj: Value, index: Value): Either[String, Value] =
      (obj, index) match {
        case (StringValue(s), NumberValue(n)) if n.isValidInt && n >= 0 && n < s.length =>
          Right(Value.fromString(s.charAt(n.toInt).toString))
        case (StringValue(s), StringValue(key)) =>
          // Try to treat as property access
          getProperty(obj, key)
        case (ListValue(items), NumberValue(n)) if n.isValidInt && n >= 0 && n < items.size =>
          Right(items(n.toInt))
        case (ObjectValue(props), StringValue(key)) =>
          props.get(key) match {
            case Some(value) => Right(value)
            case None => Left(s"Key '$key' not found in object")
          }
        case _ => Left(s"Index access not supported for this type combination")
      }
  }
}
