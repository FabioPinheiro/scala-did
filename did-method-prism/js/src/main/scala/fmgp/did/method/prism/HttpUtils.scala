package fmgp.did.method.prism

import zio._
import zio.json._
import fmgp.did._

import scala.util.chaining.scalaUtilChainingOps
import scala.scalajs.js
import org.scalajs.dom._
import fmgp.typings.std.stdStrings.resume

object HttpUtils {
  def make: UIO[HttpUtils] = ZIO.succeed(HttpUtils())
  def layer: ULayer[HttpUtils] = ZLayer.fromZIO(make)
}

case class HttpUtils() {
  def getT[T](url: String)(using decoder: JsonDecoder[T], classTag: reflect.ClassTag[T]): Task[T] =
    for {
      response <- ZIO.fromPromiseJS(fetch(url, new RequestInit { method = HttpMethod.GET }))
      data <- ZIO.fromPromiseJS(response.text())
      ret <- data.fromJson[T] match
        case Left(fail) =>
          val aux = s"Fail to parse: $fail"
          ZIO.logWarning(aux) *> ZIO.fail(new RuntimeException(aux))
        case Right(doc) => ZIO.succeed(doc)
    } yield (ret)

  def getSeqT[T](url: String)(using decoder: JsonDecoder[T], classTag: reflect.ClassTag[T]): Task[Seq[T]] =
    for {
      response <- ZIO.fromPromiseJS(fetch(url, new RequestInit { method = HttpMethod.GET }))
      ret <- response.status match
        case 404 =>
          ZIO.log(s"Events Not Found for DID in $url") *> ZIO.succeed(Seq.empty)
        case c if c >= 200 & c < 300 =>
          for {
            rawData <- ZIO.fromPromiseJS(response.text())
            rawDataSplitByLines = rawData.split("\n").toSeq.map(_.fromJson[T])
            seqOfTOrError =
              rawDataSplitByLines.foldLeft(Right(Seq.empty): Either[String, Seq[T]])((acc, ele) =>
                acc match
                  case Left(errors) => Left(errors)
                  case Right(seq)   =>
                    ele match
                      case Left(error)  => Left(error)
                      case Right(event) => Right(seq :+ event)
              )
            seqOfT <- seqOfTOrError match
              case Left(fail) =>
                val aux = s"Fail to parse: $fail"
                ZIO.logWarning(aux) *> ZIO.fail(new RuntimeException(aux))
              case Right(seqEvents) =>
                ZIO.succeed(seqEvents)

          } yield seqOfT
        case anotherCode =>
          val aux = s"Fail on HTTP request with code $anotherCode"
          ZIO.logError(aux) *> ZIO.fail(new RuntimeException(aux))
    } yield ret
}
