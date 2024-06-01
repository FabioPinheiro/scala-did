package fmgp.crypto

import java.security.*
import org.bouncycastle.jce.provider.BouncyCastleProvider

object CryptoProvider {
  // Add the BouncyCastleProvider to the security provider list
  println("Security.addProvider BouncyCastleProvider") // TODO REMOVE
  Security.addProvider(new BouncyCastleProvider()) // I believe this is not executed

  // jcaContext: JWEJCAContext = new JWEJCAContext()

  inline def provider = BouncyCastleProvider()

  inline def secureRandom = SecureRandom()

  inline def keyEncryptionProvider: Provider = null // provider

}
