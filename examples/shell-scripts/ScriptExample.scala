package zest.examples.shell

import cats.effect.IO
import zest.core.api.Template
import zest.core.errors.TemplateError

object ScriptExample extends App {
  // Define configuration for shell script generation
  case class ScriptConfig(
    appName: String,
    version: String,
    environment: String,
    workingDir: String,
    entryPoint: String,
    dependencies: List[String],
    features: List[String]
  )

  val bashTemplate = """#!/bin/bash
# Generated deployment script for {{ appName }}
# Version: {{ version }}
# Environment: {{ environment }}

set -e

echo "==================================="
echo "Deploying {{ appName }} v{{ version }}"
echo "Environment: {{ environment }}"
echo "==================================="

# Configuration
APP_NAME="{{ appName }}"
WORKING_DIR="{{ workingDir }}"
ENTRY_POINT="{{ entryPoint }}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING:${NC} $1"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1"
    exit 1
}

log "Starting deployment process..."

# Check if working directory exists
if [ ! -d "$WORKING_DIR" ]; then
    warn "Working directory $WORKING_DIR does not exist. Creating..."
    mkdir -p "$WORKING_DIR"
fi

cd "$WORKING_DIR"

log "Installing dependencies..."
{% for dep in dependencies %}
npm install {{ dep }}
{% endfor %}

log "Building application..."
npm run build

log "Starting {{ appName }}..."
if [ "{{ environment }}" = "production" ]; then
    NODE_ENV=production pm2 start $ENTRY_POINT --name $APP_NAME
else
    NODE_ENV=development node $ENTRY_POINT
fi

log "{{ appName }} deployed successfully!"

{% if features contains "monitoring" %}
log "Setting up monitoring..."
# Add monitoring setup here
{% endif %}

{% if features contains "backup" %}
log "Setting up backup..."
# Add backup setup here
{% endif %}

echo "==================================="
echo "Deployment completed!"
echo "==================================="
"""

  def run(): IO[Unit] = {
    IO.println("=== Zest Shell Script Generation Examples ===\n")

    for {
      _ <- IO.println("1. Web Application Deployment Script:")

      templateResult <- IO.fromEither(Template(bashTemplate).build[ScriptConfig]())

      config = ScriptConfig(
        appName = "MyWebApp",
        version = "2.1.0",
        environment = "production",
        workingDir = "/var/www/myapp",
        entryPoint = "server.js",
        dependencies = List("express", "cors", "helmet", "dotenv"),
        features = List("monitoring", "backup")
      )

      renderResult <- templateResult.render(config)

      _ <- renderResult match {
        case Right(script) => IO.println(script)
        case Left(error: TemplateError) =>
          IO.println(s"Error: ${TemplateError.formatError(error)}")
      }

      _ <- IO.println("\n2. Development Setup Script:")

      devConfig = ScriptConfig(
        appName = "DevApp",
        version = "1.0.0",
        environment = "development",
        workingDir = "/home/dev/projects/myapp",
        entryPoint = "index.js",
        dependencies = List("nodemon", "eslint"),
        features = List("monitoring")
      )

      devRenderResult <- templateResult.render(devConfig)

      _ <- devRenderResult match {
        case Right(script) => IO.println(script)
        case Left(error: TemplateError) =>
          IO.println(s"Error: ${TemplateError.formatError(error)}")
      }

      _ <- IO.println("\n=== Examples Complete ===")
    } yield ()
  }

  // Run the example
  run().unsafeRunSync()
}
