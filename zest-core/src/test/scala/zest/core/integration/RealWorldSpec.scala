package zest.core.integration

import munit.*
import zest.core.api.*
import zest.core.context.*
import zest.core.errors.TemplateError

class RealWorldSpec extends FunSuite {

  test("HTML generation example") {
    val template = """<!DOCTYPE html>
<html>
<head>
    <title>{{ title }}</title>
</head>
<body>
    <h1>{{ title }}</h1>
    <p>Welcome, {{ user.name }}!</p>
    {% if user.admin %}
    <p>You are an administrator.</p>
    {% else %}
    <p>You are a regular user.</p>
    {% endif %}
    <p>Member since {{ user.joinedYear }}.</p>
</body>
</html>"""

    case class User(name: String, admin: Boolean, joinedYear: Int)
    case class Page(title: String, user: User)

    val user = User("Alice", true, 2023)
    val page = Page("My Website", user)

    val result = Template(template)
      .build[Page]()
      .flatMap(_.render(page))
      .unsafeRunSync()

    assert(result.isRight)
    result match {
      case Right(html) =>
        assert(html.contains("Welcome, Alice!"))
        assert(html.contains("You are an administrator."))
        assert(html.contains("Member since 2023."))
      case Left(error) => fail(s"Unexpected error: $error")
    }
  }

  test("Shell script generation") {
    val template = """#!/bin/bash
# Generated script for {{ appName }}
# Version: {{ version }}

set -e

echo "Starting {{ appName }}..."

{% if environment == "production" %}
echo "Running in PRODUCTION mode"
export NODE_ENV=production
{% else %}
echo "Running in DEVELOPMENT mode"
export NODE_ENV=development
{% endif %}

cd {{ workingDir }}

{% if dependencies.nonEmpty %}
echo "Installing dependencies..."
{% for dep in dependencies %}
npm install {{ dep }}
{% endfor %}
{% endif %}

echo "Starting application..."
node {{ entryPoint }}

echo "{{ appName }} started successfully!"
"""

    case class ScriptConfig(
      appName: String,
      version: String,
      environment: String,
      workingDir: String,
      entryPoint: String,
      dependencies: List[String]
    )

    val config = ScriptConfig(
      appName = "MyApp",
      version = "1.0.0",
      environment = "development",
      workingDir = "/app",
      entryPoint = "index.js",
      dependencies = List("express", "cors")
    )

    val result = Template(template)
      .build[ScriptConfig]()
      .flatMap(_.render(config))
      .unsafeRunSync()

    assert(result.isRight)
    result match {
      case Right(script) =>
        assert(script.contains("Running in DEVELOPMENT mode"))
        assert(script.contains("Installing dependencies..."))
        assert(script.contains("npm install express"))
        assert(script.contains("npm install cors"))
      case Left(error) => fail(s"Unexpected error: $error")
    }
  }

  test("Configuration file generation") {
    val template = """# {{ serviceName }} Configuration
# Generated on {{ timestamp }}

[database]
host = "{{ database.host }}"
port = {{ database.port }}
name = "{{ database.name }}"

[server]
host = "{{ server.host }}"
port = {{ server.port }}
ssl_enabled = {{ server.sslEnabled }}

{% if features.logging %}
[logging]
level = "{{ features.logging.level }}"
format = "{{ features.logging.format }}"

{% if features.metrics %}
[metrics]
enabled = {{ features.metrics.enabled }}
interval = {{ features.metrics.interval }}
{% endif %}
{% endif %}

[cache]
ttl = {{ cache.ttl }}
size = {{ cache.size }}

{% if environment == "production" %}
[security]
mode = "strict"
cors_enabled = true
{% else %}
[security]
mode = "permissive"
cors_enabled = false
{% endif %}
"""

    case class DatabaseConfig(host: String, port: Int, name: String)
    case class ServerConfig(host: String, port: Int, sslEnabled: Boolean)
    case class LoggingConfig(level: String, format: String)
    case class MetricsConfig(enabled: Boolean, interval: Int)
    case class FeaturesConfig(logging: Option[LoggingConfig], metrics: Option[MetricsConfig])
    case class CacheConfig(ttl: Int, size: Int)
    case class ServiceConfig(
      serviceName: String,
      timestamp: String,
      database: DatabaseConfig,
      server: ServerConfig,
      features: FeaturesConfig,
      cache: CacheConfig,
      environment: String
    )

    val config = ServiceConfig(
      serviceName = "web-api",
      timestamp = "2024-01-01",
      database = DatabaseConfig("localhost", 5432, "myapp"),
      server = ServerConfig("0.0.0.0", 8080, true),
      features = FeaturesConfig(
        logging = Some(LoggingConfig("info", "json")),
        metrics = Some(MetricsConfig(true, 60))
      ),
      cache = CacheConfig(300, 1000),
      environment = "production"
    )

    val result = Template(template)
      .build[ServiceConfig]()
      .flatMap(_.render(config))
      .unsafeRunSync()

    assert(result.isRight)
    result match {
      case Right(configText) =>
        assert(configText.contains("host = \"localhost\""))
        assert(configText.contains("port = 5432"))
        assert(configText.contains("level = \"info\""))
        assert(configText.contains("enabled = true"))
        assert(configText.contains("mode = \"strict\""))
      case Left(error) => fail(s"Unexpected error: $error")
    }
  }

