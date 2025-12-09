package zest.examples.html

import cats.effect.IO
import zest.core.api.Template
import zest.core.errors.TemplateError

object HtmlExample extends App {
  // Define data structures for type-safe templates
  case class User(name: String, email: String, admin: Boolean, joinedYear: Int)
  case class Page(title: String, user: User, items: List[String])

  // Example 1: Simple HTML template
  val simpleTemplate = """<!DOCTYPE html>
<html>
<head>
    <title>{{ title }}</title>
</head>
<body>
    <h1>Welcome, {{ user.name }}!</h1>
    <p>Email: {{ user.email }}</p>
    {% if user.admin %}
    <p><strong>You are an administrator.</strong></p>
    {% else %}
    <p>You are a regular user.</p>
    {% endif %}
    <p>Member since {{ user.joinedYear }}.</p>
</body>
</html>"""

  // Example 2: Dynamic list rendering
  val listTemplate = """<ul>
{% for item in items %}
    <li>{{ item }}</li>
{% endfor %}
</ul>"""

  def run(): IO[Unit] = {
    IO.println("=== Zest HTML Generation Examples ===\n")

    // Render simple template
    for {
      _ <- IO.println("1. Simple HTML Template:")
      templateResult <- IO.fromEither(Template(simpleTemplate).build[Page]())
      user = User("Alice Smith", "alice@example.com", true, 2023)
      page = Page("My Website", user, List("Home", "About", "Contact"))
      renderResult <- templateResult.render(page)

      _ <- renderResult match {
        case Right(html) => IO.println(html)
        case Left(error: TemplateError) =>
          IO.println(s"Error: ${TemplateError.formatError(error)}")
      }

      _ <- IO.println("\n2. List Rendering Template:")
      listTemplateResult <- IO.fromEither(Template(listTemplate).build[Page]())
      listRenderResult <- listTemplateResult.render(page)

      _ <- listRenderResult match {
        case Right(html) => IO.println(html)
        case Left(error: TemplateError) =>
          IO.println(s"Error: ${TemplateError.formatError(error)}")
      }

      _ <- IO.println("\n=== Examples Complete ===")
    } yield ()
  }

  // Run the example
  run().unsafeRunSync()
}
