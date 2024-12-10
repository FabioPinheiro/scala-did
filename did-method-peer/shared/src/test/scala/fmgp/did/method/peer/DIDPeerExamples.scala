package fmgp.did.method.peer

import zio.json._
import zio.json.ast.Json
import fmgp.did.DIDSubject

object DIDPeerExamples {

  val aliceWithMultiService =
    """did:peer:2
        |.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y
        |.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd
        |.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ
        |.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ
        |.SeyJ0IjoiRElEQ29tbU1lc3NhZ2luZyIsInMiOiJodHRwczovLzMuc2VydmVyLmVuZHBvaW50IiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ
        |.SeyJ0IjoiU2VyaXZlVHlwZTEyMyIsInMiOiJodHRwczovL25ldy5zZXJ2ZXIudHlwZSJ9
        |""".stripMargin.replaceAll("\n", "")

  // Services:
  // {"t":"dm","s":"https://alice.did.fmgp.app/","r":[],"a":["didcomm/v2"]}
  // {"t":"dm","s":"https://alice.did.fmgp.app/","r":[],"a":["didcomm/v2"]}
  // {"t":"DIDCommMessaging","s":"https://3.server.endpoint","r":[],"a":["didcomm/v2"]}
  // {"t":"SeriveType123","s":"https://new.server.type"}

  /** see https://identity.foundation/peer-did-method-spec/#multi-key-creation
    *
    * Ez6LSbysY2xFMRpGMhb7tFTLMpeuPRaqaWM1yECx2AtzE3KCc
    *
    * Vz6MkqRYqQiSgvZQdnBytw86Qbs2ZWUkGv22od935YF4s8M7V
    *
    * Vz6MkgoLTnTypo3tDRwCkZXSccTPHRLhF4ZnjhueYAFpEX6vg
    *
    * SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS9lbmRwb2ludCIsInIiOlsiZGlkOmV4YW1wbGU6c29tZW1lZGlhdG9yI3NvbWVrZXkiXSwiYSI6WyJkaWRjb21tL3YyIiwiZGlkY29tbS9haXAyO2Vudj1yZmM1ODciXX0
    * {{{
    *   {"t":"dm","s":"https://example.com/endpoint","r":["did:example:somemediator#somekey"],"a":["didcomm/v2","didcomm/aip2;env=rfc587"]}
    * }}}
    */
  def ex4_peer2_did =
    "did:peer:2.Ez6LSbysY2xFMRpGMhb7tFTLMpeuPRaqaWM1yECx2AtzE3KCc.Vz6MkqRYqQiSgvZQdnBytw86Qbs2ZWUkGv22od935YF4s8M7V.Vz6MkgoLTnTypo3tDRwCkZXSccTPHRLhF4ZnjhueYAFpEX6vg.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS9lbmRwb2ludCIsInIiOlsiZGlkOmV4YW1wbGU6c29tZW1lZGlhdG9yI3NvbWVrZXkiXSwiYSI6WyJkaWRjb21tL3YyIiwiZGlkY29tbS9haXAyO2Vudj1yZmM1ODciXX0"

