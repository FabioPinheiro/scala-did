package fmgp.crypto

import zio.json.*
import scalus.cardano.wallet.hd.HdKeyPair
import scalus.cardano.wallet.hd.Bip32Ed25519
import scalus.crypto.ed25519.given
import fmgp.util.*
import fmgp.did.method.prism.cardano.Cip0000

sealed trait DerivedKey {
  def derivationPath: String
  def path: Cip0000 = Cip0000.fromPath(derivationPath).getOrElse(???) // FIXME
}
case class KeySecp256k1(derivationPath: String, key: Secp256k1PrivateKey) extends DerivedKey {
  def secp256k1PrivateKey = key
}
case class KeyEd25519(derivationPath: String, key: HdKeyPair) extends DerivedKey
case class KeyX25519(derivationPath: String, key: OKPPrivateKeyWithoutKid) extends DerivedKey

object DerivedKey {

  // TODO decoderArrayByte and encoderArrayByte move to another place
  def decoderArrayByte: JsonDecoder[Array[Byte]] = JsonDecoder.string.map(hex => hex2bytes(hex))
  def encoderArrayByte: JsonEncoder[Array[Byte]] = JsonEncoder.string.contramap(bytes => bytes2Hex(bytes))

  def secp256k1FromRaw(hex: String) = Secp256k1PrivateKey(hex2bytes(hex))

  given decoder: JsonDecoder[DerivedKey] = {
    given JsonDecoder[Array[Byte]] = decoderArrayByte
    given JsonDecoder[Secp256k1PrivateKey] = JsonDecoder.string.map(hex => secp256k1FromRaw(hex))
    given extendedKeyDecoder: JsonDecoder[Bip32Ed25519.ExtendedKey] = DeriveJsonDecoder.gen[Bip32Ed25519.ExtendedKey]
    given JsonDecoder[HdKeyPair] = extendedKeyDecoder.map(ek => HdKeyPair.fromExtendedKey(ek))
    DeriveJsonDecoder.gen[DerivedKey]
  }
  given encoder: JsonEncoder[DerivedKey] = {
    given JsonEncoder[Array[Byte]] = encoderArrayByte
    given JsonEncoder[Secp256k1PrivateKey] = JsonEncoder.string.contramap(key => bytes2Hex(key.rawBytes))
    given extendedKeyEncoder: JsonEncoder[Bip32Ed25519.ExtendedKey] = DeriveJsonEncoder.gen[Bip32Ed25519.ExtendedKey]
    given JsonEncoder[HdKeyPair] = extendedKeyEncoder.contramap(hd => hd.extendedKey)
    DeriveJsonEncoder.gen[DerivedKey]
  }

  def apply(derivationPath: String, key: Secp256k1PrivateKey): KeySecp256k1 = KeySecp256k1(derivationPath, key)
  def apply(derivationPath: String, key: HdKeyPair): KeyEd25519 = KeyEd25519(derivationPath, key)
}
