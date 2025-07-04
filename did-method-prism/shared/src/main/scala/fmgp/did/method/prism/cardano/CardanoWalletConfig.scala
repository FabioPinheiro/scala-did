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
