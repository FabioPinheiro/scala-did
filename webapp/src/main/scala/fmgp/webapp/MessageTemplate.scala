package fmgp.webapp

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.extension._
import fmgp.did.comm.protocol.basicmessage2._
import fmgp.did.comm.protocol.routing2._
import fmgp.did.comm.protocol.trustping2._
import fmgp.did.comm.protocol.reportproblem2._
import fmgp.did.comm.protocol.discoverfeatures2._
import fmgp.util.Base64
import fmgp.Config
import fmgp.NotificationsSubscription

object MessageTemplate {
  def mFrom: Option[FROM] = Global.agentVar.now().flatMap(o => FROM.either(o.id.string).toOption)
  def from: FROM = mFrom.getOrElse(DidExample.senderDIDDocument.id.toDID)
  def to: TO = Global.recipientVar.now().getOrElse(DidExample.recipientDIDDocument.id.toDID)
  def anotherDid: FROMTO = DidExample.senderDIDDocument.id.toDID
  def thid: MsgID = MsgID("thid-responding-to-msg-id")
  def thidMaybe: MsgID = MsgID("maybe-thid-if-responding")

  def exPlaintextMessage = PlaintextMessageClass(
    id = MsgID(),
    `type` = PIURI("basic"),
    to = Some(Set(to)),
    from = Some(from),
    thid = Some(thid),
    created_time = Some(123456789),
    expires_time = Some(123456789),
    body = Some(JSON_RFC7159()),
    attachments = Some(Seq.empty[Attachment]),
    // # Extensions
    return_route = Some(ReturnRoute.all),
    `accept-lang` = Some(Seq("PT")),
    lang = Some("PT"), // IANA’s language codes  // IANA’s language subtag registry.
    // l10n = Some(L10n(
    //   inline = Some(Seq[L10nInline),
    //   service = Some(L10nService),
    //   table = Some(L10nTable)
    //   )),
    // sender_order: NotRequired[SenderOrder] = None,
    // sent_count: NotRequired[SentCount] = None,
    // received_orders: NotRequired[Seq[ReceivedOrdersElement]] = None,
  )

  def exForwardMessageJson = ForwardMessageJson(
    to = Set(to),
    next = to.toDID,
    from = None,
    expires_time = Some(987654321),
    msg = obj_encryptedMessage_ECDHES_X25519_XC20P,
  )
  def exForwardMessageBase64 = ForwardMessageBase64(
    to = Set(to),
    next = to.toDID,
    from = None,
    expires_time = Some(987654321),
    msg = obj_encryptedMessage_ECDHES_X25519_XC20P,
  )

  def exTrustPing = TrustPingWithRequestedResponse(from = from, to = to)
  def exTrustPingResponse = TrustPingResponse(thid = MsgID("some_thid_123"), from = mFrom, to = to)
  def exBasicMessage = BasicMessage(from = mFrom, to = Set(to), content = "Hello, World!")

  object Mediatorcoordination2 {
    import fmgp.did.comm.protocol.mediatorcoordination2._

    def exMediateRequest2 = MediateRequest(from = from, to = to)
    def exMediateGrant2 = MediateGrant(from = from, to = to, thid = thid, routing_did = from.asFROMTO)
    def exMediateDeny2 = MediateDeny(from = from, to = to, thid = thid)
    def exKeylistUpdate2 = KeylistUpdate(from = from, to = to, updates = Seq(anotherDid -> KeylistAction.add))
    def exKeylistResponse2 = KeylistResponse(
      from = from,
      to = to,
      thid = thid,
      updated = Seq((anotherDid, KeylistAction.add, KeylistResult.success))
    )
    def exKeylistQuery2 = KeylistQuery(
      from = from,
      to = to,
      paginate = Some(KeylistQuery.Paginate(limit = 10, offset = 0)),
    )
    def exKeylist2 = Keylist(
      thid = thid,
      from = from,
      to = to,
      keys = Seq(Keylist.RecipientDID(to.asFROMTO)),
      pagination = Some(Keylist.Pagination(count = 10, offset = 0, remaining = 40)),
    )
  }

