package fmgp.did.method.prism.cardano

/** https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md
  *
  * The value 21325 represents the last 16 bits of 344977920845, which is the decimal representation of the
  * concatenation of the hexadecimals 50 52 49 53 4d that form the word PRISM in ASCII.
  */
val PRISM_LABEL_CIP_10 = 21325 // 21324 was used in testnet to do some tests

// FIXME!!!!

/** https://cips.cardano.org/cip/CIP-20
  */
val MSG_LABEL_CIP_20 = 674

/** @see
  *   https://developers.cardano.org/docs/get-started/cardano-serialization-lib/transaction-metadata/#metadata-limitations
  */
inline val cborByteStringMaxMatadataSize = 32