  def ex4_peer2_didDocument = """{
   "@context": "https://w3id.org/did/v1",
   "id": "did:peer:2.Ez6LSbysY2xFMRpGMhb7tFTLMpeuPRaqaWM1yECx2AtzE3KCc.Vz6MkqRYqQiSgvZQdnBytw86Qbs2ZWUkGv22od935YF4s8M7V.Vz6MkgoLTnTypo3tDRwCkZXSccTPHRLhF4ZnjhueYAFpEX6vg.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS9lbmRwb2ludCIsInIiOlsiZGlkOmV4YW1wbGU6c29tZW1lZGlhdG9yI3NvbWVrZXkiXSwiYSI6WyJkaWRjb21tL3YyIiwiZGlkY29tbS9haXAyO2Vudj1yZmM1ODciXX0",
   "authentication": [
       {
           "id": "did:peer:2.Ez6LSbysY2xFMRpGMhb7tFTLMpeuPRaqaWM1yECx2AtzE3KCc.Vz6MkqRYqQiSgvZQdnBytw86Qbs2ZWUkGv22od935YF4s8M7V.Vz6MkgoLTnTypo3tDRwCkZXSccTPHRLhF4ZnjhueYAFpEX6vg.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS9lbmRwb2ludCIsInIiOlsiZGlkOmV4YW1wbGU6c29tZW1lZGlhdG9yI3NvbWVrZXkiXSwiYSI6WyJkaWRjb21tL3YyIiwiZGlkY29tbS9haXAyO2Vudj1yZmM1ODciXX0#6MkqRYqQiSgvZQdnBytw86Qbs2ZWUkGv22od935YF4s8M7V",
           "type": "Ed25519VerificationKey2020",
           "controller": "did:peer:2.Ez6LSbysY2xFMRpGMhb7tFTLMpeuPRaqaWM1yECx2AtzE3KCc.Vz6MkqRYqQiSgvZQdnBytw86Qbs2ZWUkGv22od935YF4s8M7V.Vz6MkgoLTnTypo3tDRwCkZXSccTPHRLhF4ZnjhueYAFpEX6vg.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS9lbmRwb2ludCIsInIiOlsiZGlkOmV4YW1wbGU6c29tZW1lZGlhdG9yI3NvbWVrZXkiXSwiYSI6WyJkaWRjb21tL3YyIiwiZGlkY29tbS9haXAyO2Vudj1yZmM1ODciXX0",
           "publicKeyMultibase": "z6MkqRYqQiSgvZQdnBytw86Qbs2ZWUkGv22od935YF4s8M7V"
       },
       {
           "id": "did:peer:2.Ez6LSbysY2xFMRpGMhb7tFTLMpeuPRaqaWM1yECx2AtzE3KCc.Vz6MkqRYqQiSgvZQdnBytw86Qbs2ZWUkGv22od935YF4s8M7V.Vz6MkgoLTnTypo3tDRwCkZXSccTPHRLhF4ZnjhueYAFpEX6vg.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS9lbmRwb2ludCIsInIiOlsiZGlkOmV4YW1wbGU6c29tZW1lZGlhdG9yI3NvbWVrZXkiXSwiYSI6WyJkaWRjb21tL3YyIiwiZGlkY29tbS9haXAyO2Vudj1yZmM1ODciXX0#6MkgoLTnTypo3tDRwCkZXSccTPHRLhF4ZnjhueYAFpEX6vg",
           "type": "Ed25519VerificationKey2020",
           "controller": "did:peer:2.Ez6LSbysY2xFMRpGMhb7tFTLMpeuPRaqaWM1yECx2AtzE3KCc.Vz6MkqRYqQiSgvZQdnBytw86Qbs2ZWUkGv22od935YF4s8M7V.Vz6MkgoLTnTypo3tDRwCkZXSccTPHRLhF4ZnjhueYAFpEX6vg.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS9lbmRwb2ludCIsInIiOlsiZGlkOmV4YW1wbGU6c29tZW1lZGlhdG9yI3NvbWVrZXkiXSwiYSI6WyJkaWRjb21tL3YyIiwiZGlkY29tbS9haXAyO2Vudj1yZmM1ODciXX0",
           "publicKeyMultibase": "z6MkgoLTnTypo3tDRwCkZXSccTPHRLhF4ZnjhueYAFpEX6vg"
       }
   ],
   "keyAgreement": [
       {
           "id": "did:peer:2.Ez6LSbysY2xFMRpGMhb7tFTLMpeuPRaqaWM1yECx2AtzE3KCc.Vz6MkqRYqQiSgvZQdnBytw86Qbs2ZWUkGv22od935YF4s8M7V.Vz6MkgoLTnTypo3tDRwCkZXSccTPHRLhF4ZnjhueYAFpEX6vg.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS9lbmRwb2ludCIsInIiOlsiZGlkOmV4YW1wbGU6c29tZW1lZGlhdG9yI3NvbWVrZXkiXSwiYSI6WyJkaWRjb21tL3YyIiwiZGlkY29tbS9haXAyO2Vudj1yZmM1ODciXX0#6LSbysY2xFMRpGMhb7tFTLMpeuPRaqaWM1yECx2AtzE3KCc",
           "type": "X25519KeyAgreementKey2020",
           "controller": "did:peer:2.Ez6LSbysY2xFMRpGMhb7tFTLMpeuPRaqaWM1yECx2AtzE3KCc.Vz6MkqRYqQiSgvZQdnBytw86Qbs2ZWUkGv22od935YF4s8M7V.Vz6MkgoLTnTypo3tDRwCkZXSccTPHRLhF4ZnjhueYAFpEX6vg.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS9lbmRwb2ludCIsInIiOlsiZGlkOmV4YW1wbGU6c29tZW1lZGlhdG9yI3NvbWVrZXkiXSwiYSI6WyJkaWRjb21tL3YyIiwiZGlkY29tbS9haXAyO2Vudj1yZmM1ODciXX0",
           "publicKeyMultibase": "z6LSbysY2xFMRpGMhb7tFTLMpeuPRaqaWM1yECx2AtzE3KCc"
       }
   ],
   "service": [
       {
           "id": "did:peer:2.Ez6LSbysY2xFMRpGMhb7tFTLMpeuPRaqaWM1yECx2AtzE3KCc.Vz6MkqRYqQiSgvZQdnBytw86Qbs2ZWUkGv22od935YF4s8M7V.Vz6MkgoLTnTypo3tDRwCkZXSccTPHRLhF4ZnjhueYAFpEX6vg.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS9lbmRwb2ludCIsInIiOlsiZGlkOmV4YW1wbGU6c29tZW1lZGlhdG9yI3NvbWVrZXkiXSwiYSI6WyJkaWRjb21tL3YyIiwiZGlkY29tbS9haXAyO2Vudj1yZmM1ODciXX0#service",
           "type": "DIDCommMessaging",
           "serviceEndpoint": {
               "uri":"https://example.com/endpoint",
               "routingKeys": ["did:example:somemediator#somekey"],
               "accept": ["didcomm/v2", "didcomm/aip2;env=rfc587"]
           }
       }
   ]
}"""

