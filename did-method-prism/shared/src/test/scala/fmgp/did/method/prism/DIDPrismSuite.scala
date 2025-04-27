package fmgp.did.method.prism

import munit._
import zio._
import zio.json._
import fmgp.crypto._
import fmgp.did._
import fmgp.multibase._

import DIDPrismExamples._
import zio.json.ast.Json
import fmgp.util.Base64
import scala.util.matching.Regex

/** didResolverPrismJVM/testOnly fmgp.did.method.prism.DIDPrismSuite */
class DIDPrismSuite extends ZSuite {

  test("Check regex for prism ex1 (long form)") {
    val d = ex1_prism_specs_long
    assert(DIDPrism.regexPrism.matches(d))
    assert(DIDPrism.regexPrismLongForm.matches(d))
    assert(!DIDPrism.regexPrismShortForm.matches(d))
  }

  test("Check regex for prism ex1 (short form)") {
    val d = ex1_prism_specs_short
    assert(DIDPrism.regexPrism.matches(d))
    assert(!DIDPrism.regexPrismLongForm.matches(d))
    assert(DIDPrism.regexPrismShortForm.matches(d))
  }

  test("Check regex for prism ex2 (short form)") {
    val d = ex2_prism_specs_short
    assert(DIDPrism.regexPrism.matches(d))
    assert(!DIDPrism.regexPrismLongForm.matches(d))
    assert(DIDPrism.regexPrismShortForm.matches(d))
  }

}
