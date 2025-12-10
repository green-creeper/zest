package zest.api

import cats.MonadError
import fs2.io.file.Path
import zest.core.ast.Node
import zest.core.context.Context
import zest.core.renderer.Renderer

trait Template[F[_]]:
  def render(context: Context): F[String]
  def renderToFile(context: Context, output: Path): F[Unit]

private[api] class TemplateImpl[F[_]](
  private val nodes: List[Node]
)(using me: MonadError[F, Throwable]) extends Template[F]:

  private val renderer = Renderer[F]

  def render(context: Context): F[String] =
    renderer.render(nodes, context)

  def renderToFile(context: Context, output: Path): F[Unit] = ??? // to be implemented in Phase 3
