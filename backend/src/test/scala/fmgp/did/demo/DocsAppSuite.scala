package fmgp.did.demo

import munit.FunSuite
import zio.*
import zio.http.*

class DocsAppSuite extends FunSuite {
  private def response(path: String): Response =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe
        .run(
          ZIO.scoped(DocsApp.routes.runZIO(Request.get(URL.decode(path).toOption.get)))
        )
        .getOrThrowFiberFailure()
    }

  private def assertRedirect(path: String): Unit = {
    val result = response(path)
    assertEquals(result.status, Status.TemporaryRedirect)
    assertEquals(
      result.headers.get(Header.Location).map(_.url.encode),
      Some(s"https://doc.did.fmgp.app$path"),
    )
  }

  test("redirects documentation routes while preserving paths and query strings") {
    assertRedirect("/doc?version=0.1.1")
    assertRedirect("/doc/x?version=0.1.1")
    assertRedirect("/api?version=0.1.1")
    assertRedirect("/api/x?version=0.1.1")
  }

  test("retires the old /apis route") {
    assertEquals(response("/apis").status, Status.Gone)
    assertEquals(response("/apis/x").status, Status.Gone)
  }
}
