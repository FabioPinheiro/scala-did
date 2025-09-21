package fmgp.did.method.prism.cli

case class PrismCliError(fail: String) extends Exception(fail)
