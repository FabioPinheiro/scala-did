package fmgp.did

import zio._
import fmgp.crypto._

/** Agent have is a DID
  *   - has keys
  *   - can have encryption preferences
  *   - can have resolver preferences
  */
trait Agent { // Rename to Identity
  def id: DID
  def keyStore: KeyStore

  // def didDocument: DIDDocument
  // // Extra Methods
  // def getPrivateKeysTypeAuthentication: Set[PrivateKey] = {
  //   val allKeysTypeAuthentication = didDocument.allKeysTypeAuthentication
  //   keyStore.keys.filter(k =>
  //     k.kid match
  //       case None      => false // no kid ...
  //       case Some(kid) => allKeysTypeAuthentication.map(_.id).contains(kid)
  //   )
  // }
}
