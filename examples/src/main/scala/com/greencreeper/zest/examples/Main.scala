package com.greencreeper.zest.examples

import cats.effect.{IO, IOApp}
import com.greencreeper.zest.template.Template
import java.io.File

object Main extends IOApp.Simple {

  case class User(name: String, age: Int, address: Address)
  case class Address(street: String, city: String)

  def run: IO[Unit] = {
    val templateContent =
      """
        |<h1>User Profile</h1>
        |<p>Name: {{user.name}}</p>
        |<p>Age: {{user.age}}</p>
        |<p>Address: {{user.address.street}}, {{user.address.city}}</p>
      """.stripMargin

    val tempFileResource = IO.delay(File.createTempFile("user_profile_template", ".html")).bracket { file =>
      IO.delay(os.write.over(os.Path(file.getAbsolutePath), templateContent)).as(file)
    } { file =>
      IO.delay(file.delete()).void
    }

    tempFileResource.use { tempFile =>
      val alice = User("Alice Wonderland", 30, Address("Rabbit Hole", "Fantasy Land"))
      val context = Map("user" -> alice)

      for {
        template <- Template.fromFile[IO](tempFile.getAbsolutePath)
        renderedHtml <- template.render(context)
        _ <- IO.println(renderedHtml)
      } yield ()
    }
  }
}
