package fmgp.did.comm.protocol.reportproblem2

import zio.json._
import scala.util.matching.Regex

/** https://identity.foundation/didcomm-messaging/spec/#problem-reports
  *
  * @param sorter
  *   tells whether the consequence of the problem are fully understood.
  *   - `e` is a error
  *   - `w` is a warning
  * @param scope
  *   gives the sender’s opinion about how much context should be undone if the problem is deemed an erro.
  *   - `p` if error undo the full protocol
  *   - `m` if error undo last step
  *   - <state> if error until state
  * @param descriptors
  */
case class ProblemCode(sorter: 'e' | 'w', scope: 'p' | 'm' | String, descriptors: String*) {
  def value = (sorter +: scope +: descriptors).mkString(".")
}

object ProblemCode {
  // https://regex101.com/r/B0v1Jo/1
  val pattern: Regex = "^([ew])\\.([pm]|[^.]+)\\.((?:[^.\\s]+\\.?)+)$".r

  def ErroFail(descriptors: String*) =
    ProblemCode('e', 'p', descriptors.mkString(".").split("\\.")*)
  def ErroUndo(descriptors: String*) =
    ProblemCode('e', 'm', descriptors.mkString(".").split("\\.")*)
  def ErroUndoToStep(step: String, descriptors: String*) =
    ProblemCode('e', step, descriptors.mkString(".").split("\\.")*)
  def WarnFail(descriptors: String*) =
    ProblemCode('w', 'p', descriptors.mkString(".").split("\\.")*)
  def WarnUndo(descriptors: String*) =
    ProblemCode('w', 'm', descriptors.mkString(".").split("\\.")*)
  def WarnUndoToStep(step: String, descriptors: String*) =
    ProblemCode('w', step, descriptors.mkString(".").split("\\.")*)

  def fromString(codeStr: String): Either[String, ProblemCode] = codeStr match {
    case pattern("e", "p", tmp_descriptors) =>
      Right(ProblemCode('e', 'p', tmp_descriptors.split("\\.")*))
    case pattern("e", "m", tmp_descriptors) =>
      Right(ProblemCode('e', 'm', tmp_descriptors.split("\\.")*))
    case pattern("e", tmp_scope, tmp_descriptors) =>
      Right(ProblemCode('e', tmp_scope, tmp_descriptors.split("\\.")*))
    case pattern("w", "p", tmp_descriptors) =>
      Right(ProblemCode('w', 'p', tmp_descriptors.split("\\.")*))
    case pattern("w", "m", tmp_descriptors) =>
      Right(ProblemCode('w', 'm', tmp_descriptors.split("\\.")*))
    case pattern("w", tmp_scope, tmp_descriptors) =>
      Right(ProblemCode('w', tmp_scope, tmp_descriptors.split("\\.")*))
    case any => Left(s"Not valid ProblemReport's code: '$any'")
  }

  given decoder: JsonDecoder[ProblemCode] = JsonDecoder.string.mapOrFail(ProblemCode.fromString(_))
  given encoder: JsonEncoder[ProblemCode] = JsonEncoder.string.contramap[ProblemCode](_.value)
}

// opaque type ProblemCode = String
// object ProblemCode:
//   def apply(value: String): ProblemCode = value
//   def apply(sorter: 'e' | 'w', scope: 'p' | 'm' | String, descriptors: String*): ProblemCode =
//     (Seq(sorter, scope) ++ descriptors).mkString(".")
//   extension (messageID: ProblemCode)
//     def value: String = messageID
//     def sorter: 'e' | 'w' =
//   given decoder: JsonDecoder[ProblemCode] = JsonDecoder.string.map(ProblemCode(_))
//   given encoder: JsonEncoder[ProblemCode] = JsonEncoder.string.contramap[ProblemCode](_.value)
