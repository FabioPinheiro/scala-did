package fmgp.did.agent

import zio.json._
import fmgp.did.comm._
import fmgp.did.DidExample
import fmgp.did.AgentProvider

type HASH = String

case class StorageItem(
    original: Option[SignedMessage | EncryptedMessage],
    plaintext: PlaintextMessage,
    from: Option[FROM],
    to: Set[TO],
    timestamp: Long,
)

case class MessageStorage(storageItems: Seq[StorageItem] = Seq.empty) {
  def messageSend(msg: SignedMessage | EncryptedMessage, from: FROM, plaintext: PlaintextMessage) =
    if (storageItems.exists(_.plaintext.id == plaintext.id)) this
    else {
      assert(
        plaintext.from.isEmpty || plaintext.from.contains(from),
        "When sening a message the MUST be empty or the field 'from' MUST be the same as in the PlaintextMessage"
      )
      MessageStorage(
        storageItems = storageItems :+ StorageItem(
          original = Some(msg),
          plaintext = plaintext,
          from = Some(from), // Extra
          to = msg match
            case s: SignedMessage =>
              s.payload.content.fromJson[PlaintextMessage].toOption.flatMap(_.to).getOrElse(Set.empty)
            case e: EncryptedMessage => e.recipientsSubject.map(_.toDID.asTO)
          ,
          timestamp = 0
        )
      )
    }
  def messageRecive(msg: SignedMessage | EncryptedMessage, plaintext: PlaintextMessage) =
    if (storageItems.exists(_.plaintext.id == plaintext.id)) this
    else
      MessageStorage(
        storageItems = storageItems :+ StorageItem(
          original = Some(msg),
          plaintext = plaintext,
          from = plaintext.from,
          to = msg match
            case s: SignedMessage =>
              s.payload.content.fromJson[PlaintextMessage].toOption.flatMap(_.to).getOrElse(Set.empty)
            case e: EncryptedMessage => e.recipientsSubject.map(_.toDID.asTO)
          ,
          timestamp = 0
        )
      )
}

object MessageStorage {

