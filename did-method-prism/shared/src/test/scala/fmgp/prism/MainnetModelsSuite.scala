package fmgp.prism

import munit._
import zio.json._

class MainnetModelsSuite extends FunSuite {
  test("CardanoMetadata 6451") {
    assert(MainnetExamples.metadata_6451.toCardanoPrismEntry.isRight)
  }
  test("CardanoMetadata 6452") {
    assert(MainnetExamples.metadata_6452.toCardanoPrismEntry.isLeft)
  }
}
