package fmgp.did.method.prism

import zio._
import zio.http._
import zio.json._
import fmgp.did._

object HttpUtils {

  def make: ZIO[Client & Scope, Nothing, HttpUtils] =
    for {
      client <- ZIO.service[Client]
      scope <- ZIO.service[Scope]
    } yield HttpUtils(client, scope)

  def layer: ZLayer[Client & Scope, Nothing, HttpUtils] =
    ZLayer.fromZIO(make)
}

case class HttpUtils(client: Client, scope: Scope) {
  def getT[T](url: String)(using decoder: JsonDecoder[T], classTag: reflect.ClassTag[T]): Task[T] =
    for {
      res <- Client
        .batched(Request.get(path = url))
        .provideEnvironment(ZEnvironment(client, scope))
      // .mapError(ex => DIDresolutionFail.fromThrowable(ex))
      _ = res
      data <- res.body.asString
      // .mapError(ex => DIDresolutionFail.fromThrowable(ex))
      didDoc <- data.fromJson[T] match
        // case Left(error) => ZIO.fail(DIDresolutionFail.fromParseError(classTag.runtimeClass.getName(), error))
        case Left(fail) =>
          val aux = s"Fail to parse: $fail"
          ZIO.logWarning(aux) *> ZIO.fail(new RuntimeException(aux))
        case Right(doc) => ZIO.succeed(doc)
    } yield (didDoc)

  def getSeqT[T](url: String)(using decoder: JsonDecoder[T], classTag: reflect.ClassTag[T]): Task[Seq[T]] =
    for {
      res <- Client
        .batched(Request.get(path = url))
        .provideEnvironment(ZEnvironment(client, scope))
      ret <- res match {
        case Response(Status.NotFound, headers, body) =>
          ZIO.log(s"Events Not Found for DID in $url") *> ZIO.succeed(Seq.empty)
        case Response(status, headers, body) =>
          body.asString
            .map(_.split("\n").toSeq.map(_.fromJson[T]))
            .map(
              _.foldLeft(Right(Seq.empty): Either[String, Seq[T]])((acc, ele) =>
                acc match
                  case Left(errors) => Left(errors)
                  case Right(seq) =>
                    ele match
                      case Left(error)  => Left(error)
                      case Right(event) => Right(seq :+ event)
              )
            )
            .flatMap {
              case Left(fail) =>
                val aux = s"Fail to parse: $fail"
                ZIO.logWarning(aux) *> ZIO.fail(new RuntimeException(aux))
              case Right(seqEvents) => ZIO.succeed(seqEvents)
            }
            .tapError(ex => ZIO.logWarning(s"Fail to read data from '$url': ${ex.toString}"))
      }
    } yield ret

}
