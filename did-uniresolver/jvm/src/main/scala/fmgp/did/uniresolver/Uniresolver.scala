package fmgp.did.uniresolver

import zio._
import zio.json._
import zio.http._
import zio.http.Client

import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._
import io.netty.bootstrap.ChannelFactory

object Uniresolver {
  val defaultEndpoint = "https://dev.uniresolver.io/1.0/identifiers/"
  val layer: ZLayer[Client & Scope, Throwable, Uniresolver] =
    ZLayer.fromZIO(
      for {
        client <- ZIO.service[Client]
        scope <- ZIO.service[Scope]
      } yield Uniresolver("https://dev.uniresolver.io/1.0/identifiers/", client, scope)
    )
}

case class Uniresolver(uniresolverServer: String, client: Client, scope: Scope) extends Resolver {

  override protected def didDocumentOf(did: FROMTO): IO[DidFail, DIDDocument] = {
    // if (!methods.contains(did.toDID.namespace)) ZIO.fail(DidMethodNotSupported(did.toDID.namespace))
    // else

    val program = for {
      res <- Client
        .request(Request.get(path = uniresolverServer + did))
        .provide(Client.default, Scope.default)
        .mapError { case _: Throwable => SomeThrowable(did.toDID.namespace) }
      data <- res.body.asString
        .mapError { case ex: Throwable => FailToParse(ex.getMessage()) }
      didResolutionResult <- data.fromJson[DIDResolutionResult] match
        case Left(error)  => ZIO.fail(FailToParse(error))
        case Right(value) => ZIO.succeed(value.didDocument)
    } yield (didResolutionResult)

    program
  }

  // val methods = Seq(
  //   "3",
  //   "ace",
  //   "ala",
  //   "algo",
  //   "bba",
  //   "bid",
  //   "btcr",
  //   "ccp",
  //   "cheqd",
  //   "com",
  //   "dns",
  //   "dock",
  //   "dyne",
  //   "ebsi",
  //   "elem",
  //   "emtrust",
  //   "ens",
  //   "eosio",
  //   "ethr",
  //   "ev",
  //   "evan",
  //   "everscale",
  //   "factom",
  //   "gatc",
  //   "github",
  //   "hcr",
  //   "icon",
  //   "iid",
  //   "indy",
  //   "io",
  //   "ion",
  //   "iscc",
  //   "jolo",
  //   "jwk",
  //   "key",
  //   "kilt",
  //   "kscirc",
  //   "lit",
  //   "meta",
  //   "moncon",
  //   "mydata",
  //   "nacl",
  //   "ont",
  //   "orb",
  //   "oyd",
  //   "pkh",
  //   "schema",
  //   "sol",
  //   "sov",
  //   "stack",
  //   "tz",
  //   "unisot",
  //   "v1",
  //   "vaa",
  //   "web"
  // )
}
