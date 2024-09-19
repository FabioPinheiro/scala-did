package fmgp.did.uniresolver

import zio._
import zio.json._
import zio.http._

import fmgp.did._
import fmgp.did.comm._
import fmgp.crypto.error._
import io.netty.bootstrap.ChannelFactory

object Uniresolver {
  val defaultEndpoint = "https://dev.uniresolver.io/1.0/identifiers/"
  def make(url: String = defaultEndpoint): ZIO[Client & Scope, Nothing, Uniresolver] =
    for {
      client <- ZIO.service[Client]
      scope <- ZIO.service[Scope]
    } yield Uniresolver(url, client, scope)
  def layer(url: String = defaultEndpoint): ZLayer[Client & Scope, Nothing, Resolver] =
    ZLayer.fromZIO(make(url))
}

case class Uniresolver(uniresolverServer: String, client: Client, scope: Scope) extends Resolver {

  override protected def didDocumentOf(did: FROMTO): IO[ResolverError, DIDDocument] = {
    // if (!methods.contains(did.toDID.namespace)) ZIO.fail(DidMethodNotSupported(did.toDID.namespace))
    // else
    for {
      res <- Client
        .batched(Request.get(path = uniresolverServer + did))
        .provideEnvironment(ZEnvironment(client) ++ ZEnvironment(scope))
        .mapError(ex => DIDresolutionFail.fromThrowable(ex))
      data <- res.body.asString
        .mapError(ex => DIDresolutionFail.fromThrowable(ex))
      didResolutionResult <- data.fromJson[DIDResolutionResult] match
        case Left(error)  => ZIO.fail(DIDresolutionFail.fromParseError("DIDResolutionResult", error))
        case Right(value) => ZIO.succeed(value.didDocument)
    } yield (didResolutionResult)
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
