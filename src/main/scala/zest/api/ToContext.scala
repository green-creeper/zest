package zest.api

import scala.deriving.Mirror
import zest.core.context.Context

trait ToContext[A]:
  def toContext(value: A): Context

object ToContext:
  // Derive instances for case classes using Scala 3 derivation
  // inline def derived[A](using Mirror.Of[A]): ToContext[A] = ???

  // Instances for common types
  given ToContext[Map[String, Any]] with
    def toContext(value: Map[String, Any]): Context = Context.fromMap(value)

  // given [A: ToContext]: ToContext[Option[A]] = ???
  // given [A: ToContext]: ToContext[List[A]] = ???
