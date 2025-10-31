package fmgp.did.comm.protocol.reportproblem2

import zio.json._

import fmgp.did._
import fmgp.did.comm._

extension (msg: PlaintextMessage)
  def toProblemReport: Either[String, ProblemReport] =
    ProblemReport.fromPlaintextMessage(msg)

/** {{{
  *  {
  *   "type": "https://didcomm.org/report-problem/2.0/problem-report",
  *   "id": "7c9de639-c51c-4d60-ab95-103fa613c805",
  *   "pthid": "1e513ad4-48c9-444e-9e7e-5b8b45c5e325",
  *   "ack": ["1e513ad4-48c9-444e-9e7e-5b8b45c5e325"],
  *   "body": {
  *     "code": "e.p.xfer.cant-use-endpoint",
  *     "comment": "Unable to use the {1} endpoint for {2}.",
  *     "args": [
  *       "https://agents.r.us/inbox",
  *       "did:sov:C805sNYhMrjHiqZDTUASHg"
  *     ],
  *     "escalate_to": "mailto:admin@foo.org"
  *   }
  * }
  * }}}
  *
  * @param pthid
  *   REQUIRED - The value is the thid of the thread in which the problem occurred. (Thus, the problem report begins a
  *   new child thread, of which the triggering context is the parent. The parent context can react immediately to the
  *   problem, or can suspend progress while troubleshooting occurs.)
  * @param ack
  *   OPTIONAL - It SHOULD be included if the problem in question was triggered directly by a preceding message.
  *   (Contrast problems arising from a timeout or a user deciding to cancel a transaction, which can arise independent
  *   of a preceding message. In such cases, ack MAY still be used, but there is no strong recommendation.)
  * @param code
  *   REQUIRED - Deserves a rich explanation; see Problem Codes below.
  * @param comment
  *   OPTIONAL - but recommended. Contains human-friendly text describing the problem. If the field is present, the text
  *   MUST be statically associated with code, meaning that each time circumstances trigger a problem with the same
  *   code, the value of comment will be the same. This enables localization and cached lookups, and it has some
  *   cybersecurity benefits. The value of comment supports simple interpolation with args (see next), where args are
  *   referenced as {1}, {2}, and so forth.
  * @param args
  *   OPTIONAL - Contains situation-specific values that are interpolated into the value of comment, providing extra
  *   detail for human readers. Each unique problem code has a definition for the args it takes. In this example,
  *   e.p.xfer.cant-use-endpoint apparently expects two values in args: the first is a URL and the second is a DID.
  *   Missing or null args MUST be replaced with a question mark character (?) during interpolation; extra args MUST be
  *   appended to the main text as comma-separated values.
  * @param escalate_to
  *   OPTIONAL - Provides a URI where additional help on the issue can be received.
  */
case class ProblemReport(
    id: MsgID = MsgID(),
    to: Set[TO],
    from: FROM, // Can it be Option?
    pthid: MsgID,
    ack: Option[Seq[MsgID]],
    // body:
    code: ProblemCode,
    comment: Option[String],
    args: Option[Seq[String]],
    escalate_to: Option[String],
) {
  def piuri = ProblemReport.piuri

  def toPlaintextMessage: PlaintextMessage =
    PlaintextMessageClass(
      id = id,
      `type` = piuri,
      to = Some(to),
      from = Some(from),
      pthid = Some(pthid),
      ack = ack,
      body = Some(
        ProblemReport
          .Body(
            code = code,
            comment = comment,
            args = args,
            escalate_to = escalate_to,
          )
          .toJSON_RFC7159
      ),
    )

  def commentWithArgs = (comment, args) match
    case (None, _)          => None
    case (Some(c), None)    => Some(c)
    case (Some(c), Some(a)) =>
      Some(a.zipWithIndex.foldLeft(c) { case (text, (arg, index)) => text.replaceAll(s"\\{${index + 1}\\}", arg) })
}

object ProblemReport {
  def piuri = PIURI("https://didcomm.org/report-problem/2.0/problem-report")

  protected final case class Body(
      code: ProblemCode,
      comment: Option[String],
      args: Option[Seq[String]],
      escalate_to: Option[String],
  ) {

    /** toJSON_RFC7159 MUST not fail! */
    def toJSON_RFC7159: JSON_RFC7159 = this.toJsonAST.flatMap(_.as[JSON_RFC7159]).getOrElse(JSON_RFC7159())
  }
  protected object Body {
    given decoder: JsonDecoder[Body] = DeriveJsonDecoder.gen[Body]
    given encoder: JsonEncoder[Body] = DeriveJsonEncoder.gen[Body]
  }

  def fromPlaintextMessage(msg: PlaintextMessage): Either[String, ProblemReport] = {
    if (msg.`type` != piuri) Left(s"No able to create ProblemReport from a Message of the type '${msg.`type`}'")
    else
      msg.from match
        case None       => Left(s"'$piuri' MUST have field 'from'")
        case Some(from) =>
          msg.pthid match
            case None        => Left(s"'$piuri' MUST have field 'pthid'")
            case Some(pthid) =>
              msg.body match
                case None    => Left(s"'$piuri' MUST have field 'body'")
                case Some(b) =>
                  b.as[Body].map { body =>
                    ProblemReport(
                      id = msg.id,
                      to = msg.to.toSet.flatten,
                      from = from,
                      pthid = pthid,
                      ack = msg.ack,
                      code = body.code,
                      comment = body.comment,
                      args = body.args,
                      escalate_to = body.escalate_to,
                    )
                  }
  }
}
