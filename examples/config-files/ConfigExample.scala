package zest.examples.config

import cats.effect.IO
import zest.core.api.Template
import zest.core.errors.TemplateError

object ConfigExample extends App {
  // Define configuration structures
  case class DatabaseConfig(
    host: String,
    port: Int,
    name: String,
    ssl: Boolean,
    maxConnections: Int
  )

  case class ServerConfig(
    host: String,
    port: Int,
    sslEnabled: Boolean,
    timeout: Int
  )

  case class CacheConfig(
    enabled: Boolean,
    ttl: Int,
    maxSize: Int
  )

  case class LoggingConfig(
    level: String,
    format: String,
    file: Option[String]
  )

  case class AppConfig(
    serviceName: String,
    version: String,
    environment: String,
    database: DatabaseConfig,
    server: ServerConfig,
    cache: CacheConfig,
    logging: LoggingConfig
  )

  val applicationConfigTemplate = """# {{ serviceName }} Configuration
# Generated on {{ timestamp }}
# Environment: {{ environment }}

[application]
name = "{{ serviceName }}"
version = "{{ version }}"
environment = "{{ environment }}"

[database]
host = "{{ database.host }}"
port = {{ database.port }}
name = "{{ database.name }}"
ssl_enabled = {{ database.ssl }}
max_connections = {{ database.maxConnections }}

[server]
host = "{{ server.host }}"
port = {{ server.port }}
ssl_enabled = {{ server.sslEnabled }}
timeout = {{ server.timeout }}

{% if cache.enabled %}
[cache]
enabled = true
ttl = {{ cache.ttl }}
max_size = {{ cache.maxSize }}
{% else %}
[cache]
enabled = false
{% endif %}

[logging]
level = "{{ logging.level }}"
format = "{{ logging.format }}"
{% if logging.file.nonEmpty %}
file = "{{ logging.file.get }}"
{% endif %}

{% if environment == "production" %}
[security]
mode = "strict"
cors_enabled = true
rate_limiting = true
{% else %}
[security]
mode = "permissive"
cors_enabled = false
rate_limiting = false
{% endif %}

[features]
metrics = true
tracing = {{ environment != "development" }}
debug = {{ environment == "development" }}
"""

  val dockerComposeTemplate = """version: '3.8'

services:
  {{ app.serviceName }}:
    image: {{ app.serviceName }}:{{ app.version }}
    ports:
      - "{{ app.server.port }}:{{ app.server.port }}"
    environment:
      - NODE_ENV={{ environment }}
      - DB_HOST={{ app.database.host }}
      - DB_PORT={{ app.database.port }}
      - DB_NAME={{ app.database.name }}
      - DB_SSL={{ app.database.ssl }}
      - LOG_LEVEL={{ app.logging.level }}
    volumes:
      - ./logs:/app/logs
    networks:
      - app-network

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    networks:
      - app-network

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: {{ app.database.name }}
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - app-network

volumes:
  postgres_data:

networks:
  app-network:
    driver: bridge
"""

  def run(): IO[Unit] = {
    IO.println("=== Zest Configuration File Generation Examples ===\n")

    for {
      _ <- IO.println("1. Application Configuration:")

      configTemplateResult <- IO.fromEither(Template(applicationConfigTemplate).build[AppConfig]())

      appConfig = AppConfig(
        serviceName = "web-api",
        version = "2.0.0",
        environment = "production",
        database = DatabaseConfig(
          host = "db.example.com",
          port = 5432,
          name = "webapi_prod",
          ssl = true,
          maxConnections = 100
        ),
        server = ServerConfig(
          host = "0.0.0.0",
          port = 8080,
          sslEnabled = true,
          timeout = 30000
        ),
        cache = CacheConfig(
          enabled = true,
          ttl = 300,
          maxSize = 1000
        ),
        logging = LoggingConfig(
          level = "info",
          format = "json",
          file = Some("/app/logs/app.log")
        )
      )

      configRenderResult <- configTemplateResult.render(appConfig)

      _ <- configRenderResult match {
        case Right(configText) => IO.println(configText)
        case Left(error: TemplateError) =>
          IO.println(s"Error: ${TemplateError.formatError(error)}")
      }

      _ <- IO.println("\n2. Docker Compose Configuration:")

      dockerTemplateResult <- IO.fromEither(Template(dockerComposeTemplate).build[AppConfig]())
      dockerRenderResult <- dockerTemplateResult.render(appConfig)

      _ <- dockerRenderResult match {
        case Right(dockerCompose) => IO.println(dockerCompose)
        case Left(error: TemplateError) =>
          IO.println(s"Error: ${TemplateError.formatError(error)}")
      }

      _ <- IO.println("\n3. Development Environment Configuration:")

      devConfig = appConfig.copy(
        environment = "development",
        server = appConfig.server.copy(port = 3000),
        database = appConfig.database.copy(host = "localhost"),
        cache = appConfig.cache.copy(enabled = false),
        logging = appConfig.logging.copy(level = "debug", file = None)
      )

      devRenderResult <- configTemplateResult.render(devConfig)

      _ <- devRenderResult match {
        case Right(configText) => IO.println(configText)
        case Left(error: TemplateError) =>
          IO.println(s"Error: ${TemplateError.formatError(error)}")
      }

      _ <- IO.println("\n=== Examples Complete ===")
    } yield ()
  }

  // Run the example
  run().unsafeRunSync()
}
