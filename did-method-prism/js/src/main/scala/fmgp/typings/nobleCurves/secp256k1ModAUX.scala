package fmgp.typings.nobleCurves

import fmgp.typings.nobleCurves.abstractHashToCurveMod.HTFMethod
import fmgp.typings.nobleCurves.esmUtilsMod.Hex
import fmgp.typings.nobleCurves.esmUtilsMod.PrivKey
import fmgp.typings.nobleCurves.abstractWeierstrassMod.ProjPointType
import fmgp.typings.nobleCurves.abstractWeierstrassMod.SignatureType
import fmgp.typings.nobleCurves.anon.BytesToNumberBE
import fmgp.typings.nobleCurves.shortwUtilsMod.CurveFnWithCreate
import org.scalablytyped.runtime.Shortcut
import org.scalablytyped.runtime.StObject
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobalScope, JSGlobal, JSImport, JSName, JSBracketAccess}

object secp256k1ModAUX {

  @JSImport("@noble/curves/secp256k1", "encodeToCurve")
  @js.native
  val encodeToCurve: HTFMethod[js.BigInt] = js.native

  @JSImport("@noble/curves/secp256k1", "hashToCurve")
  @js.native
  val hashToCurve: HTFMethod[js.BigInt] = js.native

  @JSImport("@noble/curves/secp256k1", "schnorr")
  @js.native
  val schnorr: SecpSchnorr = js.native

  object secp256k1 extends Shortcut {

    @JSImport("@noble/curves/secp256k1", "secp256k1")
    @js.native
    val ^ : CurveFnWithCreate = js.native

    /* This class was inferred from a value with a constructor. In rare cases (like HTMLElement in the DOM) it might not work as you expect. */
    @deprecated("Interface is no longer nessesaty", "@noble/curves@1.9.1") // in the update from 1.8.2 to 1.9.2
    @JSImport("@noble/curves/secp256k1", "secp256k1.ProjectivePoint")
    @js.native
    open class ProjectivePoint protected () extends StObject with ProjPointType[js.BigInt] {
      def this(x: js.BigInt, y: js.BigInt, z: js.BigInt) = this()

      // /* CompleteClass */
      // override def add(other: ProjPointType[js.BigInt]): ProjPointType[js.BigInt] = js.native

      // /* CompleteClass */
      // override def double(): ProjPointType[js.BigInt] = js.native

      // /* CompleteClass */
      // override def multiply(scalar: js.BigInt): ProjPointType[js.BigInt] = js.native

      // /* CompleteClass */
      // override def negate(): ProjPointType[js.BigInt] = js.native

      // /* CompleteClass */
      // override def subtract(other: ProjPointType[js.BigInt]): ProjPointType[js.BigInt] = js.native
    }

    // / * This class was inferred from a value with a constructor. In rare cases (like HTMLElement in the DOM) it might not work as you expect. * /
    // @JSImport("@noble/curves/secp256k1", "secp256k1.Signature")
    // @js.native
    // open class Signature protected () extends StObject with SignatureType {
    //   def this(r: js.BigInt, s: js.BigInt) = this()
    // }

    type _To = CurveFnWithCreate

    /* This means you don't have to write `^`, but can instead just say `secp256k1.foo` */
    override def _to: CurveFnWithCreate = ^
  }

  trait SecpSchnorr extends StObject {

    def getPublicKey(privateKey: Hex): js.typedarray.Uint8Array
    @JSName("getPublicKey")
    var getPublicKey_Original: js.Function1[ /* privateKey */ Hex, js.typedarray.Uint8Array]

    def sign(message: Hex, privateKey: PrivKey): js.typedarray.Uint8Array
    def sign(message: Hex, privateKey: PrivKey, auxRand: Hex): js.typedarray.Uint8Array
    @JSName("sign")
    var sign_Original: js.Function3[
      /* message */ Hex,
      /* privateKey */ PrivKey,
      /* auxRand */ js.UndefOr[Hex],
      js.typedarray.Uint8Array
    ]

    var utils: BytesToNumberBE

    def verify(signature: Hex, message: Hex, publicKey: Hex): Boolean
    @JSName("verify")
    var verify_Original: js.Function3[ /* signature */ Hex, /* message */ Hex, /* publicKey */ Hex, Boolean]
  }
  object SecpSchnorr {

    inline def apply(
        getPublicKey: /* privateKey */ Hex => js.typedarray.Uint8Array,
        sign: ( /* message */ Hex, /* privateKey */ PrivKey, /* auxRand */ js.UndefOr[Hex]) => js.typedarray.Uint8Array,
        utils: BytesToNumberBE,
        verify: ( /* signature */ Hex, /* message */ Hex, /* publicKey */ Hex) => Boolean
    ): SecpSchnorr = {
      val __obj = js.Dynamic.literal(
        getPublicKey = js.Any.fromFunction1(getPublicKey),
        sign = js.Any.fromFunction3(sign),
        utils = utils.asInstanceOf[js.Any],
        verify = js.Any.fromFunction3(verify)
      )
      __obj.asInstanceOf[SecpSchnorr]
    }

    @scala.inline
    implicit open class MutableBuilder[Self <: SecpSchnorr](val x: Self) extends AnyVal {

      inline def setGetPublicKey(value: /* privateKey */ Hex => js.typedarray.Uint8Array): Self =
        StObject.set(x, "getPublicKey", js.Any.fromFunction1(value))

      inline def setSign(
          value: (
              /* message */ Hex, /* privateKey */ PrivKey, /* auxRand */ js.UndefOr[Hex]
          ) => js.typedarray.Uint8Array
      ): Self = StObject.set(x, "sign", js.Any.fromFunction3(value))

      inline def setUtils(value: BytesToNumberBE): Self = StObject.set(x, "utils", value.asInstanceOf[js.Any])

      inline def setVerify(value: ( /* signature */ Hex, /* message */ Hex, /* publicKey */ Hex) => Boolean): Self =
        StObject.set(x, "verify", js.Any.fromFunction3(value))
    }
  }
}
