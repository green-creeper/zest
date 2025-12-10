# Zest Templating Engine

A minimal, idiomatic Scala 3 templating engine designed for simple string substitutions in various output formats like HTML, shell scripts, or plain text. It allows binding a Scala object (or a `Map`) to a template and using its fields with a `{{object.value}}` syntax.

This version is implemented using a **tagless final** approach, allowing it to be used with any effect type that has a `cats.effect.Sync` instance (e.g., `cats.effect.IO`, `monix.eval.Task`).

## Features

*   **Tagless Final:** Flexible and non-blocking, works with any effect type `F[_]`.
*   **Simple Syntax:** Uses `{{object.value}}` for placeholder substitution.
*   **Object Binding:** Binds directly to Scala case classes or `Map[String, Any]` instances.
*   **Nested Access:** Supports accessing nested fields (e.g., `{{user.address.street}}`).
*   **File-based Templates:** Templates are loaded from external files.
*   **Error Handling:** Fails fast on missing fields by raising an error in `F`.

## Setup

To use this library in your project, add the library dependency to your `build.sbt` file:

```scala
libraryDependencies += "com.greencreeper" %% "zest" % "0.1.0"
```

## Usage

Here's how to use the templating engine with `cats.effect.IO`.

### 1. Define your data models

```scala
case class User(name: String, age: Int, address: Address)
case class Address(street: String, city: String)
```

### 2. Prepare your template file

Create a template file (e.g., `user_profile.html`):

```html
<!-- user_profile.html -->
<h1>User Profile</h1>
<p>Name: {{user.name}}</p>
<p>Age: {{user.age}}</p>
<p>Address: {{user.address.street}}, {{user.address.city}}</p>
```

### 3. Render the template

```scala
import cats.effect.{IO, IOApp}
import com.greencreeper.zest.template.Template
import java.io.File

object ExampleUsage extends IOApp.Simple {

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
```

### Error Handling

If a field is missing in the context, the `render` method will return a failed `F`. You can handle this using standard error handling methods for your chosen effect type.

```scala
import cats.effect.IO
import com.greencreeper.zest.template.Template

val template = Template.fromFile[IO]("path/to/template.txt")
val context = Map("user" -> "Alice") // Missing 'user.name'

template.flatMap(_.render(context)).attempt.map {
  case Left(e) => IO.println(s"Rendering failed: ${e.getMessage}")
  case Right(rendered) => IO.println(rendered)
}
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
