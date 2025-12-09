# Zest - Micro Templating Engine for Scala 3

A lightweight, type-safe templating engine for Scala 3 that embraces functional programming principles. Generate HTML, shell scripts, configuration files, and any text-based output with simple syntax and powerful features.

## Features

- **Type Safety**: Leverage Scala 3's type system for compile-time safety
- **Functional**: Pure functions, immutable data structures, effect tracking with cats-effect
- **Composable**: Small, reusable components that combine elegantly
- **Simple**: Minimal syntax, easy to learn and use
- **Extensible**: Plugin architecture for custom functions and filters
- **Resource-safe**: Proper resource management using cats-effect

## Installation

Add Zest to your `build.sbt`:

```scala
libraryDependencies += "com.block.zest" %% "zest-core" % "0.1.0"
```

## Quick Start

### Basic Usage

```scala
import cats.effect.IO
import zest.core.api.Template

// Simple string-based template
val template = "Hello {{ name }}!"
val result = Template(template)
  .build[Map[String, Any]]()
  .flatMap(_.render(Map("name" -> "World")))
  .unsafeRunSync()

println(result) // Right("Hello World!")
```

### Type-Safe Templates

```scala
case class User(name: String, email: String, age: Int)

val template = """
  <div>
    <h1>{{ user.name }}</h1>
    <p>Email: {{ user.email }}</p>
    <p>Age: {{ user.age }}</p>
  </div>
"""

val user = User("Alice", "alice@example.com", 30)

val result = Template(template)
  .build[User]()
  .flatMap(_.render(user))
  .unsafeRunSync()

println(result) // Right("<div>...<h1>Alice</h1>...</div>")
```

## Template Syntax

### Variable Interpolation

```scala
{{ variableName }}
{{ user.name }}
{{ items[0].title }}
{{ config.database.host }}
```

### Conditional Statements

```scala
{% if condition %}
  This shows if condition is true
{% endif %}

{% if user.admin %}
  Welcome, administrator!
{% else %}
  Welcome, user!
{% endif %}

{% if user.age > 18 %}
  Adult content
{% elif user.age > 13 %}
  Teen content
{% else %}
  Kids content
{% endif %}
```

### Comments

```scala
{# This is a comment #}
{# Multi-line
   comment #}
```

### Expressions

```scala
{{ name }}
{{ user.age + 1 }}
{{ price * 1.1 }}
{{ user.active && user.verified }}
{{ user.age > 18 }}
{{ user.name | upper }}
{{ text | length }}
```

## Examples

### HTML Generation

```scala
case class Page(title: String, user: User, items: List[String])

val htmlTemplate = """
<!DOCTYPE html>
<html>
<head>
    <title>{{ title }}</title>
</head>
<body>
    <h1>Welcome, {{ user.name }}!</h1>
    {% if user.admin %}
    <p>You are an administrator.</p>
    {% endif %}
    <ul>
    {% for item in items %}
        <li>{{ item }}</li>
    {% endfor %}
    </ul>
</body>
</html>
"""

val page = Page(
  title = "My Website",
  user = User("Alice", "alice@example.com", 30),
  items = List("Home", "About", "Contact")
)

val result = Template(htmlTemplate)
  .build[Page]()
  .flatMap(_.render(page))
  .unsafeRunSync()
```

### Shell Script Generation

```scala
case class ScriptConfig(
  appName: String,
  version: String,
  environment: String,
  dependencies: List[String]
)

val scriptTemplate = """#!/bin/bash
# Generated for {{ appName }} v{{ version }}

set -e

echo "Starting {{ appName }}..."

{% if environment == "production" %}
export NODE_ENV=production
{% else %}
export NODE_ENV=development
{% endif %}

cd /app

{% for dep in dependencies %}
npm install {{ dep }}
{% endfor %}

node index.js
"""

val config = ScriptConfig(
  appName = "MyApp",
  version = "1.0.0",
  environment = "production",
  dependencies = List("express", "cors")
)

val script = Template(scriptTemplate)
  .build[ScriptConfig]()
  .flatMap(_.render(config))
  .unsafeRunSync()
```

### Configuration Files

```scala
case class AppConfig(
  serviceName: String,
  database: DatabaseConfig,
  server: ServerConfig
)

case class DatabaseConfig(host: String, port: Int, name: String)
case class ServerConfig(host: String, port: Int)

val configTemplate = """# {{ serviceName }} Configuration

[database]
host = "{{ database.host }}"
port = {{ database.port }}
name = "{{ database.name }}"

[server]
host = "{{ server.host }}"
port = {{ server.port }}
"""

val appConfig = AppConfig(
  serviceName = "web-api",
  database = DatabaseConfig("localhost", 5432, "myapp"),
  server = ServerConfig("0.0.0.0", 8080)
)

val config = Template(configTemplate)
  .build[AppConfig]()
  .flatMap(_.render(appConfig))
  .unsafeRunSync()
```

## API Reference

### TemplateEngine

The main entry point for template operations:

```scala
import zest.core.api.TemplateEngine

val engine = TemplateEngine()

// Parse a template
val template = engine.parse("Hello {{ name }}!")

// Render with context
val result = engine.renderString("Hello {{ name }}!", Map("name" -> "World"))
```

### Template Builder

Type-safe template creation:

```scala
// String-based context
val template = Template("Hello {{ name }}!")
  .build()
  .flatMap(_.render(Map("name" -> "World")))

// Case class context (type-safe)
val template = Template("Hello {{ name }}!")
  .build[User]()
  .flatMap(_.render(user))
```

## Error Handling

Zest provides comprehensive error handling:

```scala
import zest.core.errors.TemplateError

val result = Template("Hello {{ missing }}!")
  .build[Map[String, Any]]()
  .flatMap(_.render(Map.empty))

result match {
  case Right(html) => println(html)
  case Left(TemplateError.RenderingError(message, template, location)) =>
    println(s"Error in $template at $location: $message")
}
```

Error types include:
- `ParseError`: Template syntax errors
- `EvaluationError`: Expression evaluation errors
- `RenderingError`: Template rendering errors
- `ContextError`: Context/data access errors
- `ValidationError`: Template validation errors

## Advanced Features

### Custom Functions

Extend Zest with custom functions:

```scala
// Built-in functions
{{ text | upper }}
{{ text | lower }}
{{ value | length }}
{{ now }}
{{ random }}
```

### Property Access

Access nested properties:

```scala
{{ user.profile.name }}
{{ config.database.host }}
{{ items[0].title }}
```

### Binary Operations

Use operators in templates:

```scala
{{ a + b }}
{{ price * 1.1 }}
{{ user.age > 18 }}
{{ a == b }}
{{ a && b }}
```

## Performance

Zest is designed for performance:

- Single-pass parsing
- Efficient string concatenation with StringBuilder
- Minimal allocations
- Type-safe context binding

Benchmarks show rendering times under 10ms for typical templates.

## Testing

Run the test suite:

```bash
sbt test
```

Zest includes comprehensive tests covering:
- Parser functionality
- Expression evaluation
- Template rendering
- Error handling
- Real-world scenarios

## Examples

See the `examples/` directory for complete examples:

- HTML generation
- Shell script generation
- Configuration file generation
- Email templates
- Performance benchmarks

## Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests for your changes
4. Ensure all tests pass
5. Submit a pull request

## License

Zest is licensed under the MIT License.

## API Documentation

For detailed API documentation, see the Scaladoc comments in the source code.

## Changelog

### 0.1.0 - Initial Release
- Basic template parsing and rendering
- Variable interpolation
- Conditional statements
- Type-safe context binding
- Error handling
- Examples and tests