  /** @see https://identity.foundation/peer-did-method-spec/#recognizing-and-handling-peer-dids */
  def ex5_peer1 = "did:peer:1zQmZMygzYqNwU6Uhmewx5Xepf2VLp5S4HLSwwgf2aiKZuwa"

  val myExampleDID =
    "did:peer:2.Ez6LSj4X4MjeLXLi6Bnd8gp4crnUD7fBtVFH1xpUmtit5MJNE.Vz6MkwU5tsPanWKYgdEMd1oPghvAoQn41dccHUqkhxJUNdAAY.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3Rlc3QiLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19"

  /** After https://github.com/FabioPinheiro/scala-did/issues/162:
    *   - The id #6LSj4X4MjeLXLi6Bnd8gp4crnUD7fBtVFH1xpUmtit5MJNE is now '#key-2'
    *   - The id #6MkwU5tsPanWKYgdEMd1oPghvAoQn41dccHUqkhxJUNdAAY is now '#key-1'
    */
  val myExampleDIDDocument = s"""{
  "id": "did:peer:2.Ez6LSj4X4MjeLXLi6Bnd8gp4crnUD7fBtVFH1xpUmtit5MJNE.Vz6MkwU5tsPanWKYgdEMd1oPghvAoQn41dccHUqkhxJUNdAAY.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3Rlc3QiLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19",
  "authentication": [
    {
      "id": "did:peer:2.Ez6LSj4X4MjeLXLi6Bnd8gp4crnUD7fBtVFH1xpUmtit5MJNE.Vz6MkwU5tsPanWKYgdEMd1oPghvAoQn41dccHUqkhxJUNdAAY.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3Rlc3QiLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19#key-2",
      "controller": "did:peer:2.Ez6LSj4X4MjeLXLi6Bnd8gp4crnUD7fBtVFH1xpUmtit5MJNE.Vz6MkwU5tsPanWKYgdEMd1oPghvAoQn41dccHUqkhxJUNdAAY.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3Rlc3QiLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19",
      "type": "JsonWebKey2020",
      "publicKeyJwk": {
        "kty": "OKP",
        "crv": "Ed25519",
        "x": "_MzID_BPFZ0p_D5kSH465Dd-zGus913hqeT-kMjH6f0"
      }
    }
  ],
  "keyAgreement": [
    {
      "id": "did:peer:2.Ez6LSj4X4MjeLXLi6Bnd8gp4crnUD7fBtVFH1xpUmtit5MJNE.Vz6MkwU5tsPanWKYgdEMd1oPghvAoQn41dccHUqkhxJUNdAAY.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3Rlc3QiLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19#key-1",
      "controller": "did:peer:2.Ez6LSj4X4MjeLXLi6Bnd8gp4crnUD7fBtVFH1xpUmtit5MJNE.Vz6MkwU5tsPanWKYgdEMd1oPghvAoQn41dccHUqkhxJUNdAAY.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3Rlc3QiLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19",
      "type": "JsonWebKey2020",
      "publicKeyJwk": {
        "kty": "OKP",
        "crv": "X25519",
        "x": "bbsUhf7puTqnVhLFwhChqNBxoQVYeK6bU2Exk0GsqDM"
      }
    }
  ],
  "service": [
    {
      "id": "did:peer:2.Ez6LSj4X4MjeLXLi6Bnd8gp4crnUD7fBtVFH1xpUmtit5MJNE.Vz6MkwU5tsPanWKYgdEMd1oPghvAoQn41dccHUqkhxJUNdAAY.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3Rlc3QiLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19#service",
      "type": "DIDCommMessaging",
      "serviceEndpoint": {
        "uri": "http://localhost:8080/test",
        "accept": ["didcomm/v2"],
        "routingKeys": []
      }
    }
  ]
}"""

