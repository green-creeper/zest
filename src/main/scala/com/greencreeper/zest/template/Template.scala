package com.greencreeper.zest.template

import cats.implicits.*
import cats.effect.Sync
import java.io.File
import scala.reflect.ClassTag
import cats.parse.{Parser, Parser0}
import cats.parse.Parser.*
import cats.parse.Parser0.*
import cats.parse.Accumulator0.*

trait Template[F[_]] {
  def render(context: Any): F[String]
}

object Template {
  def fromFile[F[_]](filePath: String)(using Sync[F]): F[Template[F]] =
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

  // rawText MUST be a Parser[RawText], meaning it consumes AT LEAST ONE character.
  // This will prevent it from producing RawText("").
  private val rawText: Parser[RawText] = {
    Parser.until(Parser.string("{{"))
      .string
      .map(RawText.apply)
      .orElse(Parser.anyChar.rep(1).string.map(RawText.apply)) // If no '{{', consume remaining as raw text
      .backtrack
  }

  // Now templatePart can be a Parser[TemplatePart] because both branches are Parsers
  private val templatePart: Parser[TemplatePart] =
    placeholder.map(identity[TemplatePart]).orElse(rawText.map(identity[TemplatePart]))

  // The main template parser, which produces a list of template parts
  val template: Parser0[List[TemplatePart]] =
    Parser.repAs0(templatePart)(listAccumulator0).map(_.toList).map(_.filterNot(_ == RawText("")))
} // Closing brace for object TemplateParser

class FileTemplate[F[_]](filePath: String)(using Sync[F]) extends Template[F] {
  private val F_instance = summon[Sync[F]] // Use a local val for the Sync instance

  private val parsedTemplate: F[List[TemplatePart]] = F_instance.delay { // Use F_instance.delay
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
        case RawText(text) => F_instance.pure(text)
        case Placeholder(path) => evaluatePath(context, path)
      }.map(_.mkString)
    }

  private def evaluatePath(context: Any, path: String): F[String] = F_instance.defer {
    val parts = path.split('.').toList

    def go(current: Any, pathParts: List[String]): F[Any] =
      pathParts match {
        case Nil => F_instance.pure(current)
        case part :: tail =>
          val baseValue = current match {
            case Some(value) => value
            case _           => current
          }

          if (baseValue == null) {
            F_instance.raiseError(new NullPointerException(s"Null value encountered at part '$part' in path '$path'"))
          } else {
            val nextValueF = baseValue match {
              case map: Map[String, Any] @unchecked =>
                map.get(part) match {
                  case Some(value) => F_instance.pure(value)
                  case None => F_instance.raiseError(new NoSuchFieldException(s"Field '$part' not found in map for path '$path'"))
                }
              case _ =>
                F_instance.delay {
                  val clazz = baseValue.getClass
                  scala.util.Try(clazz.getMethod(part))
                    .map(_.invoke(baseValue))
                    .orElse(scala.util.Try(clazz.getDeclaredField(part)).map { field =>
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

}
