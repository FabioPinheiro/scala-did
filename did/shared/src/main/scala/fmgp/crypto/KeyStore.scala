package fmgp.crypto

import zio.json._

type Keys = Set[PrivateKey]
case class KeyStore(keys: Set[PrivateKeyWithKid])

object KeyStore {
  given decoder: JsonDecoder[KeyStore] = // DeriveJsonDecoder.gen[Keys]
    JsonDecoder.set[PrivateKeyWithKid].map(keys => KeyStore(keys))
  given encoder: JsonEncoder[KeyStore] = // DeriveJsonEncoder.gen[Keys]
    JsonEncoder.set[PrivateKeyWithKid].contramap(ks => ks.keys)
}