  val alicePingMediator = (
    """{"ciphertext":"3O0_1eYq4xMFm0KY1whN1YiphWVzqMrIckjxF3O0pJnySnnxIGgBqu6uhHShZGCjuABvXCUt1DfLiaJ94ysmUCx_sv5kjzXqbOZUusTQwRRgsiWK1lZNNmGBG1cgVVs9Ii1UUm6aqGvBQON3tIPMCuLhEq048Ml19x5x6jzo8Wm6JJREo569C7JeXYUlJkU9q173CyUjjZhnW523RHa8_JRYz3NH5cYwDjFmz1NGRL27qLdiye5uCUo6JyFo-zPjtD8-ZgXCyBY1OSrWV1Anh1LZtIoKmIG6MDEKq-pn36HqFp9nzLFzx3ycPCLWO3qEpL1zIkYC8zKOLJk8NN4DGiT-lMcMZMmYW7fWpo5eM_iamJX7tCyV7zAfAnyDW_dnF2Q2sL4xdSBd-GFn7oTeuZQzRJTnNcLrGxsiJAZ0XZ2r-QXdJpCa1vpb2YKf2odnKvBbe5FE6AsYr8T_gQ6kkyQrRQcxSvI8voUcWWLS8UL9DLXrgitFAqEgPCahnCMiGYjnWHA0t770zRSX9DJENiM0p0fp6vN-E5dJID2B7qIbflwzdKd8RxNQ-ud2rJzh-HOQi1E7hSMhPkt99pzjNu7UNnW34U-KkDkgoGshN5nGBrZKG885l2ltLIRhVkjros26ZhbYC57s9TCYWKKe7XMFu6DN6VNlvv-k7THU0Q47JW9vkD8xOvWu0Rwg_WAprwrL-BjxX6i5Hv6begiLGAhAy9hp28vZSMRBK0V86yi00PmQb0CbJ_BBeP3qYRHtkwZCvCK5zjR1gQV5ByDDHjbNC7DryEhwRr82wBf36_Tj0CR7N6A-i5M9XPWns4oI","protected":"eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6IkYyTXl5enFBZ3I1Q1ZFU1c0UGd3U21mQTN6Q09NUk5VRXQtQWNZU0RfVGMifSwiYXB2IjoiZG5rREkydXVFem83aTFMNFlDSXg2M01zSnhteEctX1NrejNaM1diWkdtdyIsInNraWQiOiJkaWQ6cGVlcjoyLkV6NkxTZ2h3U0U0Mzd3bkRFMXB0M1g2aFZEVVF6U2pzSHppbnBYM1hGdk1qUkFtN3kuVno2TWtoaDFlNUNFWVlxNkpCVWNUWjZDcDJyYW5DV1JydjdZYXgzTGU0TjU5UjZkZC5TZXlKMElqb2laRzBpTENKeklqb2lhSFIwY0hNNkx5OWhiR2xqWlM1a2FXUXVabTFuY0M1aGNIQXZJaXdpY2lJNlcxMHNJbUVpT2xzaVpHbGtZMjl0YlM5Mk1pSmRmUSM2TFNnaHdTRTQzN3duREUxcHQzWDZoVkRVUXpTanNIemlucFgzWEZ2TWpSQW03eSIsImFwdSI6IlpHbGtPbkJsWlhJNk1pNUZlalpNVTJkb2QxTkZORE0zZDI1RVJURndkRE5ZTm1oV1JGVlJlbE5xYzBoNmFXNXdXRE5ZUm5aTmFsSkJiVGQ1TGxaNk5rMXJhR2d4WlRWRFJWbFpjVFpLUWxWalZGbzJRM0F5Y21GdVExZFNjblkzV1dGNE0weGxORTQxT1ZJMlpHUXVVMlY1U2pCSmFtOXBXa2N3YVV4RFNucEphbTlwWVVoU01HTklUVFpNZVRsb1lrZHNhbHBUTld0aFYxRjFXbTB4Ym1ORE5XaGpTRUYyU1dsM2FXTnBTVFpYTVRCelNXMUZhVTlzYzJsYVIyeHJXVEk1ZEdKVE9USk5hVXBrWmxFak5reFRaMmgzVTBVME16ZDNia1JGTVhCME0xZzJhRlpFVlZGNlUycHpTSHBwYm5CWU0xaEdkazFxVWtGdE4zayIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJBMjU2Q0JDLUhTNTEyIiwiYWxnIjoiRUNESC0xUFUrQTI1NktXIn0","recipients":[{"encrypted_key":"F5ExrYtwMcoTKQqmhPv9wj3chydlc6n50GTxrRp_GlK18lU3y0RktjN1lMbXiGrZobedUDssV6C4zcH1DzjcsAstKdeLiPrp","header":{"kid":"did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9zaXQtcHJpc20tbWVkaWF0b3IuYXRhbGFwcmlzbS5pbyIsInIiOltdLCJhIjpbImRpZGNvbW0vdjIiXX0#6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y"}}],"tag":"fjsv7G-9DJud6L9O9GeGImeF5AXxs__m5uJYwxzFpvE","iv":"AlB-V302pW6FOTZ2ItM-SA"}"""
      .fromJson[EncryptedMessage]
      .getOrElse(???),
    """{
  "id" : "e1b74b26-2361-47c6-9db2-7d25a5b339c7",
  "type" : "https://didcomm.org/trust-ping/2.0/ping",
  "to" : [
    "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9zaXQtcHJpc20tbWVkaWF0b3IuYXRhbGFwcmlzbS5pbyIsInIiOltdLCJhIjpbImRpZGNvbW0vdjIiXX0"
  ],
  "from" : "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ",
  "body" : {
    "response_requested" : true
  },
  "typ" : "application/didcomm-plain+json"
}""".fromJson[PlaintextMessage].getOrElse(???)
  )

