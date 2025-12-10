package com.greencreeper.zest.template

import scala.io.Source
import java.io.File
import scala.util.matching.Regex
import scala.util.{Try, Success, Failure}
import java.util.regex.Matcher

trait Template {
  def render(context: Any): String
}

object Template {
  def fromFile(filePath: String): Template = {
    new FileTemplate(filePath)
  }
  val placeholderRegex: Regex = "\\{\\{([a-zA-Z0-9_\\.]+)\\}\\}".r
}

class FileTemplate(filePath: String) extends Template {
  private val templateContent: String =
    Source.fromFile(new File(filePath)).mkString

  override def render(context: Any): String = {
    Template.placeholderRegex.replaceAllIn(templateContent, m => {
      val path = m.group(1)
      val replacement = evaluatePath(context, path)
      Matcher.quoteReplacement(replacement)
    })
  }

  private def evaluatePath(context: Any, path: String): String = {
    val parts = path.split('.')
    var current: Any = context

    val result = Try {
      for (part <- parts) {
        if (current == null) {
          throw new NullPointerException("Null value encountered in path traversal")
        }

        current match {
          case map: Map[String, Any] @unchecked =>
            current = map.getOrElse(part, null)
          case _ =>
            val currentClass = current.getClass
            try {
              val method = currentClass.getMethod(part)
              current = method.invoke(current)
            } catch {
              case _: NoSuchMethodException =>
                try {
                  val field = currentClass.getDeclaredField(part)
                  field.setAccessible(true)
                  current = field.get(current)
                } catch {
                  case _: NoSuchFieldException =>
                    throw new IllegalArgumentException(s"Field or method '$part' not found in ${currentClass.getName}")
                  case e: Exception =>
                    throw new RuntimeException(s"Exception accessing field '$part': ${e.getMessage}", e)
                }
              case e: Exception =>
                throw new RuntimeException(s"Exception invoking method '$part': ${e.getMessage}", e)
            }
        }
      }
      Option(current).map(_.toString).getOrElse("null")
    }

    result match {
      case Success(value) => value
      case Failure(e) =>
        e match {
          case npe: NullPointerException => "ERROR: Null value encountered."
          case iae: IllegalArgumentException => s"ERROR: ${iae.getMessage}"
          case re: RuntimeException => s"ERROR: ${re.getMessage}"
          case _ => s"ERROR: An unexpected error occurred: ${e.getMessage}"
        }
    }
  }
}
