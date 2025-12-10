package com.greencreeper.zest.template

import munit.FunSuite
import java.io.File
import scala.io.Source

class TemplateSpec extends FunSuite {

  // Helper to create a temporary template file
  def withTempTemplate(content: String)(testBody: File => Any): Any = {
    val tempFile = File.createTempFile("test_template", ".txt")
    try {
      os.write.over(os.Path(tempFile.getAbsolutePath), content)
      testBody(tempFile)
    } finally {
      tempFile.delete()
    }
  }

  case class User(name: String, age: Int, address: Address)
  case class Address(street: String, city: String)
  case class Item(id: Int, name: String, price: Option[Double])

  test("Template should render basic string substitution") {
    withTempTemplate("Hello, {{user.name}}!") { tempFile =>
      val user = User("Alice", 30, Address("Main St", "Anytown"))
      val context = Map("user" -> user)
      val template = Template.fromFile(tempFile.getAbsolutePath)
      assertEquals(template.render(context), "Hello, Alice!")
    }
  }

  test("Template should render nested object substitution") {
    withTempTemplate("User lives on {{user.address.street}} in {{user.address.city}}.") { tempFile =>
      val user = User("Alice", 30, Address("Main St", "Anytown"))
      val context = Map("user" -> user)
      val template = Template.fromFile(tempFile.getAbsolutePath)
      assertEquals(template.render(context), "User lives on Main St in Anytown.")
    }
  }

  test("Template should handle missing fields gracefully") {
    withTempTemplate("Hello, {{user.nonExistentField}}!") { tempFile =>
      val user = User("Alice", 30, Address("Main St", "Anytown"))
      val context = Map("user" -> user)
      val template = Template.fromFile(tempFile.getAbsolutePath)
      assert(template.render(context).contains("ERROR: Field or method 'nonExistentField' not found"))
    }
  }

  test("Template should handle null values gracefully") {
    withTempTemplate("User name: {{user.name}}, Age: {{user.age}}, Item price: {{item.price}}.") { tempFile =>
      val item = Item(1, "Book", None)
      val user = User("Bob", 25, Address("Third Ave", "Otherville"))
      val context = Map("user" -> user, "item" -> item)
      val template = Template.fromFile(tempFile.getAbsolutePath)
      assertEquals(template.render(context), "User name: Bob, Age: 25, Item price: None.")
    }
  }

  test("Template should handle a field that is null") {
    case class WithNull(value: String, nullable: String)
    withTempTemplate("Value: {{obj.value}}, Nullable: {{obj.nullable}}.") { tempFile =>
      val obj = WithNull("test", null)
      val context = Map("obj" -> obj)
      val template = Template.fromFile(tempFile.getAbsolutePath)
      assertEquals(template.render(context), "Value: test, Nullable: null.")
    }
  }

  test("placeholderRegex should correctly match placeholders") {
    val templateString = "Hello, {{user.name}}! Your age is {{user.age}}."
    val matches = Template.placeholderRegex.findAllMatchIn(templateString).toList

    assertEquals(matches.length, 2)
    assertEquals(matches(0).group(1), "user.name")
    assertEquals(matches(1).group(1), "user.age")

    val simpleTemplate = "{{value}}"
    val simpleMatches = Template.placeholderRegex.findAllMatchIn(simpleTemplate).toList
    assertEquals(simpleMatches.length, 1)
    assertEquals(simpleMatches(0).group(1), "value")

    val noMatches = "No placeholders here."
    assertEquals(Template.placeholderRegex.findAllMatchIn(noMatches).toList.length, 0)
  }
}
