package fmgp.did.comm

import zio._
import zio.json._
import fmgp.util.{Base64, Base64Obj}

object SignedMessageExamples {

  def allSignedMessage_json = Seq(
    exampleSignatureEdDSA_json,
    // exampleSignatureEdDSA_obj,
    exampleSignatureES256_json,
    // exampleSignatureES256_obj,
    exampleSignatureES256K_json,
    // exampleSignatureES256K_obj,
    // exampleSignatureEdDSA_failSignature_obj,
  )

  val exampleSignatureEdDSA_json = """{
   "payload":"eyJpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXBsYWluK2pzb24iLCJ0eXBlIjoiaHR0cDovL2V4YW1wbGUuY29tL3Byb3RvY29scy9sZXRzX2RvX2x1bmNoLzEuMC9wcm9wb3NhbCIsImZyb20iOiJkaWQ6ZXhhbXBsZTphbGljZSIsInRvIjpbImRpZDpleGFtcGxlOmJvYiJdLCJjcmVhdGVkX3RpbWUiOjE1MTYyNjkwMjIsImV4cGlyZXNfdGltZSI6MTUxNjM4NTkzMSwiYm9keSI6eyJtZXNzYWdlc3BlY2lmaWNhdHRyaWJ1dGUiOiJhbmQgaXRzIHZhbHVlIn19",
   "signatures":[
      {
         "protected":"eyJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXNpZ25lZCtqc29uIiwiYWxnIjoiRWREU0EifQ",
         "signature":"FW33NnvOHV0Ted9-F7GZbkia-vYAfBKtH4oBxbrttWAhBZ6UFJMxcGjL3lwOl4YohI3kyyd08LHPWNMgP2EVCQ",
         "header":{
            "kid":"did:example:alice#key-1"
         }
      }
   ]
}"""

  val exampleSignatureEdDSA_obj = SignedMessage(
    Payload.fromBase64url(
      "eyJpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXBsYWluK2pzb24iLCJ0eXBlIjoiaHR0cDovL2V4YW1wbGUuY29tL3Byb3RvY29scy9sZXRzX2RvX2x1bmNoLzEuMC9wcm9wb3NhbCIsImZyb20iOiJkaWQ6ZXhhbXBsZTphbGljZSIsInRvIjpbImRpZDpleGFtcGxlOmJvYiJdLCJjcmVhdGVkX3RpbWUiOjE1MTYyNjkwMjIsImV4cGlyZXNfdGltZSI6MTUxNjM4NTkzMSwiYm9keSI6eyJtZXNzYWdlc3BlY2lmaWNhdHRyaWJ1dGUiOiJhbmQgaXRzIHZhbHVlIn19"
    ),
    Seq(
      JWMSignatureObj(
        `protected` = Base64("eyJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXNpZ25lZCtqc29uIiwiYWxnIjoiRWREU0EifQ")
          .unsafeAsObj[SignProtectedHeader],
        signature =
          SignatureJWM("FW33NnvOHV0Ted9-F7GZbkia-vYAfBKtH4oBxbrttWAhBZ6UFJMxcGjL3lwOl4YohI3kyyd08LHPWNMgP2EVCQ"),
        header = Some(JWMHeader("did:example:alice#key-1")),
      )
    )
  )

  val exampleSignatureES256_json = """{
   "payload":"eyJpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXBsYWluK2pzb24iLCJ0eXBlIjoiaHR0cDovL2V4YW1wbGUuY29tL3Byb3RvY29scy9sZXRzX2RvX2x1bmNoLzEuMC9wcm9wb3NhbCIsImZyb20iOiJkaWQ6ZXhhbXBsZTphbGljZSIsInRvIjpbImRpZDpleGFtcGxlOmJvYiJdLCJjcmVhdGVkX3RpbWUiOjE1MTYyNjkwMjIsImV4cGlyZXNfdGltZSI6MTUxNjM4NTkzMSwiYm9keSI6eyJtZXNzYWdlc3BlY2lmaWNhdHRyaWJ1dGUiOiJhbmQgaXRzIHZhbHVlIn19",
   "signatures":[
      {
         "protected":"eyJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXNpZ25lZCtqc29uIiwiYWxnIjoiRVMyNTYifQ",
         "signature":"gcW3lVifhyR48mLHbbpnGZQuziskR5-wXf6IoBlpa9SzERfSG9I7oQ9pssmHZwbvJvyMvxskpH5oudw1W3X5Qg",
         "header":{
            "kid":"did:example:alice#key-2"
         }
      }
   ]
}"""

