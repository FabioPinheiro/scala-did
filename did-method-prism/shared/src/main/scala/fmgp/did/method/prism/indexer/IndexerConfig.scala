package fmgp.did.method.prism.indexer

import fmgp.did.DIDSubject

/** @param apiKey
  *   blockfrost API key
  */
case class IndexerConfig(apiKey: Option[String], workdir: String, network: String) {
  def rawMetadataPath = s"$workdir/cardano-21325"

  def opsPath(did: DIDSubject) = s"$workdir/ops/${did.string}"
  def ssiPath(did: DIDSubject) = s"$workdir/ssi/${did.string}"
  def diddocPath(did: DIDSubject) = s"$workdir/diddoc/${did.string}"
}
