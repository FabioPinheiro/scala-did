package fmgp.did.method.prism.cardano

case class CardanoWalletConfig(
    // TODO REMVOE defualt
    mnemonic: Seq[String] = Seq(
      "mention",
      "side",
      "album",
      "physical",
      "uncle",
      "lab",
      "horn",
      "nasty",
      "script",
      "few",
      "hazard",
      "announce",
      "upon",
      "group",
      "ten",
      "moment",
      "fantasy",
      "helmet",
      "supreme",
      "early",
      "gadget",
      "curve",
      "lecture",
      "edge"
    ),
    passphrase: String = ""
) {
  def mnemonicPhrase = mnemonic.mkString(" ")
}

object CardanoWalletConfig {
  def fromMnemonicPhrase(phrase: String, passphrase: String = "") = phrase.split(" ") match
    case words @ Array(
          w01,
          w02,
          w03,
          w04,
          w05,
          w06,
          w07,
          w08,
          w09,
          w10,
          w11,
          w12,
          w13,
          w14,
          w15,
          w16,
          w17,
          w18,
          w19,
          w20,
          w21,
          w22,
          w23,
          w24
        ) =>
      // Check words https://github.com/cardano-foundation/cardano-wallet/blob/master/specifications/mnemonic/english.txt
      if (words.exists(w => MnemonicEnglish.words.contains(w)))
        // TODO check Checksums https://cips.cardano.org/cip/CIP-4
        Right(CardanoWalletConfig(words.toSeq, passphrase))
      else
        Left(
          "MnemonicPhrase MUST online contains words from 'https://github.com/cardano-foundation/cardano-wallet/blob/master/specifications/mnemonic/english.txt'"
        )
    case _ => Left("MnemonicPhrase MSU have 24 words")

}