  val mediatorResponseToAlicePing = (
    """{"ciphertext":"hbUQWeFyVpNXP3SbEaX38_pUd0l4KDA_uv-Fjy2eIc4QUJzv3mTMDjisHX_NonW0Imfukrk_H3l8mngyQhKu0YEtcSx-iEby5x3tIOLd2JgYFh9ox6FfuriqlKX4kTnhav2mJZTaWZ-O2qcMNEIVu4MRJLHzq9HATiPnbLiW4aFpseAntls-of1DNml-g-nWMWGhd6w7C0A0iqOSw36sLW4x_f43B2wJpo2O2gLMUdneX0MOp6e9vzLqIwj4_w8J86yqCYgoZyDpQpqjZCQKekRaJYdLOpC3ZnmcAiLiOcJ6nKqvTEAdyac3tWCTcAQMLcN3TB8oBVHIuMcp4CR-cBP3BNj52wsciN9X7X1Ywt8N7nAMU-kDDcqaOKwtOovQ7PBVsHkrF_Umk2QtNKayzcGbeuVXGE-kSmFe1JTf2GjiOsuwCurd9EZDXH1mD1ZbZd8CVS-uC-iJLpmscdfoAQBH9zQzoA1tMkk2IYaoKK6zBhsRyqwqh6If5RymbVRlopfgrI7j8fWtg5qTqqkW3k7xfUSA195Ou_ID3sRLbbruTvGO6BcmwKeU1BnCINxce_-kj2IhuLqjhDR6FgwDG9n7sZyzHKwudVer-hA53hwoATwLnKgDGlpYZfgTxTKsCZR_r2Yrq32X9aqjlLCyLVBcIutC64Z3Zbh332HZ-6XYSgyxZSHMDTrW4vLnKWuZLmgkJ0-NpL-ejJCHKBGoC2MFpHAS9yT9KTrqnfZKE4_xhtveSNhPS1DFwUv2Ms8Z4NMvj4UhuIu_WL6Dj2rc4s9XkrCcbIOHZyH-e9a1bzYfqFv_v-Ad-5HwTikGq4N4K47ZZvX2myBIX7gwytjs_rzbfawMFaKaAEh7ohsYPhg","protected":"eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6ImlPejFzN3RKbDB6azhTVGhmOW52TTdkZHF2NVVOUkhqZENnTXNZM1puWGsifSwiYXB2IjoiLWNOQ3l0eFVrSHpSRE5SckV2Vm05S0VmZzhZcUtQVnVVcVg1a0VLbU9yMCIsInNraWQiOiJkaWQ6cGVlcjoyLkV6NkxTZ2h3U0U0Mzd3bkRFMXB0M1g2aFZEVVF6U2pzSHppbnBYM1hGdk1qUkFtN3kuVno2TWtoaDFlNUNFWVlxNkpCVWNUWjZDcDJyYW5DV1JydjdZYXgzTGU0TjU5UjZkZC5TZXlKMElqb2laRzBpTENKeklqb2lhSFIwY0hNNkx5OXphWFF0Y0hKcGMyMHRiV1ZrYVdGMGIzSXVZWFJoYkdGd2NtbHpiUzVwYnlJc0luSWlPbHRkTENKaElqcGJJbVJwWkdOdmJXMHZkaklpWFgwIzZMU2dod1NFNDM3d25ERTFwdDNYNmhWRFVRelNqc0h6aW5wWDNYRnZNalJBbTd5IiwiYXB1IjoiWkdsa09uQmxaWEk2TWk1RmVqWk1VMmRvZDFORk5ETTNkMjVFUlRGd2RETllObWhXUkZWUmVsTnFjMGg2YVc1d1dETllSblpOYWxKQmJUZDVMbFo2TmsxcmFHZ3haVFZEUlZsWmNUWktRbFZqVkZvMlEzQXljbUZ1UTFkU2NuWTNXV0Y0TTB4bE5FNDFPVkkyWkdRdVUyVjVTakJKYW05cFdrY3dhVXhEU25wSmFtOXBZVWhTTUdOSVRUWk1lVGw2WVZoUmRHTklTbkJqTWpCMFlsZFdhMkZYUmpCaU0wbDFXVmhTYUdKSFJuZGpiV3g2WWxNMWNHSjVTWE5KYmtscFQyeDBaRXhEU21oSmFuQmlTVzFTY0ZwSFRuWmlWekIyWkdwSmFWaFlNQ00yVEZObmFIZFRSVFF6TjNkdVJFVXhjSFF6V0Rab1ZrUlZVWHBUYW5OSWVtbHVjRmd6V0VaMlRXcFNRVzAzZVEiLCJ0eXAiOiJhcHBsaWNhdGlvbi9kaWRjb21tLWVuY3J5cHRlZCtqc29uIiwiZW5jIjoiQTI1NkNCQy1IUzUxMiIsImFsZyI6IkVDREgtMVBVK0EyNTZLVyJ9","recipients":[{"encrypted_key":"p0-k-eJDXT1dosrCBf5BECUB1uz32aUYIfaPXiHj9YFp0bg0c2-7sWXMAVq8RL2bd70ku2XT9FfoXv1bMpIYZWR8jvIWe6EC","header":{"kid":"did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ#6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y"}}],"tag":"Tx-LKPJFw3a4s_3VWiBoiREdixausnqk1DqfUdZ42DA","iv":"HeZFG-1neUSDghTuinosBA"}"""
      .fromJson[EncryptedMessage]
      .getOrElse(???),
    """{
  "id" : "edd914d7-ab31-4340-80f9-912eacab77a1",
  "type" : "https://didcomm.org/trust-ping/2.0/ping-response",
  "to" : [
    "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9hbGljZS5kaWQuZm1ncC5hcHAvIiwiciI6W10sImEiOlsiZGlkY29tbS92MiJdfQ"
  ],
  "from" : "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9zaXQtcHJpc20tbWVkaWF0b3IuYXRhbGFwcmlzbS5pbyIsInIiOltdLCJhIjpbImRpZGNvbW0vdjIiXX0",
  "thid" : "e1b74b26-2361-47c6-9db2-7d25a5b339c7",
  "body" : {},
  "typ" : "application/didcomm-plain+json"
}""".fromJson[PlaintextMessage].getOrElse(???)
  )

  val example = MessageStorage()
    .messageSend(
      msg = alicePingMediator._1,
      from = AgentProvider.alice.id: FROM,
      plaintext = alicePingMediator._2
    )
    .messageRecive(
      mediatorResponseToAlicePing._1,
      mediatorResponseToAlicePing._2
    )
}
