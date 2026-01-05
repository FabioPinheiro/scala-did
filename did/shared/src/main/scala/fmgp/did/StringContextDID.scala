package fmgp.did

import fmgp.crypto.error.DidException

extension (sc: StringContext) {
  def did(args: Any*): DID =
    val didString = sc.s(args*)
    DIDSubject.either(didString) match
      case Left(ex)          => throw new DidException(ex)
      case Right(didSubject) => didSubject
}
