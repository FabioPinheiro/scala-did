package fmgp.did.method.prism.proto

import scalapb.GeneratedMessageCompanion
import scalapb.GeneratedMessage
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import com.google.protobuf.InvalidProtocolBufferException

// TODO Catch Exceptions from proto parseFrom
// PrismObject.parseFrom(protoBytes)
//SignedPrismEvent.parseFrom(

extension [A <: GeneratedMessage](proto: GeneratedMessageCompanion[A]) {

  // def parseFromOrFail(input: CodedInputStream): A

  // def parseFromOrFail(input: InputStream): A =

  def tryParseFrom(s: Array[Byte]): Either[String, A] =
    Try(proto.parseFrom(s))
      .map(Right(_))
      .recover { case ex: InvalidProtocolBufferException => Left(ex.getMessage()) }
      .get // Not the best
}
