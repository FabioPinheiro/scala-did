package fmgp.did

import zio.json._

sealed trait ResolverError
object ResolverError {
  given decoder: JsonDecoder[ResolverError] = DeriveJsonDecoder.gen[ResolverError]
  given encoder: JsonEncoder[ResolverError] = DeriveJsonEncoder.gen[ResolverError]
}

case class UnsupportedMethod(method: String) extends ResolverError
case class DIDresolutionFail(cause: String) extends ResolverError
object DIDresolutionFail {
  def fromThrowable(throwable: Throwable) =
    DIDresolutionFail(throwable.getClass.getName() + ":" + throwable.getMessage)
  def fromParseError(className: String, reason: String) =
    DIDresolutionFail(s"Fail to Parse $className due to: $reason")
}
