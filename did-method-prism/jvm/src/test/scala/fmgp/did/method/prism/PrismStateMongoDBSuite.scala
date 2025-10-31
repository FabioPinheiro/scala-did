package fmgp.did.method.prism

import munit.*
import zio.*
import zio.stream.*
import fmgp.did.method.prism.mongo.*
import fmgp.util.hex2bytes
import _root_.proto.prism.SignedPrismEvent
import fmgp.did.method.prism.proto.MaybeEvent
import fmgp.did.method.prism.proto.InvalidPrismObject
import fmgp.did.method.prism.proto.InvalidSignedPrismEvent
import fmgp.did.method.prism.proto.MySignedPrismEvent
import fmgp.did.method.prism.proto.OP

/** didResolverPrismJVM/testOnly fmgp.did.method.prism.PrismStateMongoDBSuite
  */
class PrismStateMongoDBSuite extends ZSuite {

  val makePrismState = ZLayer.fromZIO(
    for {
      reactiveMongoApi <- ZIO.service[ReactiveMongoApi]
      state = PrismStateMongoDB(reactiveMongoApi)
    } yield state: PrismState
  )
  val mongo = AsyncDriverResource.layer >>> ReactiveMongoApi
    .layer("mongodb+srv://fabio:ZiT61pB5@cluster0.bgnyyy1.mongodb.net/test")

  val prismStateFixture: FunFixture[ULayer[PrismState]] =
    ZTestLocalFixture { _ => ZIO.succeed(mongo.orDie >>> makePrismState) }(_ => ZIO.unit)

  // import fmgp.did.method.prism.vdr.KeyConstanceUtils._
  import fmgp.did.method.prism.vdr.VDRUtilsTestExtra._

  val aux = Seq(createSSI, createVDR, updateVDR, updateVDR_withTheNewKey, updateSSI_addKey)
    .map(e => hex2bytes(e))
    .map(protoBytes => SignedPrismEvent.parseFrom(protoBytes))
    .zipWithIndex
    .map((proto, index) => MaybeEvent.fromProto(proto, "tx", 0, index))
    .map {
      case InvalidPrismObject(tx, b, reason)                                   => ???
      case InvalidSignedPrismEvent(tx, b, o, reason)                           => ???
      case obj @ MySignedPrismEvent(tx, b, o, signedWith, signature, protobuf) => obj
    }

  prismStateFixture.testZLayered("PrismState create event") {
    for {
      state <- ZIO.service[PrismState]
      _ <- state.addEvent(aux.head)
    } yield ()
  }

}
