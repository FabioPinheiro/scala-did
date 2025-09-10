package fmgp.did.method.prism.cardano

import munit._
import zio.json._

import fmgp.did.method.prism.PrismStateInMemory
import fmgp.did.method.prism.proto._
import PreprodExamples._

/** didResolverPrismJVM/testOnly fmgp.did.method.prism.cardano.PreprodModelsSuite */
class PreprodModelsSuite extends FunSuite {
  test("CardanoMetadata 416") {
    assert(metadata_416_cbor.toCardanoPrismEntry.isRight)
  }
  test("CardanoMetadata 417") {
    assert(metadata_417_cbor.toCardanoPrismEntry.isRight)
  }
  test("CardanoMetadata 418") {
    assert(metadata_419_cbor.toCardanoPrismEntry.isRight)
  }
  test("CardanoMetadata 17137") {
    assert(metadata_17137_cbor.toCardanoPrismEntry.isRight)
    metadata_17137_cbor.toCardanoPrismEntry match
      case Left(value) => fail("Must be CardanoPrismEntry")
      case Right(cardanoPrismEntry) =>
        MaybeEvent
          .fromProto(
            prismObject = cardanoPrismEntry.content,
            tx = cardanoPrismEntry.tx,
            blockIndex = cardanoPrismEntry.index,
          )
          .map {
            case InvalidPrismObject(tx, b, reason) => fail(s"Must be MySignedPrismEvent: fail with $reason")
            case InvalidSignedPrismEvent(tx, b, o, reason) =>
              fail(s"Must be MySignedPrismEvent: fail with $reason")
            case MySignedPrismEvent(tx, b, o, signedWith, signature, protobuf) => // ok
          }

  }

  test("WIP") {

    val seq = Seq(metadata_416_cbor, metadata_417_cbor, metadata_419_cbor).map(_.toCardanoPrismEntry.getOrElse(???))

    val aux1 = seq.map(cardanoPrismEntry =>
      MaybeEvent.fromProto(
        prismObject = cardanoPrismEntry.content,
        tx = cardanoPrismEntry.tx,
        blockIndex = cardanoPrismEntry.index,
      )
    )

    val aux2 = aux1.map(_.map(_.asInstanceOf[MySignedPrismEvent[OP]]).map { op =>
      // println(op.opHash)
      // println(op.toJsonPretty)
      op
    })

    aux2(0).head match
      case MySignedPrismEvent(tx, b, o, signedWith, signature, protobuf) =>

    val op0 = aux2(0).head.asInstanceOf[MySignedPrismEvent[CreateDidOP]]
    val op1 = aux2(1).head.asInstanceOf[MySignedPrismEvent[UpdateDidOP]]
    val op2 = aux2(2).head.asInstanceOf[MySignedPrismEvent[UpdateDidOP]]

    // println(s"CreateDidOP ${op0.opHash}")
    // println(s"UpdateDidOP ${op1.opHash} -- ${op1.event.previousEventHash}")
    // println(s"CreateDidOP ${op2.opHash} -- ${op2.event.previousEventHash}")

    val s0 = PrismStateInMemory.empty
    // println("State 0")
    val s1 = s0.addEvent(op0)
    // println("State 1")
    val s2 = s1.addEvent(op1)
    // println("State 2")
    val s3 = s2.addEvent(op2)
    // println("State 3")
    // println(s3.toJsonPretty)
  }
}
