package fmgp.did.method.prism.cardano

enum CardanoNetwork:
  // "Cardano testnet network has been decommissioned."
  case Mainnet, Testnet, Preprod, Preview

  def name = this.toString.toLowerCase

  /** https://blockfrost.dev/docs/start-building#available-networks */
  def blockfrostURL = {
    this match
      case Mainnet => "https://cardano-mainnet.blockfrost.io/api/v0"
      case Testnet => "https://cardano-testnet.blockfrost.io/api/v0"
      case Preprod => "https://cardano-preprod.blockfrost.io/api/v0"
      case Preview => "https://cardano-preview.blockfrost.io/api/v0"
      // val IPFS = "https://ipfs.blockfrost.io/api/v0"
      // val milkomedaMainnet = "https://milkomeda-mainnet.blockfrost.io/api/v0"
      // val milkomedaMTestnet = "https://milkomeda-testnet.blockfrost.io/api/v0"
  }

object CardanoNetwork {
  def fromBlockfrostToken(token: String) = token.slice(0, 7) match {
    case "mainnet" => CardanoNetwork.Mainnet
    case "testnet" => CardanoNetwork.Testnet
    case "preprod" => CardanoNetwork.Preprod
    case "preview" => CardanoNetwork.Preview
  }
}
