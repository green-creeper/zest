package com.greencreeper.zest.examples

import cats.effect.{IO, IOApp, Resource}
import com.greencreeper.zest.template.Template
import scala.io.Source
import java.io.File

object Main extends IOApp.Simple {

  case class User(name: String, age: Int, address: Address)
  case class Address(street: String, city: String)

  def run: IO[Unit] = {
    // Read HTML template content from resource file
    val htmlTemplateContentIO: IO[String] = IO.delay {
      val source = Source.fromResource("user_profile.html")
      try source.mkString finally source.close()
    }

    // Markdown content as a string literal
    val markdownTemplateContent: String =
      """# User Profile (Markdown)
        |
        |- **Name:** {{user.name}}
        |- **Age:** {{user.age}}
        |- **Address:** {{user.address.street}}, {{user.address.city}}
      """.stripMargin

    val alice = User("Alice Wonderland", 30, Address("Rabbit Hole", "Fantasy Land"))
    val addressMap = Map(
      "street" -> alice.address.street,
      "city" -> alice.address.city
    )
    val userMap = Map(
      "name" -> alice.name,
      "age" -> alice.age,
      "address" -> addressMap // Embed the address map
    )
    val context = Map("user" -> userMap)

    for {
      htmlTemplateContent <- htmlTemplateContentIO

      // Example using Template.fromString with Markdown literal
      _ <- IO.println("--- Rendering with Template.fromString (Markdown Literal) ---")
      templateFromString <- Template.fromString[IO](markdownTemplateContent)
      renderedMdFromString <- templateFromString.render(context)
      _ <- IO.println(renderedMdFromString)

      // Example using Template.fromFile (via temporary file from HTML resource)
      _ <- IO.println("--- Rendering with Template.fromFile (HTML Resource) ---")
      _ <- Resource.make {
        IO.delay(File.createTempFile("user_profile_from_file", ".html")).flatTap { file =>
          IO.delay(os.write.over(os.Path(file.getAbsolutePath), htmlTemplateContent))
        }
      } { file =>
        IO.delay(file.delete()).void
      }.use { tempFile =>
        for {
          templateFromFile <- Template.fromFile[IO](tempFile.getAbsolutePath)
          renderedHtmlFromFile <- templateFromFile.render(context)
          _ <- IO.println(renderedHtmlFromFile)
        } yield ()
      }
    } yield ()
  }
}