  test("Email template generation") {
    val template = """Subject: Welcome to {{ siteName }}, {{ user.name }}!

Hi {{ user.name }},

Welcome to {{ siteName }}! We're excited to have you as a member.

{% if user.premium %}
You have access to premium features including:
{% for feature in premiumFeatures %}
- {{ feature }}
{% endfor %}
{% endif %}

Your account details:
- Username: {{ user.username }}
- Email: {{ user.email }}
- Member since: {{ user.joined }}

{% if user.hasUnreadMessages %}
You have {{ user.unreadCount }} unread messages in your inbox.
{% endif %}

Best regards,
The {{ siteName }} Team
"""

    case class User(
      name: String,
      username: String,
      email: String,
      joined: String,
      premium: Boolean,
      hasUnreadMessages: Boolean,
      unreadCount: Int
    )

    val user = User(
      name = "Alice Johnson",
      username = "alicej",
      email = "alice@example.com",
      joined = "2024-01-15",
      premium = true,
      hasUnreadMessages = true,
      unreadCount = 3
    )

    val result = Template(template)
      .build[User]()
      .flatMap(_.render(user))
      .unsafeRunSync()

    assert(result.isRight)
    result match {
      case Right(emailText) =>
        assert(emailText.contains("Welcome to"))
        assert(emailText.contains("Alice Johnson"))
        assert(emailText.contains("You have 3 unread messages"))
      case Left(error) => fail(s"Unexpected error: $error")
    }
  }

  test("Error handling in real scenario") {
    val template = "Hello {{ user.missingProperty }}!"
    val context = Map("user" -> Map("name" -> "Alice"))

    val result = Template(template)
      .build[Map[String, Any]]()
      .flatMap(_.render(context))
      .unsafeRunSync()

    assert(result.isLeft)
    result match {
      case Left(TemplateError.RenderingError(_, _, _)) => // Expected
      case _ => fail(s"Expected RenderingError, got $result")
    }
  }

  test("Performance test - multiple renders") {
    val template = "Hello {{ name }}, you have {{ count }} messages."
    val context = Map("name" -> "User", "count" -> 0)

    val startTime = System.currentTimeMillis()

    // Render 1000 times
    val results = (0 until 1000).map { i =>
      val ctx = context + ("count" -> i)
      Template(template)
        .build[Map[String, Any]]()
        .flatMap(_.render(ctx))
        .unsafeRunSync()
    }

    val endTime = System.currentTimeMillis()
    val duration = endTime - startTime

    // All should succeed
    assert(results.forall(_.isRight))

    // Should be reasonably fast (less than 5 seconds for 1000 renders)
    assert(duration < 5000, s"Rendering took too long: $duration ms")

    // Check a few results
    results.take(3).zipWithIndex.foreach { case (result, i) =>
      result match {
        case Right(text) =>
          assert(text.contains(s"you have $i messages."))
        case Left(error) => fail(s"Unexpected error for iteration $i: $error")
      }
    }
  }
}