  val exampleSignatureES256_obj = SignedMessage(
    Payload.fromBase64url(
      "eyJpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXBsYWluK2pzb24iLCJ0eXBlIjoiaHR0cDovL2V4YW1wbGUuY29tL3Byb3RvY29scy9sZXRzX2RvX2x1bmNoLzEuMC9wcm9wb3NhbCIsImZyb20iOiJkaWQ6ZXhhbXBsZTphbGljZSIsInRvIjpbImRpZDpleGFtcGxlOmJvYiJdLCJjcmVhdGVkX3RpbWUiOjE1MTYyNjkwMjIsImV4cGlyZXNfdGltZSI6MTUxNjM4NTkzMSwiYm9keSI6eyJtZXNzYWdlc3BlY2lmaWNhdHRyaWJ1dGUiOiJhbmQgaXRzIHZhbHVlIn19"
    ),
    Seq(
      JWMSignatureObj(
        `protected` = Base64("eyJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXNpZ25lZCtqc29uIiwiYWxnIjoiRVMyNTYifQ")
          .unsafeAsObj[SignProtectedHeader],
        signature =
          SignatureJWM("gcW3lVifhyR48mLHbbpnGZQuziskR5-wXf6IoBlpa9SzERfSG9I7oQ9pssmHZwbvJvyMvxskpH5oudw1W3X5Qg"),
        header = Some(JWMHeader("did:example:alice#key-2")),
      )
    )
  )

  /** From [[fmgp.did.comm.Examples.plaintextMessage]] */
  val exampleSignatureES256K_json = """{
  "payload":"eyJpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXBsYWluK2pzb24iLCJ0eXBlIjoiaHR0cDovL2V4YW1wbGUuY29tL3Byb3RvY29scy9sZXRzX2RvX2x1bmNoLzEuMC9wcm9wb3NhbCIsImZyb20iOiJkaWQ6ZXhhbXBsZTphbGljZSIsInRvIjpbImRpZDpleGFtcGxlOmJvYiJdLCJjcmVhdGVkX3RpbWUiOjE1MTYyNjkwMjIsImV4cGlyZXNfdGltZSI6MTUxNjM4NTkzMSwiYm9keSI6eyJtZXNzYWdlc3BlY2lmaWNhdHRyaWJ1dGUiOiJhbmQgaXRzIHZhbHVlIn19",
  "signatures":[
     {
        "protected":"eyJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXNpZ25lZCtqc29uIiwiYWxnIjoiRVMyNTZLIn0",
        "signature":"EGjhIcts6tqiJgqtxaTiTY3EUvL-_rLjn9lxaZ4eRUwa1-CS1nknZoyJWbyY5NQnUafWh5nvCtQpdpMyzH3blw",
        "header":{
           "kid":"did:example:alice#key-3"
        }
     }
  ]
}"""

  /** From [[fmgp.did.comm.Examples.plaintextMessage]] */
  val exampleSignatureES256K_obj = SignedMessage(
    Payload.fromBase64url(
      "eyJpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXBsYWluK2pzb24iLCJ0eXBlIjoiaHR0cDovL2V4YW1wbGUuY29tL3Byb3RvY29scy9sZXRzX2RvX2x1bmNoLzEuMC9wcm9wb3NhbCIsImZyb20iOiJkaWQ6ZXhhbXBsZTphbGljZSIsInRvIjpbImRpZDpleGFtcGxlOmJvYiJdLCJjcmVhdGVkX3RpbWUiOjE1MTYyNjkwMjIsImV4cGlyZXNfdGltZSI6MTUxNjM4NTkzMSwiYm9keSI6eyJtZXNzYWdlc3BlY2lmaWNhdHRyaWJ1dGUiOiJhbmQgaXRzIHZhbHVlIn19"
    ),
    Seq(
      JWMSignatureObj(
        `protected` = Base64("eyJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXNpZ25lZCtqc29uIiwiYWxnIjoiRVMyNTZLIn0")
          .unsafeAsObj[SignProtectedHeader],
        signature =
          SignatureJWM("EGjhIcts6tqiJgqtxaTiTY3EUvL-_rLjn9lxaZ4eRUwa1-CS1nknZoyJWbyY5NQnUafWh5nvCtQpdpMyzH3blw"),
        header = Some(JWMHeader("did:example:alice#key-3")),
      )
    )
  )

