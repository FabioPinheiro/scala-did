package fmgp.did.method

import zio._
import zio.json._
import zio.json.ast.Json
import zio.http._
import fmgp.did.method.peer.DidPeerResolver
import fmgp.did.comm.FROMTO
import fmgp.crypto.error.DidFail
import fmgp.did.DIDDocument
import fmgp.did.uniresolver.DIDResolutionResult
import zio.http.MediaTypes

object DidPeerUniresolverDriver {

  val resolverPeer = Routes(
    Method.GET / "resolver" / "peer" / string("did") -> handler { (did: String, req: Request) =>
      for {
        resolver <- ZIO.service[DidPeerResolver]
        response <-
          ZIO
            .fromEither(FROMTO.either(did))
            .flatMap(resolver.didDocument(_))
            .map(didDocument =>
              DIDResolutionResult(
                didDocument = didDocument,
                didResolutionMetadata = Json.Obj(),
                didDocumentMetadata = Json.Obj(),
              )
            )
            .map(didResolutionResult =>
              Response(
                status = Status.Ok,
                headers = Headers(Header.ContentType(MediaType.application.json)),
                body = Body.fromCharSequence(didResolutionResult.toJsonPretty),
              )
            )
            .catchAll(didFail =>
              ZIO.succeed(
                Response(
                  status = Status.BadRequest,
                  headers = Headers.empty,
                  body = Body.fromCharSequence(didFail.toJsonPretty),
                )
              )
            )
      } yield (response)
    }
  ).toHttpApp
}
