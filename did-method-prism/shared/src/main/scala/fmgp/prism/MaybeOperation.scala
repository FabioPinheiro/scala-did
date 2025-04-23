package fmgp.prism

import zio._
import zio.json._

import fmgp.crypto.SHA256

import proto.prism.PrismOperation
import proto.prism.PrismObject
import proto.prism.SignedPrismOperation
import proto.prism.CreateDIDOperation.DIDCreationData
import fmgp.util.safeValueOf
import fmgp.util.bytes2Hex

// TODO Rename to MaybeEvent
sealed trait MaybeOperation[+T <: OP] // sealed abstract class MaybeOperation[+T <: OP]

case class InvalidPrismObject(
    tx: String,
    b: Int,
    reason: String,
) extends MaybeOperation[Nothing]
    with PrismBlockIndex

case class InvalidSignedPrismOperation(
    tx: String,
    b: Int,
    o: Int,
    reason: String,
) extends MaybeOperation[Nothing]
    with PrismOperationIndex

case class MySignedPrismOperation[+T <: OP](
    tx: String,
    b: Int,
    o: Int,
    signedWith: String,
    signature: Array[Byte],
    operation: T,
    protobuf: PrismOperation
) extends MaybeOperation[T]
    with PrismOperationIndex {
  def opHash = SHA256.digestToHex(protobuf.toByteArray)
  def opId = OpId(b = b, o = o, opHash = opHash)
}

object InvalidPrismObject {
  given decoder: JsonDecoder[InvalidPrismObject] = DeriveJsonDecoder.gen[InvalidPrismObject]
  given encoder: JsonEncoder[InvalidPrismObject] = DeriveJsonEncoder.gen[InvalidPrismObject]
}
object InvalidSignedPrismOperation {
  given decoder: JsonDecoder[InvalidSignedPrismOperation] = DeriveJsonDecoder.gen[InvalidSignedPrismOperation]
  given encoder: JsonEncoder[InvalidSignedPrismOperation] = DeriveJsonEncoder.gen[InvalidSignedPrismOperation]
}

object MySignedPrismOperation {

  given decoder: JsonDecoder[MySignedPrismOperation[OP]] = {
    import fmgp.util.decoderByteArray
    given decoderPrismOperation: JsonDecoder[PrismOperation] = // use mapOrFail
      decoderByteArray.map(e => PrismOperation.parseFrom(e))
    DeriveJsonDecoder.gen[MySignedPrismOperation[OP]]
  }
  given encoder: JsonEncoder[MySignedPrismOperation[OP]] = {
    import fmgp.util.encoderByteArray
    given encoderPrismOperation: JsonEncoder[PrismOperation] =
      encoderByteArray.contramap((e: PrismOperation) => e.toByteArray)
    DeriveJsonEncoder.gen[MySignedPrismOperation[OP]]
  }

}

object MaybeOperation {

  given decoder: JsonDecoder[MaybeOperation[OP]] =
    MySignedPrismOperation.decoder
      .widen[MaybeOperation[OP]]
      .orElseAndWarpErrors(InvalidSignedPrismOperation.decoder.widen[MaybeOperation[OP]])
      .orElseAndWarpErrors(InvalidPrismObject.decoder.widen[MaybeOperation[OP]])

  given encoder: JsonEncoder[MaybeOperation[OP]] = new JsonEncoder[MaybeOperation[OP]] {
    override def unsafeEncode(b: MaybeOperation[OP], indent: Option[Int], out: zio.json.internal.Write): Unit = {
      b match {
        case obj: MySignedPrismOperation[OP]  => MySignedPrismOperation.encoder.unsafeEncode(obj, indent, out)
        case obj: InvalidSignedPrismOperation => InvalidSignedPrismOperation.encoder.unsafeEncode(obj, indent, out)
        case obj: InvalidPrismObject          => InvalidPrismObject.encoder.unsafeEncode(obj, indent, out)
      }
    }
  }

  // given JsonDecoder[MaybeOperation[OP]] = DeriveJsonDecoder.gen[MaybeOperation[OP]]
  // given JsonEncoder[MaybeOperation[OP]] = DeriveJsonEncoder.gen[MaybeOperation[OP]]

  def fromProto(
      tx: String,
      blockIndex: Int,
      prismObject: PrismObject,
  ): Seq[MaybeOperation[OP]] =
    prismObject.blockContent match
      case None => Seq(InvalidPrismObject(tx = tx, b = blockIndex, reason = "blockContent is missing"))
      case Some(prismBlock) =>
        prismBlock.operations.zipWithIndex.map {
          case (SignedPrismOperation(signedWith, signature, operation, unknownFields), opIndex) =>
            assert(unknownFields.serializedSize == 0, "SignedPrismOperation have unknownFields")
            operation match
              case None => InvalidSignedPrismOperation(tx = tx, b = blockIndex, o = opIndex, "operation is missing")
              case Some(prismOperation) =>
                import proto.prism.PrismOperation.Operation._
                val op = prismOperation.operation match {
                  case Empty                    => VoidOP("PRISM Operation is missing")
                  case CreateDid(p)             => CreateDidOP.fromProto(p)
                  case UpdateDid(p)             => UpdateDidOP.fromProto(p)
                  case IssueCredentialBatch(p)  => VoidOP("TODO IssueCredential") // IssueCredentialBatchOP.fromProto(p)
                  case RevokeCredentials(p)     => VoidOP("TODO RevokeCredentials") // RevokeCredentialsOP.fromProto(p)
                  case ProtocolVersionUpdate(p) => VoidOP("TODO") // ProtocolVersionUpdateOP.fromProto(p)
                  case DeactivateDid(p)         => DeactivateDidOP.fromProto(p)
                  case CreateStorageEntry(p)    => VoidOP("TODO") // CreateStorageEntryOP.fromProto(p)
                  case UpdateStorageEntry(p)    => VoidOP("TODO") // UpdateStorageEntryOP.fromProto(p)
                }
                MySignedPrismOperation(
                  tx = tx,
                  b = blockIndex,
                  o = opIndex,
                  signedWith = signedWith,
                  signature = signature.toByteArray(),
                  operation = op,
                  protobuf = prismOperation,
                )
        }

}