  /** Wrong signature */
  val exampleSignatureEdDSA_failSignature_obj = SignedMessage(
    Payload.fromBase64url(
      "eyJpZCI6IjEyMzQ1Njc4OTAiLCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXBsYWluK2pzb24iLCJ0eXBlIjoiaHR0cDovL2V4YW1wbGUuY29tL3Byb3RvY29scy9sZXRzX2RvX2x1bmNoLzEuMC9wcm9wb3NhbCIsImZyb20iOiJkaWQ6ZXhhbXBsZTphbGljZSIsInRvIjpbImRpZDpleGFtcGxlOmJvYiJdLCJjcmVhdGVkX3RpbWUiOjE1MTYyNjkwMjIsImV4cGlyZXNfdGltZSI6MTUxNjM4NTkzMSwiYm9keSI6eyJtZXNzYWdlc3BlY2lmaWNhdHRyaWJ1dGUiOiJhbmQgaXRzIHZhbHVlIn19"
    ),
    Seq(
      JWMSignatureObj(
        `protected` = Base64("eyJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXNpZ25lZCtqc29uIiwiYWxnIjoiRWREU0EifQ")
          .unsafeAsObj[SignProtectedHeader],
        signature =
          SignatureJWM("gcW3lVifhyR48mLHbbpnGZQuziskR5-wXf6IoBlpa9SzERfSG9I7oQ9pssmHZwbvJvyMvxskpH5oudw1W3X5Qg"),
        header = Some(JWMHeader("did:example:alice#key-1")),
      )
    )
  )

  /** SignedMessage of a trust-ping from Alice to Bob
    *
    * Created with https://did.fmgp.app/#/encrypt
    */
  val exampleTrustPingFromAliceToBob_json = """{
    |"payload" : "eyJpZCI6ImE3Yzk1MDQ2LTVjZTktNDM2OS1hNThiLWQxNzlmYmU5MmZjZCIsInR5cGUiOiJodHRwczovL2RpZGNvbW0ub3JnL3RydXN0LXBpbmcvMi4wL3BpbmciLCJ0byI6WyJkaWQ6ZXhhbXBsZTpib2IiXSwiZnJvbSI6ImRpZDpleGFtcGxlOmFsaWNlIiwiYm9keSI6eyJyZXNwb25zZV9yZXF1ZXN0ZWQiOnRydWV9LCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXBsYWluK2pzb24ifQ",
    |"signatures" : [
    |  {
    |    "protected" : "eyJraWQiOiJkaWQ6ZXhhbXBsZTphbGljZSNrZXktMyIsImFsZyI6IkVTMjU2SyJ9",
    |    "signature" : "pzZ6R6mrHmvqfd3I16MW3yEO1xqBNybYD-N4bLIRaAoRLRpfA5WCOPpLv8gSxZm_IjAfLCrfvFj73VcB2qebPQ"
    |  }
    |]
    |}""".stripMargin

  /** SignedMessage of a trust-ping from Alice to Bob
    *
    * Created with https://did.fmgp.app/#/encrypt
    */
  val exampleTrustPingFromAliceToBob_obj = SignedMessage(
    Payload.fromBase64url(
      "eyJpZCI6ImE3Yzk1MDQ2LTVjZTktNDM2OS1hNThiLWQxNzlmYmU5MmZjZCIsInR5cGUiOiJodHRwczovL2RpZGNvbW0ub3JnL3RydXN0LXBpbmcvMi4wL3BpbmciLCJ0byI6WyJkaWQ6ZXhhbXBsZTpib2IiXSwiZnJvbSI6ImRpZDpleGFtcGxlOmFsaWNlIiwiYm9keSI6eyJyZXNwb25zZV9yZXF1ZXN0ZWQiOnRydWV9LCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLXBsYWluK2pzb24ifQ"
    ),
    Seq(
      JWMSignatureObj(
        `protected` = Base64("eyJraWQiOiJkaWQ6ZXhhbXBsZTphbGljZSNrZXktMyIsImFsZyI6IkVTMjU2SyJ9")
          .unsafeAsObj[SignProtectedHeader],
        signature =
          SignatureJWM("pzZ6R6mrHmvqfd3I16MW3yEO1xqBNybYD-N4bLIRaAoRLRpfA5WCOPpLv8gSxZm_IjAfLCrfvFj73VcB2qebPQ"),
        header = None,
      )
    )
  )
}
