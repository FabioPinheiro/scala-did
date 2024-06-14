package fmgp.did.method.web

import zio._
import zio.json._
import fmgp.did._
import fmgp.did.comm.FROMTO
import fmgp.crypto._

/** DID web
  *
  * @see
  *   https://w3c-ccg.github.io/did-method-web/
  */
case class DIDWeb(specificId: String) extends DID {
  final def namespace: String = DIDWeb.namespace

  def domainName: String = specificId.split(":").head // NOTE split always have head

  def paths: Array[String] = specificId.split(":").drop(1)

  def url = "https://" + domainName.replace("%3A", ":") + "/" + {
    if (paths.isEmpty) ".well-known" else paths.mkString("/")
  } + "/did.json"

}

object DIDWeb {
  def namespace: String = "web"
  def applyUnsafe(did: String): DIDWeb = DIDWeb(DIDSubject(did).specificId)
}
