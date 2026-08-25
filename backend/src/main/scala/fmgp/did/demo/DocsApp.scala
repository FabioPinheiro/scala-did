package fmgp.did.demo

import zio.http.*

/** Redirects routes which were formerly served from documentation resources embedded in the demo jar.
  *
  * The documentation is now owned and deployed by scala-did. `/apis` is intentionally deprecated: the old
  * generated-resource layout no longer has a public equivalent.
  */
object DocsApp {
  private val documentationURL = URL.decode("https://doc.did.fmgp.app") match
    case Right(url)  => url
    case Left(error) => throw error

  private def redirect(request: Request): Response =
    Response.redirect(
      documentationURL
        .addPath(request.url.path)
        .addQueryParams(request.url.queryParams)
    )

  val routes = Routes(
    Method.GET / "doc" -> handler { (request: Request) => redirect(request) },
    Method.GET / "doc" / trailing -> handler {
      Handler.param[(Path, Request)](_._2).map(redirect)
    },
    Method.GET / "api" -> handler { (request: Request) => redirect(request) },
    Method.GET / "api" / trailing -> handler {
      Handler.param[(Path, Request)](_._2).map(redirect)
    },
    Method.GET / "apis" -> handler(Response.status(Status.Gone)),
    Method.GET / "apis" / trailing -> handler(Response.status(Status.Gone)),
  ).sandbox
}
