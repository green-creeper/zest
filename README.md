# Zest Templating Engine

A minimal, idiomatic Scala 3 templating engine designed for simple string substitutions in various output formats like HTML, shell scripts, or plain text. It allows binding a Scala object (or a `Map`) to a template and using its fields with a `{{object.value}}` syntax.

## Features

*   **Simple Syntax:** Uses `{{object.value}}` for placeholder substitution.
*   **Object Binding:** Binds directly to Scala case classes or `Map[String, Any]` instances.
*   **Nested Access:** Supports accessing nested fields (e.g., `{{user.address.street}}`).
*   **File-based Templates:** Templates are loaded from external files.
*   **Basic Error Handling:** Provides informative error messages for missing fields or null values during path traversal.

## Setup

To include this templating engine in your Scala 3 project, add the following dependency to your `build.sbt` file:

```scala
// Assuming you have a project named 'zest' and it's organized under 'com.greencreeper'
// You might need to publish this as a local library or a proper artifact for wider use.
// For now, if within the same project, you can use the source directly.

// Example build.sbt snippet:
// ThisBuild / organization := "com.greencreeper"
// ThisBuild / scalaVersion := "3.4.1"
// ...
// libraryDependencies += "com.greencreeper" %% "zest-template" % "0.1.0-SNAPSHOT" // If published
```
Since this is a self-contained example within a single project, you just need the source files (`src/main/scala/com/greencreeper/zest/template/Template.scala`).

## Usage

Here's how to use the templating engine:

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
<p>Favorite Color: {{user.favoriteColor}}</p>
```

### 3. Render the template

```scala
import com.greencreeper.zest.template.Template
import java.io.File
import os.Path // Assuming os-lib is available for file operations

object ExampleUsage extends App {
  case class User(name: String, age: Int, address: Address)
  case class Address(street: String, city: String)

  val templateContent = """
    |<h1>User Profile</h1>
    |<p>Name: {{user.name}}</p>
    |<p>Age: {{user.age}}</p>
    |<p>Address: {{user.address.street}}, {{user.address.city}}</p>
    |<p>Favorite Color: {{user.favoriteColor}}</p>
  """.stripMargin

  // Create a temporary template file for demonstration
  val tempFile = File.createTempFile("user_profile_template", ".html")
  var missingFieldTempFile: File = null

  try {
    os.write.over(os.Path(tempFile.getAbsolutePath), templateContent)

    val alice = User("Alice Wonderland", 30, Address("Rabbit Hole", "Fantasy Land"))
    val context = Map("user" -> alice) // Context can be a Map or a case class instance directly

    val template = Template.fromFile(tempFile.getAbsolutePath)
    val renderedHtml = template.render(context)

    println("--- Rendered HTML ---")
    println(renderedHtml)

    // Demonstrate missing field handling
    val missingFieldTemplateContent = "Hello, {{user.nonExistent}}!"
    missingFieldTempFile = File.createTempFile("missing_field_template", ".txt")
    os.write.over(os.Path(missingFieldTempFile.getAbsolutePath), missingFieldTemplateContent)
    val missingFieldTemplate = Template.fromFile(missingFieldTempFile.getAbsolutePath)
    println("\n--- Demonstrating missing field handling ---")
    println(missingFieldTemplate.render(context))

  } finally {
    if (tempFile.exists()) tempFile.delete()
    if (missingFieldTempFile != null && missingFieldTempFile.exists()) missingFieldTempFile.delete()
  }
}
```

This will output:

```html
--- Rendered HTML ---
<h1>User Profile</h1>
<p>Name: Alice Wonderland</p>
<p>Age: 30</p>
<p>Address: Rabbit Hole, Fantasy Land</p>
<p>Favorite Color: ERROR: Field or method 'favoriteColor' not found in com.greencreeper.zest.ExampleUsage$User</p>

--- Demonstrating missing field handling ---
Hello, ERROR: Field or method 'nonExistent' not found in com.greencreeper.zest.ExampleUsage$User!
```
