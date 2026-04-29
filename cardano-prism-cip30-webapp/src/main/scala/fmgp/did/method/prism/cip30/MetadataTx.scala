package fmgp.did.method.prism.cip30

import io.bullet.borer.{Cbor as BorerCbor, Decoder, Reader}
import scalus.cardano.address.Address
import scalus.cardano.ledger.{
  AuxiliaryData,
  AuxiliaryDataHash,
  Coin,
  KeepRaw,
  Metadatum,
  OriginalCborByteArray,
  ProtocolVersion,
  Sized,
  TaggedSortedSet,
  Transaction,
  TransactionBody,
  TransactionInput,
  TransactionOutput,
  TransactionWitnessSet,
  Utxo,
  Value,
  Word64,
}
import scalus.uplc.builtin.{ByteString, platform}

import _root_.proto.prism.PrismBlock
import _root_.proto.prism.PrismObject
import _root_.proto.prism.SignedPrismEvent
import fmgp.did.method.prism.cardano.PRISM_LABEL_CIP_10
import fmgp.did.method.prism.cardano.cborByteStringMaxMatadataSize

/** Build a pay-yourself Cardano transaction whose CIP-10 metadata carries the
  * given PRISM events. Pure functions only -- no `js.Dynamic`, no I/O. The
  * browser caller wires the inputs from CIP-30 (`getUtxos`, `getChangeAddress`)
  * and ships the resulting CBOR to `signTx` / `submitTx`.
  *
  * The body bytes signed by the wallet round-trip unchanged through Scalus's
  * `KeepRaw[TransactionBody]`; re-emitting after merging the witness set
  * yields a tx whose body hash matches what the wallet actually signed.
  */
