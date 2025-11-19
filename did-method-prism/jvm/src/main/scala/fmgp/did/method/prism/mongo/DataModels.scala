package fmgp.did.method.prism.mongo

import fmgp.did.DIDSubject
import fmgp.did.method.prism.EventHash
import fmgp.did.method.prism.cardano.EventCursor
import fmgp.did.method.prism.proto.MySignedPrismEvent
import fmgp.did.method.prism.proto.OP
import fmgp.util.bytes2Hex
import fmgp.util.hex2bytes
import proto.prism.PrismEvent
import reactivemongo.api.bson.*

import scala.util.Failure
import scala.util.Success
import scala.util.Try

type HASH = String
object DataModels {

  given BSONWriter[PrismEvent] with {
    override def writeTry(obj: PrismEvent): Try[BSONValue] =
      // Success(BSONBinary(obj.toByteArray, Subtype.GenericBinarySubtype))
      Success(BSONString(bytes2Hex(obj.toByteArray)))
  }

  given BSONReader[PrismEvent] with {
    override def readTry(bson: BSONValue): Try[PrismEvent] = {
      bson match
        // case b: BSONBinary => Success(PrismEvent.parseFrom(b.byteArray))
        case bsonString: BSONString => Success(PrismEvent.parseFrom(hex2bytes(bsonString.value)))
        case _                      => Failure(new RuntimeException("Wrong type for PrismEvent"))
    }
  }

  // given BSONWriter[EventHash] = BSONWriter.binaryWriter.beforeWrite(eh => eh.byteArray)
  // given BSONReader[EventHash] = BSONReader.binaryReader.afterRead(bytes => EventHash.fromBytes(bytes))
  given BSONWriter[EventHash] = BSONWriter.stringWriter.beforeWrite(eventHash => eventHash.hex)
  given BSONReader[EventHash] = BSONReader.stringReader.afterRead(str => EventHash.fromHex(str))

  given BSONDocumentWriter[MySignedPrismEvent[OP]] with {
    override def writeTry(obj: MySignedPrismEvent[OP]): Try[BSONDocument] =
      obj match
        case MySignedPrismEvent(tx, b, o, signedWith, signature, protobuf) =>
          Success(
            BSONDocument(
              "_id" -> obj.eventHash,
              "tx" -> tx,
              "b" -> b,
              "o" -> o,
              "signedWith" -> signedWith,
              "signature" -> bytes2Hex(signature),
              "protobuf" -> protobuf,
            )
          )
  }

  given BSONDocumentReader[MySignedPrismEvent[OP]] with { // = Macros.reader[MySignedPrismEvent[OP]]
    override def readDocument(doc: BSONDocument): Try[MySignedPrismEvent[OP]] = {
      for {
        tx <- doc.getAsTry[String]("tx")
        o <- doc.getAsTry[Int]("o")
        b <- doc.getAsTry[Int]("b")
        signedWith <- doc.getAsTry[String]("signedWith")
        signature <- doc.getAsTry[String]("signature").map(hex2bytes _)
        protobuf <- doc.getAsTry[PrismEvent]("protobuf")
      } yield MySignedPrismEvent[OP](tx, o, b, signedWith, signature, protobuf)
    }
  }

  given BSONDocumentWriter[EventWithRootRef] with {
    override def writeTry(obj: EventWithRootRef): Try[BSONDocument] =
      obj.event match
        case MySignedPrismEvent(tx, b, o, signedWith, signature, protobuf) =>
          Success(
            BSONDocument(
              "_id" -> obj.event.eventHash,
              "ref" -> obj.rootRef,
              "tx" -> tx,
              "b" -> b,
              "o" -> o,
              "signedWith" -> signedWith,
              "signature" -> bytes2Hex(signature),
              "protobuf" -> protobuf,
            )
          )
  }

  given BSONDocumentReader[EventWithRootRef] with { // = Macros.reader[MySignedPrismEvent[OP]]
    override def readDocument(doc: BSONDocument): Try[EventWithRootRef] = {
      for {
        ref <- doc.getAsTry[EventHash]("ref")
        tx <- doc.getAsTry[String]("tx")
        o <- doc.getAsTry[Int]("o")
        b <- doc.getAsTry[Int]("b")
        signedWith <- doc.getAsTry[String]("signedWith")
        signature <- doc.getAsTry[String]("signature").map(hex2bytes _)
        protobuf <- doc.getAsTry[PrismEvent]("protobuf")
      } yield EventWithRootRef(ref, MySignedPrismEvent[OP](tx, o, b, signedWith, signature, protobuf))
    }
  }

  // given BSONDocumentWriter[EventCursor] = Macros.writer[EventCursor]
  given BSONDocumentReader[EventCursor] = Macros.reader[EventCursor]

}
