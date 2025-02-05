package fmgp.prism

trait PrismBlockIndex {
  def tx: String

  /** Index relative to the Cardano Trasation with PRISM_LABEL */
  def b: Int

  /** Index relative to the Cardano Trasation with PRISM_LABEL */
  def prismBlockIndex: Int = b
}

trait PrismOperationIndex extends PrismBlockIndex {

  /** Index relative to the PrismBlock */
  def o: Int

  /** Index relative to the PrismBlock */
  def prismOperationIndex: Int = o
}