object MetadataTx:

  /** CIP-30 wallet snapshot, hex-decoded. `utxosCbor` are the raw bytes of
    * each `[input, output]` array as returned by `api.getUtxos()`.
    */
  final case class WalletSnapshot(
      networkId: Int,
      utxosCbor: List[Array[Byte]],
      changeAddressBytes: Array[Byte],
  )

  /** Conservative fee for a small self-send tx with PRISM metadata. */
  val FixedFeeLovelace: Long = 300_000L

  /** Refuse if the picked UTxO is below this threshold (covers fee + min-utxo
    * value floor).
    */
  val MinSpendableLovelace: Long = 1_500_000L

  def buildPrismTx(
      snap: WalletSnapshot,
      events: Seq[SignedPrismEvent],
  ): Either[String, Transaction] =
    if snap.utxosCbor.isEmpty then Left("Wallet has no UTxOs.")
    else if events.isEmpty then Left("No PRISM events to submit.")
    else
      for
        changeAddr <- parseAddress(snap.changeAddressBytes)
        utxos      <- decodeUtxos(snap.utxosCbor)
        chosen     <- pickLargestAdaUtxo(utxos)
        body        = buildBody(chosen, changeAddr)
        aux         = prismMetadata(events)
        bodyHashed  = body.copy(auxiliaryDataHash = Some(auxDataHash(aux)))
      yield Transaction(bodyHashed, TransactionWitnessSet.empty, aux)

  /** Splice the wallet's witness-set bytes into the unsigned tx so the result
    * is ready for `submitTx`. The body bytes round-trip unchanged.
    */
  def attachWitnesses(
      unsignedTx: Transaction,
      witnessSetCbor: Array[Byte],
  ): Transaction =
    given OriginalCborByteArray = OriginalCborByteArray(witnessSetCbor)
    given ProtocolVersion       = ProtocolVersion.conwayPV
    val ws = BorerCbor.decode(witnessSetCbor).to[TransactionWitnessSet].value
    unsignedTx.copy(witnessSetRaw = KeepRaw(merge(unsignedTx.witnessSet, ws)))

  // ---- internals ---------------------------------------------------------

  private def parseAddress(bytes: Array[Byte]): Either[String, Address] =
    try Right(Address.fromBytes(bytes))
    catch case t: Throwable => Left(s"Invalid change address: ${t.getMessage}")

  private def decodeUtxos(rawList: List[Array[Byte]]): Either[String, List[Utxo]] =
    val builder = List.newBuilder[Utxo]
    var failure: Option[String] = None
    val it = rawList.iterator
    while it.hasNext && failure.isEmpty do
      val raw = it.next()
      try builder += decodeOneUtxo(raw)
      catch case t: Throwable => failure = Some(s"UTxO decode failed: ${t.getMessage}")
    failure match
      case Some(msg) => Left(msg)
      case None      => Right(builder.result())

  private def decodeOneUtxo(bytes: Array[Byte]): Utxo =
    given OriginalCborByteArray = OriginalCborByteArray(bytes)
    given ProtocolVersion       = ProtocolVersion.conwayPV
    val decoder: Decoder[Utxo] = Decoder { (r: Reader) =>
      r.readArrayHeader()
      val input  = r.read[TransactionInput]()
      val output = r.read[TransactionOutput]()
      Utxo(input, output)
    }
    BorerCbor.decode(bytes).to[Utxo](using decoder).value

  private def pickLargestAdaUtxo(utxos: List[Utxo]): Either[String, Utxo] =
    val ranked = utxos.sortBy(u => -outputCoin(u.output).value)
    ranked.headOption match
      case None => Left("Wallet has no UTxOs.")
      case Some(best) if outputCoin(best.output).value < MinSpendableLovelace =>
        val have = outputCoin(best.output).value / 1_000_000.0
        val need = MinSpendableLovelace / 1_000_000.0
        Left(f"No UTxO >= $need%.2f ADA (largest is $have%.2f).")
      case Some(best) => Right(best)

  private def outputCoin(o: TransactionOutput): Coin = o match
    case s: TransactionOutput.Shelley => s.value.coin
    case b: TransactionOutput.Babbage => b.value.coin

  private def buildBody(chosen: Utxo, changeAddr: Address): TransactionBody =
    val inputCoin  = outputCoin(chosen.output)
    val changeCoin = Coin(inputCoin.value - FixedFeeLovelace)
    val changeOut  = TransactionOutput.Babbage(changeAddr, Value(changeCoin))
    TransactionBody(
      inputs  = TaggedSortedSet(chosen.input),
      outputs = IndexedSeq(Sized(changeOut)),
      fee     = Coin(FixedFeeLovelace),
    )

  private def prismMetadata(events: Seq[SignedPrismEvent]): AuxiliaryData =
    val prismObject = PrismObject(blockContent = Some(PrismBlock(events = events)))
    val cborChunks: Seq[Metadatum] =
      prismObject.toByteArray
        .grouped(cborByteStringMaxMatadataSize)
        .toIndexedSeq
        .map(b => Metadatum.Bytes(scalus.uplc.builtin.ByteString.fromArray(b)))
    val prismMap: Metadatum = Metadatum.Map(
      Map(
        Metadatum.Text("v") -> Metadatum.Int(1),
        Metadatum.Text("c") -> Metadatum.List(cborChunks.toIndexedSeq),
      )
    )
    val labels: Map[Word64, Metadatum] =
      Map(Word64.fromUnsignedInt(PRISM_LABEL_CIP_10) -> prismMap)
    AuxiliaryData.AlonzoFormat(metadata = Some(labels))

  private def auxDataHash(aux: AuxiliaryData): AuxiliaryDataHash =
    val cbor = BorerCbor.encode(aux).toByteArray
    val hash = platform.blake2b_256(ByteString.unsafeFromArray(cbor))
    AuxiliaryDataHash.fromByteString(hash)

  private def merge(a: TransactionWitnessSet, b: TransactionWitnessSet): TransactionWitnessSet =
    a.copy(
      vkeyWitnesses      = TaggedSortedSet.from(a.vkeyWitnesses.toSet ++ b.vkeyWitnesses.toSet),
      bootstrapWitnesses =
        TaggedSortedSet.from(a.bootstrapWitnesses.toSet ++ b.bootstrapWitnesses.toSet),
    )
