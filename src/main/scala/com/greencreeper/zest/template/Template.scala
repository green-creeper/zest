package com.greencreeper.zest.template

import cats.implicits._
import cats.effect.Sync
import java.io.File
import scala.util.matching.Regex
import scala.reflect.ClassTag

trait Template[F[_]] {
  def render(context: Any): F[String]
}

object Template {
  def fromFile[F[_]: Sync](filePath: String): F[Template[F]] =
    Sync[F].delay {
      new FileTemplate[F](filePath)
    }

  val placeholderRegex: Regex = "\\{\\{([a-zA-Z0-9_\\.]+)\\}\\}".r
}

class FileTemplate[F[_]: Sync](filePath: String) extends Template[F] {
  private val F = Sync[F]

  private val templateContent: F[String] = F.delay {
    val source = scala.io.Source.fromFile(new File(filePath))
    try source.mkString
    finally source.close()
  }

  override def render(context: Any): F[String] =
    templateContent.flatMap { content =>
      val matches = Template.placeholderRegex.findAllMatchIn(content).toList
      val replacements = matches.traverse { m =>
        val path = m.group(1)
        evaluatePath(context, path).map(replacement => (m.matched, replacement))
      }

      replacements.map { reps =>
        reps.foldLeft(content) { case (current, (placeholder, value)) =>
          current.replace(placeholder, value)
        }
      }
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