  /** missing optional fields of the service endpoint */
  def rootsid_ex_peer2_did =
    "did:peer:2.Ez6LSms555YhFthn1WV8ciDBpZm86hK9tp83WojJUmxPGk1hZ.Vz6MkmdBjMyB4TS5UbbQw54szm8yvMMf1ftGV2sQVYAxaeWhE.SeyJpZCI6Im5ldy1pZCIsInQiOiJkbSIsInMiOiJodHRwczovL21lZGlhdG9yLnJvb3RzaWQuY2xvdWQiLCJhIjpbImRpZGNvbW0vdjIiXX0"

  /** After https://github.com/FabioPinheiro/scala-did/issues/162:
    *   - The id #6MkmdBjMyB4TS5UbbQw54szm8yvMMf1ftGV2sQVYAxaeWhE is now '#key-2'
    *   - The id #6LSms555YhFthn1WV8ciDBpZm86hK9tp83WojJUmxPGk1hZ is now '#key-1'
    */
  val rootsid_ex_peer2_didDocument = """{
    |  "id" : "did:peer:2.Ez6LSms555YhFthn1WV8ciDBpZm86hK9tp83WojJUmxPGk1hZ.Vz6MkmdBjMyB4TS5UbbQw54szm8yvMMf1ftGV2sQVYAxaeWhE.SeyJpZCI6Im5ldy1pZCIsInQiOiJkbSIsInMiOiJodHRwczovL21lZGlhdG9yLnJvb3RzaWQuY2xvdWQiLCJhIjpbImRpZGNvbW0vdjIiXX0",
    |  "authentication" : [
    |    {
    |      "id" : "did:peer:2.Ez6LSms555YhFthn1WV8ciDBpZm86hK9tp83WojJUmxPGk1hZ.Vz6MkmdBjMyB4TS5UbbQw54szm8yvMMf1ftGV2sQVYAxaeWhE.SeyJpZCI6Im5ldy1pZCIsInQiOiJkbSIsInMiOiJodHRwczovL21lZGlhdG9yLnJvb3RzaWQuY2xvdWQiLCJhIjpbImRpZGNvbW0vdjIiXX0#key-2",
    |      "controller" : "did:peer:2.Ez6LSms555YhFthn1WV8ciDBpZm86hK9tp83WojJUmxPGk1hZ.Vz6MkmdBjMyB4TS5UbbQw54szm8yvMMf1ftGV2sQVYAxaeWhE.SeyJpZCI6Im5ldy1pZCIsInQiOiJkbSIsInMiOiJodHRwczovL21lZGlhdG9yLnJvb3RzaWQuY2xvdWQiLCJhIjpbImRpZGNvbW0vdjIiXX0",
    |      "type" : "JsonWebKey2020",
    |      "publicKeyJwk" : {
    |        "kty" : "OKP",
    |        "crv" : "Ed25519",
    |        "x" : "aoy9ByHYBC3xvQZ3FadD4S4TqhaHoRdp2Ix--9soIik"
    |      }
    |    }
    |  ],
    |  "keyAgreement" : [
    |    {
    |      "id" : "did:peer:2.Ez6LSms555YhFthn1WV8ciDBpZm86hK9tp83WojJUmxPGk1hZ.Vz6MkmdBjMyB4TS5UbbQw54szm8yvMMf1ftGV2sQVYAxaeWhE.SeyJpZCI6Im5ldy1pZCIsInQiOiJkbSIsInMiOiJodHRwczovL21lZGlhdG9yLnJvb3RzaWQuY2xvdWQiLCJhIjpbImRpZGNvbW0vdjIiXX0#key-1",
    |      "controller" : "did:peer:2.Ez6LSms555YhFthn1WV8ciDBpZm86hK9tp83WojJUmxPGk1hZ.Vz6MkmdBjMyB4TS5UbbQw54szm8yvMMf1ftGV2sQVYAxaeWhE.SeyJpZCI6Im5ldy1pZCIsInQiOiJkbSIsInMiOiJodHRwczovL21lZGlhdG9yLnJvb3RzaWQuY2xvdWQiLCJhIjpbImRpZGNvbW0vdjIiXX0",
    |      "type" : "JsonWebKey2020",
    |      "publicKeyJwk" : {
    |        "kty" : "OKP",
    |        "crv" : "X25519",
    |        "x" : "l19eKLVCxTFs7_kW1jBpfhWwQCGypJlQ3g5UEh6Z0Fg"
    |      }
    |    }
    |  ],
    |  "service": [
    |    {
    |      "id": "did:peer:2.Ez6LSms555YhFthn1WV8ciDBpZm86hK9tp83WojJUmxPGk1hZ.Vz6MkmdBjMyB4TS5UbbQw54szm8yvMMf1ftGV2sQVYAxaeWhE.SeyJpZCI6Im5ldy1pZCIsInQiOiJkbSIsInMiOiJodHRwczovL21lZGlhdG9yLnJvb3RzaWQuY2xvdWQiLCJhIjpbImRpZGNvbW0vdjIiXX0#service",
    |      "type": "DIDCommMessaging",
    |      "serviceEndpoint": {"uri": "https://mediator.rootsid.cloud","accept": ["didcomm/v2"]}
    |    }
    |  ]
    |}""".stripMargin