  object Mediatorcoordination3 {
    import fmgp.did.comm.protocol.mediatorcoordination3._

    def exMediateRequest3 = MediateRequest(from = from, to = to)
    def exMediateGrant3 = MediateGrant(from = from, to = to, thid = thid, routing_did = Seq(from.asFROMTO))
    def exMediateDeny3 = MediateDeny(from = from, to = to, thid = thid)
    def exRecipientUpdate3 = RecipientUpdate(from = from, to = to, updates = Seq(anotherDid -> RecipientAction.add))
    def exRecipientResponse3 = RecipientResponse(
      from = from,
      to = to,
      thid = thid,
      updated = Seq((anotherDid, RecipientAction.add, RecipientResult.success))
    )
    def exRecipientQuery3 = RecipientQuery(
      from = from,
      to = to,
      paginate = Some(RecipientQuery.Paginate(limit = 10, offset = 0)),
    )
    def exRecipient3 = Recipient(
      thid = thid,
      from = from,
      to = to,
      dids = Seq(Recipient.RecipientDID(to.asFROMTO)),
      pagination = Some(Recipient.Pagination(count = 10, offset = 0, remaining = 40)),
    )
  }

  object Pickup3 {
    import fmgp.did.comm.protocol.pickup3._

    def exStatusRequest = StatusRequest(from = from, to = to, recipient_did = Some(FROMTO("did:recipient_did:123")))
    def exStatus = Status(
      from = from,
      to = to,
      thid = thid,
      recipient_did = Some(FROMTO("did:recipient_did:123")),
      message_count = 5,
      longest_waited_seconds = Some(3600),
      newest_received_time = Some(1658085169),
      oldest_received_time = Some(1658084293),
      total_bytes = Some(8096),
      live_delivery = Some(false),
    )
    def exDeliveryRequest =
      DeliveryRequest(from = from, to = to, limit = 5, recipient_did = Some(FROMTO("did:recipient_did:123")))
    def exMessageDelivery = MessageDelivery(
      from = from,
      to = to,
      thid = thid,
      recipient_did = Some(FROMTO("did:recipient_did:123")),
      attachments = Map("321" -> obj_encryptedMessage_ECDHES_X25519_XC20P)
    )
    def exMessagesReceived =
      MessagesReceived(from = from, to = to, thid = Some(thidMaybe), message_id_list = Seq("321"))
    def exLiveModeChange = LiveModeChange(from = from, to = to, live_delivery = true)
  }

  object DiscoverFeatures2 {
    def exFeatureQuery = FeatureQuery(
      from = from,
      to = Set(to),
      queries = Seq(
        FeatureQuery.Query(
          `feature-type` = "protocol",
          `match` = "https://didcomm.org/tictactoe/1.*",
        ),
        FeatureQuery.Query(
          `feature-type` = "goal-code",
          `match` = "org.didcomm.*",
        ),
      ),
    )

    def exFeatureDisclose = FeatureDisclose(
      from = from,
      to = Set(to),
      thid = Some(MsgID()),
      disclosures = Seq(
        FeatureDisclose.Disclose(
          `feature-type` = "protocol",
          id = "https://didcomm.org/tictactoe/1.0",
          roles = Some(Seq("player"))
        ),
        FeatureDisclose.Disclose(
          `feature-type` = "goal-code",
          id = "org.didcomm.sell.goods.consumer",
          roles = None
        ),
      ),
    )
  }

  object ReportProblem2 {
    def exProblemReport = ProblemReport(
      from = from,
      to = Set(to),
      pthid = MsgID(),
      ack = Some(Seq(MsgID())),
      code = ProblemCode.ErroFail("any"),
      comment = Some("Some problem happen {1}"),
      args = Some(Seq(".")),
      escalate_to = Some("fabiomgpinheiro@gmail.com"),
    )
  }

  import fmgp.util._
  import fmgp.crypto._

