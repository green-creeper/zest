package com.greencreeper.zest.template

import munit.CatsEffectSuite
import java.io.File
import cats.effect.IO

class TemplateSpec extends CatsEffectSuite {

  def withTempTemplate(content: String)(testBody: File => IO[Unit]): IO[Unit] = {
    IO.delay(File.createTempFile("test_template", ".txt")).bracket { tempFile =>
      IO.delay(os.write.over(os.Path(tempFile.getAbsolutePath), content)) >> testBody(tempFile)
    } { tempFile =>
      IO.delay(tempFile.delete()).void
    }
  }

  case class User(name: String, age: Int, address: Address)
  case class Address(street: String, city: String)
  case class Item(id: Int, name: String, price: Option[Double])

  test("Template should render basic string substitution") {
    withTempTemplate("Hello, {{user.name}}!") { tempFile =>
      val user = User("Alice", 30, Address("Main St", "Anytown"))
      val context = Map("user" -> user)
      for {
        template <- Template.fromFile[IO](tempFile.getAbsolutePath)
        rendered <- template.render(context)
        _ <- IO(assertEquals(rendered, "Hello, Alice!"))
      } yield ()
    }
  }

  test("Template should render nested object substitution") {
    withTempTemplate("User lives on {{user.address.street}} in {{user.address.city}}.") { tempFile =>
      val user = User("Alice", 30, Address("Main St", "Anytown"))
      val context = Map("user" -> user)
      for {
        template <- Template.fromFile[IO](tempFile.getAbsolutePath)
        rendered <- template.render(context)
        _ <- IO(assertEquals(rendered, "User lives on Main St in Anytown."))
      } yield ()
    }
  }

  test("Template should fail on missing fields") {
    withTempTemplate("Hello, {{user.nonExistentField}}!") { tempFile =>
      val user = User("Alice", 30, Address("Main St", "Anytown"))
      val context = Map("user" -> user)
      val test = for {
        template <- Template.fromFile[IO](tempFile.getAbsolutePath)
        _ <- template.render(context)
      } yield ()
      test.attempt.map {
        case Left(e: NoSuchFieldException) =>
          assert(e.getMessage.contains("Field or method 'nonExistentField' not found"))
        case _ => fail("Expected a NoSuchFieldException")
      }
    }
  }

  test("Template should handle Option values") {
    withTempTemplate("Item price: {{item.price}}.") { tempFile =>
      val item = Item(1, "Book", Some(19.99))
      val context = Map("item" -> item)
      for {
        template <- Template.fromFile[IO](tempFile.getAbsolutePath)
        rendered <- template.render(context)
        _ <- IO(assertEquals(rendered, "Item price: Some(19.99)."))
      } yield ()
    }
  }

  test("Template should handle a field that is null") {
    case class WithNull(value: String, nullable: String)
    withTempTemplate("Value: {{obj.value}}, Nullable: {{obj.nullable}}.") { tempFile =>
      val obj = WithNull("test", null)
      val context = Map("obj" -> obj)
      for {
        template <- Template.fromFile[IO](tempFile.getAbsolutePath)
        rendered <- template.render(context)
        _ <- IO(assertEquals(rendered, "Value: test, Nullable: null."))
      } yield ()
    }
  }
}