  val didpeer4initDoc =
    """{
      |  "@context": [
      |    "https://www.w3.org/ns/did/v1",
      |    "https://w3id.org/security/suites/x25519-2020/v1",
      |    "https://w3id.org/security/suites/ed25519-2020/v1"
      |  ],
      |  "verificationMethod": [
      |    {
      |      "id": "#6LSqPZfn",
      |      "type": "X25519KeyAgreementKey2020",
      |      "publicKeyMultibase": "z6LSqPZfn9krvgXma2icTMKf2uVcYhKXsudCmPoUzqGYW24U"
      |    },
      |    {
      |      "id": "#6MkrCD1c",
      |      "type": "Ed25519VerificationKey2020",
      |      "publicKeyMultibase": "z6MkrCD1csqtgdj8sjrsu8jxcbeyP6m7LiK87NzhfWqio5yr"
      |    }
      |  ],
      |  "authentication": ["#6MkrCD1c"],
      |  "assertionMethod": ["#6MkrCD1c"],
      |  "keyAgreement": ["#6LSqPZfn"],
      |  "capabilityInvocation": ["#6MkrCD1c"],
      |  "capabilityDelegation": ["#6MkrCD1c"],
      |  "service": [
      |    {
      |      "id": "#didcommmessaging-0",
      |      "type": "DIDCommMessaging",
      |      "serviceEndpoint": {
      |        "uri": "didcomm:transport/queue",
      |        "accept": ["didcomm/v2"],
      |        "routingKeys": []
      |      }
      |    }
      |  ]
      |}""".stripMargin.fromJson[Json.Obj].getOrElse(???)

