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
}