  val obj_encryptedMessage_ECDHES_X25519_XC20P = EncryptedMessageGeneric(
    ciphertext = CipherText(
      "KWS7gJU7TbyJlcT9dPkCw-ohNigGaHSukR9MUqFM0THbCTCNkY-g5tahBFyszlKIKXs7qOtqzYyWbPou2q77XlAeYs93IhF6NvaIjyNqYklvj-OtJt9W2Pj5CLOMdsR0C30wchGoXd6wEQZY4ttbzpxYznqPmJ0b9KW6ZP-l4_DSRYe9B-1oSWMNmqMPwluKbtguC-riy356Xbu2C9ShfWmpmjz1HyJWQhZfczuwkWWlE63g26FMskIZZd_jGpEhPFHKUXCFwbuiw_Iy3R0BIzmXXdK_w7PZMMPbaxssl2UeJmLQgCAP8j8TukxV96EKa6rGgULvlo7qibjJqsS5j03bnbxkuxwbfyu3OxwgVzFWlyHbUH6p"
    ),
    // {"epk":{"kty":"OKP","crv":"X25519","x":"JHjsmIRZAaB0zRG_wNXLV2rPggF00hdHbW5rj8g0I24"},"apv":"NcsuAnrRfPK69A-rkZ0L9XWUG4jMvNC3Zg74BPz53PA","typ":"application/didcomm-encrypted+json","enc":"XC20P","alg":"ECDH-ES+A256KW"}
    `protected` = Base64Obj[ProtectedHeader](
      AnonProtectedHeader(
        epk = OKPPublicKey(
          kty = KTY.OKP,
          crv = Curve.X25519,
          x = "JHjsmIRZAaB0zRG_wNXLV2rPggF00hdHbW5rj8g0I24",
          kid = None
        ),
        apv = APV("NcsuAnrRfPK69A-rkZ0L9XWUG4jMvNC3Zg74BPz53PA"),
        typ = Some(MediaTypes.ENCRYPTED),
        enc = ENCAlgorithm.XC20P,
        alg = KWAlgorithm.`ECDH-ES+A256KW`,
      ),
      Some(
        Base64(
          "eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6IkpIanNtSVJaQWFCMHpSR193TlhMVjJyUGdnRjAwaGRIYlc1cmo4ZzBJMjQifSwiYXB2IjoiTmNzdUFuclJmUEs2OUEtcmtaMEw5WFdVRzRqTXZOQzNaZzc0QlB6NTNQQSIsInR5cCI6ImFwcGxpY2F0aW9uL2RpZGNvbW0tZW5jcnlwdGVkK2pzb24iLCJlbmMiOiJYQzIwUCIsImFsZyI6IkVDREgtRVMrQTI1NktXIn0"
        )
      )
    ),
    recipients = Seq(
      Recipient(
        encrypted_key = Base64("3n1olyBR3nY7ZGAprOx-b7wYAKza6cvOYjNwVg3miTnbLwPP_FmE1A"),
        header = RecipientHeader(VerificationMethodReferenced("did:example:bob#key-x25519-1"))
      ),
      Recipient(
        encrypted_key = Base64("j5eSzn3kCrIkhQAWPnEwrFPMW6hG0zF_y37gUvvc5gvlzsuNX4hXrQ"),
        header = RecipientHeader(VerificationMethodReferenced("did:example:bob#key-x25519-2"))
      ),
      Recipient(
        encrypted_key = Base64("TEWlqlq-ao7Lbynf0oZYhxs7ZB39SUWBCK4qjqQqfeItfwmNyDm73A"),
        header = RecipientHeader(VerificationMethodReferenced("did:example:bob#key-x25519-3"))
      ),
    ),
    tag = TAG("6ylC_iAs4JvDQzXeY6MuYQ"),
    iv = IV("ESpmcyGiZpRjc5urDela21TOOTW8Wqd1")
  )

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

  object PubSub {
    import fmgp.did.comm.protocol.pubsub.*

    def exRequestToSubscribe = RequestToSubscribe(
      from = from,
      to = Set(to),
      pthid = None,
      created_time = None,
      body = RequestToSubscribe.Body()
    )

