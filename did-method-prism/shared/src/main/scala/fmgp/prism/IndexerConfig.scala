package fmgp.prism

/** @param apiKey
  *   blockfrost API key
  */
case class IndexerConfig(apiKey: Option[String], workdir: String, network: String) {
  def rawMetadataPath = s"$workdir/cardano-21325"
  def eventsPath = s"$workdir/prism-events"
  // def statePath = s"$workdir/prism-state.json"

  def opidPath(did: String) = s"$workdir/opid/$did"
  def opsPath(did: String) = s"$workdir/ops/$did"
  def ssiPath(did: String) = s"$workdir/ssi/$did"
  def diddocPath(did: String) = s"$workdir/diddoc/$did"
}
