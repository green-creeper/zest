package zest.core.context

import munit.*
import zest.core.ast.*

class ContextSpec extends FunSuite {

  test("create empty context") {
    val context = Context.empty
    val result = context.getVariable("name")
    assert(result.isLeft)
    assertEquals(result.left.get, "Variable 'name' not found in context")
  }

  test("create context from map") {
    val data = Map("name" -> "Alice", "age" -> 30)
    val context = Context.fromMap(data)

    val nameResult = context.getVariable("name")
    assert(nameResult.isRight)
    nameResult match {
      case Right(StringValue("Alice")) => // Success
      case _ => fail(s"Expected StringValue('Alice'), got $nameResult")
    }

    val ageResult = context.getVariable("age")
    assert(ageResult.isRight)
    ageResult match {
      case Right(NumberValue(30.0)) => // Success
      case _ => fail(s"Expected NumberValue(30.0), got $ageResult")
    }
  }

  test("create context from nested map") {
    val data = Map(
      "user" -> Map(
        "name" -> "Alice",
        "profile" -> Map("active" -> true)
      )
    )
    val context = Context.fromMap(data)

    val userResult = context.getVariable("user")
    assert(userResult.isRight)
    userResult match {
      case Right(ObjectValue(props)) =>
        assertEquals(props.size, 2)
        props.get("name") match {
          case Some(StringValue("Alice")) => // Success
          case _ => fail("Expected name to be Alice")
        }
        props.get("profile") match {
          case Some(ObjectValue(profileProps)) =>
            profileProps.get("active") match {
              case Some(BooleanValue(true)) => // Success
              case _ => fail("Expected active to be true")
            }
          case _ => fail("Expected profile object")
        }
      case _ => fail(s"Expected ObjectValue, got $userResult")
    }
  }

  test("get property from object") {
    val data = Map("user" -> Map("name" -> "Alice"))
    val context = Context.fromMap(data)

    val userResult = context.getVariable("user").right.get
    val nameResult = context.getProperty(userResult, "name")
    assert(nameResult.isRight)
    nameResult match {
      case Right(StringValue("Alice")) => // Success
      case _ => fail(s"Expected StringValue('Alice'), got $nameResult")
    }
  }

  test("get property from string") {
    val data = Map("text" -> "hello")
    val context = Context.fromMap(data)

    val textResult = context.getVariable("text").right.get
    val lengthResult = context.getProperty(textResult, "length")
    assert(lengthResult.isRight)
    lengthResult match {
      case Right(NumberValue(5.0)) => // Success
      case _ => fail(s"Expected NumberValue(5.0), got $lengthResult")
    }
  }

  test("get property from number") {
    val data = Map("number" -> 42.0)
    val context = Context.fromMap(data)

    val numberResult = context.getVariable("number").right.get
    val intResult = context.getProperty(numberResult, "toInt")
    assert(intResult.isRight)
    intResult match {
      case Right(NumberValue(42.0)) => // Success (double representation of int)
      case _ => fail(s"Expected NumberValue(42.0), got $intResult")
    }
  }

  test("get property from list") {
    val data = Map("items" -> List("a", "b", "c"))
    val context = Context.fromMap(data)

    val itemsResult = context.getVariable("items").right.get
    val sizeResult = context.getProperty(itemsResult, "size")
    assert(sizeResult.isRight)
    sizeResult match {
      case Right(NumberValue(3.0)) => // Success
      case _ => fail(s"Expected NumberValue(3.0), got $sizeResult")
    }
  }

  test("get index from string") {
    val data = Map("text" -> "hello")
    val context = Context.fromMap(data)

    val textResult = context.getVariable("text").right.get
    val indexResult = context.getIndex(textResult, Value.fromNumber(0))
    assert(indexResult.isRight)
    indexResult match {
      case Right(StringValue("h")) => // Success
      case _ => fail(s"Expected StringValue('h'), got $indexResult")
    }
  }

  test("get index from list") {
    val data = Map("items" -> List("a", "b", "c"))
    val context = Context.fromMap(data)

    val itemsResult = context.getVariable("items").right.get
    val indexResult = context.getIndex(itemsResult, Value.fromNumber(1))
    assert(indexResult.isRight)
    indexResult match {
      case Right(StringValue("b")) => // Success
      case _ => fail(s"Expected StringValue('b'), got $indexResult")
    }
  }

  test("get index from object") {
    val data = Map("obj" -> Map("key" -> "value"))
    val context = Context.fromMap(data)

    val objResult = context.getVariable("obj").right.get
    val indexResult = context.getIndex(objResult, Value.fromString("key"))
    assert(indexResult.isRight)
    indexResult match {
      case Right(StringValue("value")) => // Success
      case _ => fail(s"Expected StringValue('value'), got $indexResult")
    }
  }

  test("type conversion") {
    // Test string conversion
    val stringVal = Value.fromString("hello")
    assertEquals(stringVal.asString, Right("hello"))
    assertEquals(stringVal.asBoolean, Right(true)) // non-empty string
    assertEquals(stringVal.asNumber, Left("Cannot convert StringValue(hello) to Number"))

    // Test number conversion
    val numberVal = Value.fromNumber(42.0)
    assertEquals(numberVal.asString, Right("42.0"))
    assertEquals(numberVal.asBoolean, Right(true)) // non-zero number
    assertEquals(numberVal.asNumber, Right(42.0))

    // Test boolean conversion
    val boolVal = Value.fromBoolean(true)
    assertEquals(boolVal.asString, Right("true"))
    assertEquals(boolVal.asBoolean, Right(true))
    assertEquals(boolVal.asNumber, Left("Cannot convert BooleanValue(true) to Number"))

    // Test null conversion
    val nullVal = Value.NullValue
    assertEquals(nullVal.asString, Right("null"))
    assertEquals(nullVal.asBoolean, Left("Cannot convert NullValue to Boolean"))
  }

  test("create context from case class") {
    case class User(name: String, age: Int, active: Boolean)

    val user = User("Alice", 30, true)
    val context = Context.fromCaseClass(user)

    val nameResult = context.getVariable("name")
    assert(nameResult.isRight)
    nameResult match {
      case Right(StringValue("Alice")) => // Success
      case _ => fail(s"Expected StringValue('Alice'), got $nameResult")
    }

    val ageResult = context.getVariable("age")
    assert(ageResult.isRight)
    ageResult match {
      case Right(NumberValue(30.0)) => // Success
      case _ => fail(s"Expected NumberValue(30.0), got $ageResult")
    }
  }
}