    def exSetupToSubscribe = SetupToSubscribe(
      from = from,
      to = Set(to),
      thid = Some(MsgID()),
      pthid = None,
      created_time = None,
      body = SetupToSubscribe.Body(publicKey = Config.PushNotifications.applicationServerKey)
    )

    def exSubscribe = Subscribe(
      from = from,
      to = Set(to),
      thid = Some(MsgID()),
      pthid = None,
      created_time = None,
      body = Subscribe.Body(
        endpoint = "endpoint",
        keyP256DH = "keyP256DH",
        keyAUTH = "keyAUTH",
        id = Some("exSubscribeHardCode")
      )
    )

    def exSubscribe(ns: NotificationsSubscription) = Subscribe(
      from = from,
      to = Set(to),
      thid = Some(MsgID()),
      pthid = None,
      created_time = None,
      body = Subscribe.Body(
        endpoint = ns.endpoint,
        keyP256DH = ns.keys.p256dh,
        keyAUTH = ns.keys.auth,
        id = Some("exSubscribe")
      )
    )

    def exSubscription = Subscription(
      from = from,
      to = Set(to),
      thid = MsgID(),
      pthid = None,
      created_time = None,
      body = Subscription.Body()
    )

  }

  object ProveControl {
    import fmgp.did.comm.protocol.provecontrol.*

    def exRequestVerification = RequestVerification(
      from = from,
      to = to,
      verificationType = VerificationType.Email,
      subject = "fabio@fmgp.app",
    )

    def exVerificationChallenge = VerificationChallenge(
      from = from,
      to = to,
      thid = Some(MsgID()),
      verificationType = VerificationType.Email,
      subject = "fabio@fmgp.app",
      secret = "SecureRandomNumber",
    )

    def exProve = Prove(
      to = to,
      from = from,
      thid = MsgID(),
      verificationType = VerificationType.Email,
      subject = "fabio@fmgp.app",
      proof = VerificationChallenge.calculateProof(
        verifier = from.toDID,
        user = to.toDID,
        verificationType = VerificationType.Email,
        subject = "fabio@fmgp.app",
        secret = "SecureRandomNumber",
      )
    )

    def exConfirmVerification = ConfirmVerification(
      to = to,
      from = from,
      thid = MsgID(),
      verificationType = VerificationType.Email,
      subject = "fabio@fmgp.app",
      attachments = Seq(),
    )
  }

  object ChatriqubeRegistry {
    import fmgp.did.comm.protocol.chatriqube.SubjectType
    import fmgp.did.comm.protocol.chatriqube.registry.*

    def exEnroll = Enroll(
      from = from,
      to = to,
    )
    def exAccount = Account(
      thid = MsgID(),
      from = from,
      to = to,
      ids = Seq((SubjectType.Email, "test@fmgp.app"))
    )
    def exSetId = SetId(
      from = from,
      to = to,
      subjectType = SubjectType.Email,
      subject = "test@fmgp.app",
      proof = "TODO"
    )
  }

  object ChatriqubeDiscovery {
    import fmgp.did.comm.protocol.chatriqube.SubjectType
    import fmgp.did.comm.protocol.chatriqube.discovery.*

    def exAskIntroduction = AskIntroduction(
      from = from,
      to = to,
      request = exampleSignatureEdDSA_obj,
    )

    def exIntroductionStatus = IntroductionStatus(
      thid = MsgID(),
      from = from,
      to = to,
      forwardRequestSent = true,
    )

    def exForwardRequest = ForwardRequest(
      thid = MsgID(),
      from = from,
      to = to,
      request = exampleSignatureEdDSA_obj,
    )

    def exRequest = Request(
      from = from,
      to = None, // Option
      subjectType = SubjectType.Email,
      subject = "test@fmgp.app",
    )
    def exAnswer = Answer(
      thid = MsgID(),
      pthid = Some(MsgID()),
      from = from,
      to = to,
      subjectType = SubjectType.Email,
      subject = "test@fmgp.app",
    )
    def exHandshake = Handshake(
      thid = MsgID(),
      from = from,
      to = to,
    )
  }
}
