package fmgp.did.method.prism.cardano

sealed trait CardanoNetwork:
  def name: String
  def blockfrostURL: String

enum PublicCardanoNetwork extends CardanoNetwork:
  // "Cardano testnet network has been decommissioned."
  case Mainnet, Testnet, Preprod, Preview

  override def name = this.toString.toLowerCase

  /** https://blockfrost.dev/docs/start-building#available-networks */
  override def blockfrostURL = {
    this match
      case Mainnet => "https://cardano-mainnet.blockfrost.io/api/v0"
      case Testnet => "https://cardano-testnet.blockfrost.io/api/v0"
      case Preprod => "https://cardano-preprod.blockfrost.io/api/v0"
      case Preview => "https://cardano-preview.blockfrost.io/api/v0"
      // val IPFS = "https://ipfs.blockfrost.io/api/v0"
      // val milkomedaMainnet = "https://milkomeda-mainnet.blockfrost.io/api/v0"
      // val milkomedaMTestnet = "https://milkomeda-testnet.blockfrost.io/api/v0"
  }

object PublicCardanoNetwork {
  val TESTNET_NOT_AVAILABLE = "Testnet is no longer available"
  def fromBlockfrostToken(token: String) = token.slice(0, 7) match {
    case "mainnet" => PublicCardanoNetwork.Mainnet
    case "testnet" => PublicCardanoNetwork.Testnet
    case "preprod" => PublicCardanoNetwork.Preprod
    case "preview" => PublicCardanoNetwork.Preview
  }
}

final case class PrivateCardanoNetwork(override val blockfrostURL: String, protocolMagic: Long) extends CardanoNetwork:
  override def name: String = "private"
