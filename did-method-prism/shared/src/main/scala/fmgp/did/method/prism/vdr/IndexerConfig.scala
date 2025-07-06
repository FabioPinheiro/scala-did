package fmgp.did.method.prism.vdr

import fmgp.did.DIDSubject
import fmgp.did.method.prism.RefVDR

/** @param mBlockfrostConfig
  *   will contain the blockfrost API key
  */
case class IndexerConfig(mBlockfrostConfig: Option[BlockfrostConfig], workdir: String) {
  def rawMetadataPath = s"$workdir/cardano-21325"

  def ssiEventsPath(did: DIDSubject) = s"$workdir/events/${did.specificId}"
  def vdrEventsPath(ref: RefVDR) = s"$workdir/events/${ref.value}"

  def ssiPath(did: DIDSubject) = s"$workdir/ssi/${did.specificId}"
  def vdrPath(ref: RefVDR) = s"$workdir/vdr/${ref.value}"

  def diddocPath(did: DIDSubject) = s"$workdir/diddoc/${did.string}"
}
