package fmgp.did.comm

import munit.*

import zio.*
import zio.json.*
import zio.json.ast.Json

/** didJVM/testOnly fmgp.did.comm.EnumTypesSuite */
class EnumTypesSuite extends FunSuite {
  test("Parse ReturnRoute 'all'") {
    "\"all\"".fromJson[ReturnRoute] match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj, ReturnRoute.all)
    }
  }

  test("Parse ReturnRoute 'thread'") {
    "\"thread\"".fromJson[ReturnRoute] match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj, ReturnRoute.thread)
    }
  }

  test("Parse ReturnRoute 'none'") {
    "\"none\"".fromJson[ReturnRoute] match {
      case Left(error) => fail(error)
      case Right(obj)  => assertEquals(obj, ReturnRoute.none)
    }
  }
}
