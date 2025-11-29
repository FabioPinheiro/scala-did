package fmgp.did.comm.protocol.chatriqube

import zio.json.*

enum SubjectType:
  case Email extends SubjectType
  case Discord extends SubjectType
  case Tel extends SubjectType
  case Domain extends SubjectType

object SubjectType {
  given decoder: JsonDecoder[SubjectType] =
    JsonDecoder.string.mapOrFail(e => fmgp.util.safeValueOf(SubjectType.valueOf(e)))
  given encoder: JsonEncoder[SubjectType] = JsonEncoder.string.contramap((e: SubjectType) => e.toString)
}