  val didpeer4docAfterResolveLongForm =
    """{
      |  "id" : "did:peer:4zQmd8CpeFPci817KDsbSAKWcXAE2mjvCQSasRewvbSF54Bd:z2M1k7h4psgp4CmJcnQn2Ljp7Pz7ktsd7oBhMU3dWY5s4fhFNj17qcRTQ427C7QHNT6cQ7T3XfRh35Q2GhaNFZmWHVFq4vL7F8nm36PA9Y96DvdrUiRUaiCuXnBFrn1o7mxFZAx14JL4t8vUWpuDPwQuddVo1T8myRiVH7wdxuoYbsva5x6idEpCQydJdFjiHGCpNc2UtjzPQ8awSXkctGCnBmgkhrj5gto3D4i3EREXYq4Z8r2cWGBr2UzbSmnxW2BuYddFo9Yfm6mKjtJyLpF74ytqrF5xtf84MnGFg1hMBmh1xVx1JwjZ2BeMJs7mNS8DTZhKC7KH38EgqDtUZzfjhpjmmUfkXg2KFEA3EGbbVm1DPqQXayPYKAsYPS9AyKkcQ3fzWafLPP93UfNhtUPL8JW5pMcSV3P8v6j3vPXqnnGknNyBprD6YGUVtgLiAqDBDUF3LSxFQJCVYYtghMTv8WuSw9h1a1SRFrDQLGHE4UrkgoRvwaGWr64aM87T1eVGkP5Dt4L1AbboeK2ceLArPScrdYGTpi3BpTkLwZCdjdiFSfTy9okL1YNRARqUf2wm8DvkVGUU7u5nQA3ZMaXWJAewk6k1YUxKd7LvofGUK4YEDtoxN5vb6r1Q2godrGqaPkjfL3RoYPpDYymf9XhcgG8Kx3DZaA6cyTs24t45KxYAfeCw4wqUpCH9HbpD78TbEUr9PPAsJgXBvBj2VVsxnr7FKbK4KykGcg1W8M1JPz21Z4Y72LWgGQCmixovrkHktcTX1uNHjAvKBqVD5C7XmVfHgXCHj7djCh3vzLNuVLtEED8J1hhqsB1oCBGiuh3xXr7fZ9wUjJCQ1HYHqxLJKdYKtoCiPmgKM7etVftXkmTFETZmpM19aRyih3bao76LdpQtbw636r7a3qt8v4WfxsXJetSL8c7t24SqQBcAY89FBsbEnFNrQCMK3JEseKHVaU388ctvRD45uQfe5GndFxthj4iSDomk4uRFd1uRbywoP1tRuabHTDX42UxPjz",
      |  "alsoKnownAs" : ["did:peer:4zQmd8CpeFPci817KDsbSAKWcXAE2mjvCQSasRewvbSF54Bd"],
      |  "verificationMethod" : [
      |    {
      |      "id" : "#6LSqPZfn",
      |      "controller" : "did:peer:4zQmd8CpeFPci817KDsbSAKWcXAE2mjvCQSasRewvbSF54Bd:z2M1k7h4psgp4CmJcnQn2Ljp7Pz7ktsd7oBhMU3dWY5s4fhFNj17qcRTQ427C7QHNT6cQ7T3XfRh35Q2GhaNFZmWHVFq4vL7F8nm36PA9Y96DvdrUiRUaiCuXnBFrn1o7mxFZAx14JL4t8vUWpuDPwQuddVo1T8myRiVH7wdxuoYbsva5x6idEpCQydJdFjiHGCpNc2UtjzPQ8awSXkctGCnBmgkhrj5gto3D4i3EREXYq4Z8r2cWGBr2UzbSmnxW2BuYddFo9Yfm6mKjtJyLpF74ytqrF5xtf84MnGFg1hMBmh1xVx1JwjZ2BeMJs7mNS8DTZhKC7KH38EgqDtUZzfjhpjmmUfkXg2KFEA3EGbbVm1DPqQXayPYKAsYPS9AyKkcQ3fzWafLPP93UfNhtUPL8JW5pMcSV3P8v6j3vPXqnnGknNyBprD6YGUVtgLiAqDBDUF3LSxFQJCVYYtghMTv8WuSw9h1a1SRFrDQLGHE4UrkgoRvwaGWr64aM87T1eVGkP5Dt4L1AbboeK2ceLArPScrdYGTpi3BpTkLwZCdjdiFSfTy9okL1YNRARqUf2wm8DvkVGUU7u5nQA3ZMaXWJAewk6k1YUxKd7LvofGUK4YEDtoxN5vb6r1Q2godrGqaPkjfL3RoYPpDYymf9XhcgG8Kx3DZaA6cyTs24t45KxYAfeCw4wqUpCH9HbpD78TbEUr9PPAsJgXBvBj2VVsxnr7FKbK4KykGcg1W8M1JPz21Z4Y72LWgGQCmixovrkHktcTX1uNHjAvKBqVD5C7XmVfHgXCHj7djCh3vzLNuVLtEED8J1hhqsB1oCBGiuh3xXr7fZ9wUjJCQ1HYHqxLJKdYKtoCiPmgKM7etVftXkmTFETZmpM19aRyih3bao76LdpQtbw636r7a3qt8v4WfxsXJetSL8c7t24SqQBcAY89FBsbEnFNrQCMK3JEseKHVaU388ctvRD45uQfe5GndFxthj4iSDomk4uRFd1uRbywoP1tRuabHTDX42UxPjz",
      |      "type" : "X25519KeyAgreementKey2020",
      |      "publicKeyMultibase" : "z6LSqPZfn9krvgXma2icTMKf2uVcYhKXsudCmPoUzqGYW24U"
      |    },
      |    {
      |      "id" : "#6MkrCD1c",
      |      "controller" : "did:peer:4zQmd8CpeFPci817KDsbSAKWcXAE2mjvCQSasRewvbSF54Bd:z2M1k7h4psgp4CmJcnQn2Ljp7Pz7ktsd7oBhMU3dWY5s4fhFNj17qcRTQ427C7QHNT6cQ7T3XfRh35Q2GhaNFZmWHVFq4vL7F8nm36PA9Y96DvdrUiRUaiCuXnBFrn1o7mxFZAx14JL4t8vUWpuDPwQuddVo1T8myRiVH7wdxuoYbsva5x6idEpCQydJdFjiHGCpNc2UtjzPQ8awSXkctGCnBmgkhrj5gto3D4i3EREXYq4Z8r2cWGBr2UzbSmnxW2BuYddFo9Yfm6mKjtJyLpF74ytqrF5xtf84MnGFg1hMBmh1xVx1JwjZ2BeMJs7mNS8DTZhKC7KH38EgqDtUZzfjhpjmmUfkXg2KFEA3EGbbVm1DPqQXayPYKAsYPS9AyKkcQ3fzWafLPP93UfNhtUPL8JW5pMcSV3P8v6j3vPXqnnGknNyBprD6YGUVtgLiAqDBDUF3LSxFQJCVYYtghMTv8WuSw9h1a1SRFrDQLGHE4UrkgoRvwaGWr64aM87T1eVGkP5Dt4L1AbboeK2ceLArPScrdYGTpi3BpTkLwZCdjdiFSfTy9okL1YNRARqUf2wm8DvkVGUU7u5nQA3ZMaXWJAewk6k1YUxKd7LvofGUK4YEDtoxN5vb6r1Q2godrGqaPkjfL3RoYPpDYymf9XhcgG8Kx3DZaA6cyTs24t45KxYAfeCw4wqUpCH9HbpD78TbEUr9PPAsJgXBvBj2VVsxnr7FKbK4KykGcg1W8M1JPz21Z4Y72LWgGQCmixovrkHktcTX1uNHjAvKBqVD5C7XmVfHgXCHj7djCh3vzLNuVLtEED8J1hhqsB1oCBGiuh3xXr7fZ9wUjJCQ1HYHqxLJKdYKtoCiPmgKM7etVftXkmTFETZmpM19aRyih3bao76LdpQtbw636r7a3qt8v4WfxsXJetSL8c7t24SqQBcAY89FBsbEnFNrQCMK3JEseKHVaU388ctvRD45uQfe5GndFxthj4iSDomk4uRFd1uRbywoP1tRuabHTDX42UxPjz",
      |      "type" : "Ed25519VerificationKey2020",
      |      "publicKeyMultibase" : "z6MkrCD1csqtgdj8sjrsu8jxcbeyP6m7LiK87NzhfWqio5yr"
      |    }
      |  ],
      |  "authentication" : ["#6MkrCD1c"],
      |  "assertionMethod" : ["#6MkrCD1c"],
      |  "keyAgreement" : ["#6LSqPZfn"],
      |  "capabilityInvocation" : ["#6MkrCD1c"],
      |  "capabilityDelegation" : ["#6MkrCD1c"],
      |  "service" : [
      |    {
      |      "id" : "#didcommmessaging-0",
      |      "type" : "DIDCommMessaging",
      |      "serviceEndpoint" : {
      |        "uri" : "didcomm:transport/queue",
      |        "accept" : ["didcomm/v2"],
      |        "routingKeys" : []
      |      }
      |    }
      |  ]
      |}""".stripMargin.fromJson[Json.Obj].getOrElse(???)

