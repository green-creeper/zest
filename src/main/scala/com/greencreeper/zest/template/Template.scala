package com.greencreeper.zest.template

import cats.implicits._
import cats.effect.Sync
import java.io.File
import scala.reflect.ClassTag
import cats.parse.{Parser, Parser0}
import cats.parse.Parser._
import cats.parse.Parser0._

trait Template[F[_]] {
  def render(context: Any): F[String]
}

object Template {
  def fromFile[F[_]: Sync](filePath: String): F[Template[F]] =
    Sync[F].delay {
      new FileTemplate[F](filePath)
    }
}

// Define the ADT for parsed template parts
sealed trait TemplatePart
final case class RawText(text: String) extends TemplatePart
final case class Placeholder(path: String) extends TemplatePart

// Define the parser using cats-parse
object TemplateParser {
  // Parser for an identifier part (alphanumeric and underscore)
  private val identifierPart = Parser.charsWhile(c => c.isLetterOrDigit || c == '_')

  // Parser for a path (e.g., "user.name.address")
  private val pathParser = identifierPart.repSep(1, Parser.char('.')).map(_.toList.mkString("."))

  // Parser for optional whitespace
  private val whitespace: Parser0[Unit] = Parser.charsWhile0(_.isWhitespace).void

  // Parser for a placeholder, e.g., {{ user.name }}
  val placeholder: Parser[Placeholder] = {
    val open = Parser.string("{{")
    val close = Parser.string("}}")
    (open.void *> whitespace *> pathParser <* whitespace <* close.void).map(Placeholder.apply)
  }

  // rawText should be Parser0[RawText] (can parse empty string)
  private val rawText: Parser0[RawText] = {
    Parser.repUntil0(Parser.anyChar, Parser.string("{{").peek)
      .string
      .map(RawText.apply)
  }

  // templatePart must be Parser0[TemplatePart] so its rep0 method can be called
  private val templatePart: Parser0[TemplatePart] =
    // Lift placeholder to Parser0[TemplatePart] then orElse with rawText
    placeholder.map(identity[TemplatePart]).orElse(rawText.map(identity[TemplatePart]))

  // The main template parser, which produces a list of template parts
  // Use templatePart.rep0 directly. This requires templatePart to be Parser0.
  val template: Parser0[List[TemplatePart]] = {
    // This will parse one or more templateParts
    val oneOrMoreParts: Parser[List[TemplatePart]] = Parser.rep(templatePart).map(_.toList)

    // Now make it parse zero or more
    oneOrMoreParts.orElse(Parser.pure(List.empty[TemplatePart]))
      .map(_.filterNot(_ == RawText(""))) // Filter out empty RawText objects
  }
} // Closing brace for object TemplateParser

class FileTemplate[F[_]: Sync](filePath: String) extends Template[F] {
  private val F = Sync[F]

  private val parsedTemplate: F[List[TemplatePart]] = F.delay {
    val source = scala.io.Source.fromFile(new File(filePath))
    val content = try source.mkString finally source.close()
    TemplateParser.template.parseAll(content) match {
      case Right(parts) => parts
      case Left(error)  => throw new IllegalArgumentException(s"Failed to parse template: $error")
    }
  }

  override def render(context: Any): F[String] =
    parsedTemplate.flatMap { parts =>
      parts.traverse {
        case RawText(text) => text.pure[F]
        case Placeholder(path) => evaluatePath(context, path)
      }.map(_.mkString)
    }

  private def evaluatePath(context: Any, path: String): F[String] = F.defer {
    val parts = path.split('.').toList

    def go(current: Any, pathParts: List[String]): F[Any] =
      pathParts match {
        case Nil => current.pure[F]
        case part :: tail =>
          val baseValue = current match {
            case Some(value) => value
            case _           => current
          }

          if (baseValue == null) {
            F.raiseError(new NullPointerException(s"Null value encountered at part '$part' in path '$path'"))
          } else {
            val nextValueF = baseValue match {
              case map: Map[String, Any] @unchecked =>
                map.get(part) match {
                  case Some(value) => value.pure[F]
                  case None => F.raiseError(new NoSuchFieldException(s"Field '$part' not found in map for path '$path'"))
                }
              case _ =>
                F.delay {
                  val clazz = baseValue.getClass
                  Try(clazz.getMethod(part))
                    .map(_.invoke(baseValue))
                    .orElse(Try(clazz.getDeclaredField(part)).map { field =>
                      field.setAccessible(true)
                      field.get(baseValue)
                    })
                    .toEither
                    .leftMap(_ => new NoSuchFieldException(s"Field or method '$part' not found in ${clazz.getName}"))
                }.rethrow
            }
            nextValueF.flatMap(nextValue => go(nextValue, tail))
          }
      }

    go(context, parts).map(result => Option(result).map(_.toString).getOrElse("null"))
  }

  private def Try[A](block: => A): scala.util.Try[A] = scala.util.Try(block)
}