  val didpeer4docEncodedExpected =
    "z2M1k7h4psgp4CmJcnQn2Ljp7Pz7ktsd7oBhMU3dWY5s4fhFNj17qcRTQ427C7QHNT6cQ7T3XfRh35Q2GhaNFZmWHVFq4vL7F8nm36PA9Y96DvdrUiRUaiCuXnBFrn1o7mxFZAx14JL4t8vUWpuDPwQuddVo1T8myRiVH7wdxuoYbsva5x6idEpCQydJdFjiHGCpNc2UtjzPQ8awSXkctGCnBmgkhrj5gto3D4i3EREXYq4Z8r2cWGBr2UzbSmnxW2BuYddFo9Yfm6mKjtJyLpF74ytqrF5xtf84MnGFg1hMBmh1xVx1JwjZ2BeMJs7mNS8DTZhKC7KH38EgqDtUZzfjhpjmmUfkXg2KFEA3EGbbVm1DPqQXayPYKAsYPS9AyKkcQ3fzWafLPP93UfNhtUPL8JW5pMcSV3P8v6j3vPXqnnGknNyBprD6YGUVtgLiAqDBDUF3LSxFQJCVYYtghMTv8WuSw9h1a1SRFrDQLGHE4UrkgoRvwaGWr64aM87T1eVGkP5Dt4L1AbboeK2ceLArPScrdYGTpi3BpTkLwZCdjdiFSfTy9okL1YNRARqUf2wm8DvkVGUU7u5nQA3ZMaXWJAewk6k1YUxKd7LvofGUK4YEDtoxN5vb6r1Q2godrGqaPkjfL3RoYPpDYymf9XhcgG8Kx3DZaA6cyTs24t45KxYAfeCw4wqUpCH9HbpD78TbEUr9PPAsJgXBvBj2VVsxnr7FKbK4KykGcg1W8M1JPz21Z4Y72LWgGQCmixovrkHktcTX1uNHjAvKBqVD5C7XmVfHgXCHj7djCh3vzLNuVLtEED8J1hhqsB1oCBGiuh3xXr7fZ9wUjJCQ1HYHqxLJKdYKtoCiPmgKM7etVftXkmTFETZmpM19aRyih3bao76LdpQtbw636r7a3qt8v4WfxsXJetSL8c7t24SqQBcAY89FBsbEnFNrQCMK3JEseKHVaU388ctvRD45uQfe5GndFxthj4iSDomk4uRFd1uRbywoP1tRuabHTDX42UxPjz"
  val didpeer4docHashExpected = "zQmd8CpeFPci817KDsbSAKWcXAE2mjvCQSasRewvbSF54Bd"
  val didpeer4long = DIDSubject(s"did:peer:4$didpeer4docHashExpected:$didpeer4docEncodedExpected")
  val didpeer4short = DIDSubject(s"did:peer:4$didpeer4docHashExpected")
}
