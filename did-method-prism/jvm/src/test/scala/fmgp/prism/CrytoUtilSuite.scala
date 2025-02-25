package fmgp.prism

import munit._
import zio.json._
import fmgp.did.DIDDocument
import fmgp.util.Base64
import fmgp.prism.CrytoUtil

import proto.prism.KeyUsage
import proto.prism.PublicKey.KeyData
import fmgp.prism.PrismPublicKey.VoidKey
import fmgp.prism.PrismPublicKey.UncompressedECKey
import fmgp.prism.PrismPublicKey.CompressedECKey
import fmgp.util.hex2bytes
import java.security.PrivateKey
import java.security.PublicKey
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util
import java.security.spec.ECPrivateKeySpec
import java.security.KeyFactory
import java.security.spec.ECPublicKeySpec

class CrytoUtilSuite extends FunSuite {

  val ecPublicKeyBase64 =
    "BC7OYUnD57Qxel3-gyGuUIeicvRYhkFMOw9vsz70WMHzt8X8jiX358Jv9KYrMOHpkHE6jpb8CTvGabgIJUPkX_4"
  // {"typ":"JWT","alg":"ES256K"}
  val messageBase64 =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJub25jZSI6IjQ3YmM5ZmMwLWVhODAtNDlmOC04OTcxLWJjYzY0MmJmZDNjMCIsImlzcyI6ImRpZDpwcmlzbTphZjJlNGJiOWU1MTRmODg5ZTdkNTY2MDZjNmYzZWVhYmNmMDgxZTc0ZTQ4NDMwN2Q3NTQ4Mzg0Y2ZiOTE4ZTdlOkNzY0JDc1FCRW1RS0QyRjFkR2hsYm5ScFkyRjBhVzl1TUJBRVFrOEtDWE5sWTNBeU5UWnJNUklnTHM1aFNjUG50REY2WGY2RElhNVFoNkp5OUZpR1FVdzdEMi16UHZSWXdmTWFJTGZGX0k0bDktZkNiX1NtS3pEaDZaQnhPbzZXX0FrN3htbTRDQ1ZENUZfLUVsd0tCMjFoYzNSbGNqQVFBVUpQQ2dselpXTndNalUyYXpFU0lDN09ZVW5ENTdReGVsMy1neUd1VUllaWN2Ulloa0ZNT3c5dnN6NzBXTUh6R2lDM3hmeU9KZmZud21fMHBpc3c0ZW1RY1RxT2x2d0pPOFpwdUFnbFEtUmZfZyIsInZwIjp7IkBjb250ZXh0IjpbImh0dHBzOlwvXC93d3cudzMub3JnXC8yMDE4XC9wcmVzZW50YXRpb25zXC92MSJdLCJ0eXBlIjpbIlZlcmlmaWFibGVQcmVzZW50YXRpb24iXX0sImF1ZCI6ImRvbWFpbiJ9"
  val rawSignatureBase64 =
    "ZGjNy5vyOaDCPfZRqpjbolcPZXD3WmM_VugjIgVhY2ANARaJ_PnNCnTFFYUgajzml8kIhyIPQsVOchQDQz1RMA"
  val swiftSignatureBase64 =
    "MEQCIGBjYQUiI-hWP2Na93BlD1ei25iqUfY9wqA58pvLzWhkAiAwUT1DAxRyTsVCDyKHCMmX5jxqIIUVxXQKzfn8iRYBDQ"
  val nimbusDerSignatureBase64 =
    "MEQCIGRozcub8jmgwj32UaqY26JXD2Vw91pjP1boIyIFYWNgAiANARaJ_PnNCnTFFYUgajzml8kIhyIPQsVOchQDQz1RMA=="

  test("testIsPointOnSecp256k1Curve") {
    val x = Base64("P6XiWtDoaj6g3le12ljqjl3J_ZXa_kRswC9GNUYmkTU").decode
    val y = Base64("gYVURJfxKMpNPnKlLr4MOLNVaYvBNOoy9L50E8jVx8Q").decode
    val key = CrytoUtil.unsafeFromByteCoordinates(x, y)
    key match
      case Left(value)  => fail(value)
      case Right(value) => assertEquals(value.getAlgorithm(), "ECDSA")
  }
  test("testIsPointOnSecp256k1Curve method 2") {
    val x = Base64("P6XiWtDoaj6g3le12ljqjl3J_ZXa_kRswC9GNUYmkTU").decode
    val y = Base64("gYVURJfxKMpNPnKlLr4MOLNVaYvBNOoy9L50E8jVx8Q").decode
    val PUBLIC_KEY_COORDINATE_BYTE_SIZE: Int = 32
    val xTrimmed = x.dropWhile(_ == 0.toByte)
    assert(
      xTrimmed.size <= PUBLIC_KEY_COORDINATE_BYTE_SIZE,
      s"Expected x coordinate byte length to be less than or equal ${PUBLIC_KEY_COORDINATE_BYTE_SIZE}, but got ${x.size} bytes"
    )

    val yTrimmed = y.dropWhile(_ == 0.toByte)
    assert(
      yTrimmed.size <= PUBLIC_KEY_COORDINATE_BYTE_SIZE,
      s"Expected y coordinate byte length to be less than or equal ${PUBLIC_KEY_COORDINATE_BYTE_SIZE}, but got ${y.size} bytes"
    )

    val header: Byte = 0x04
    val key = Seq(header).toArray ++ x ++ y
    CrytoUtil.unsafeFromCompressed(key)
  }

  test("stackexchange Test Vectors") {
    keys
      .map { (x, y) =>
        val PUBLIC_KEY_COORDINATE_BYTE_SIZE: Int = 32
        val xTrimmed = x.dropWhile(_ == 0.toByte)
        assert(
          xTrimmed.size <= PUBLIC_KEY_COORDINATE_BYTE_SIZE,
          s"Expected x coordinate byte length to be less than or equal ${PUBLIC_KEY_COORDINATE_BYTE_SIZE}, but got ${x.size} bytes"
        )
        val yTrimmed = y.dropWhile(_ == 0.toByte)
        assert(
          yTrimmed.size <= PUBLIC_KEY_COORDINATE_BYTE_SIZE,
          s"Expected y coordinate byte length to be less than or equal ${PUBLIC_KEY_COORDINATE_BYTE_SIZE}, but got ${y.size} bytes"
        )
        val header: Byte = 0x04
        val key = Seq(header).toArray ++ x ++ y

        (CrytoUtil.unsafeFromByteCoordinates(x, y), CrytoUtil.unsafeFromCompressed(key))
      }
      .foreach {
        case (Right(k1), Right(k2)) =>
          assertEquals(k1.getFormat(), k2.getFormat())
          assertEquals(k1.getAlgorithm(), "ECDSA")
          assertEquals(k2.getAlgorithm(), "ECDSA")
          assert(k1.getEncoded().sameElements(k2.getEncoded()))
        case (_, _) => fail("Must have the same keys")
      }
  }

  test("Metadata 6418 (CompressedECKey)") {
    val cardanoPrismEntry = MainnetExamples.metadata_6418.toCardanoPrismEntry.getOrElse(???)
    val tmp = MaybeOperation.fromProto("tx", -1, cardanoPrismEntry.content)
    val mySignedPrismOperation = tmp.head.asInstanceOf[MySignedPrismOperation[OP]]

    PrismPublicKey
      .filterECKey(mySignedPrismOperation.operation.asInstanceOf[CreateDidOP].publicKeys)
      .find(_.usage == PrismKeyUsage.MasterKeyUsage) match
      case None                                            => fail("Missing MASTER_KEY")
      case Some(UncompressedECKey(id, usage, curve, x, y)) => fail("Expeting CompressedEcKeyData")
      case Some(CompressedECKey(id, usage, curve, data)) =>
        CrytoUtil.unsafeFromCompressed(data) match
          case Left(error) => fail("No key: error")
          case Right(key) =>
            val p = key.asInstanceOf[java.security.interfaces.ECPublicKey]

            CrytoUtil.checkECDSASignature(
              msg = mySignedPrismOperation.protobuf.toByteArray,
              sig = mySignedPrismOperation.signature,
              pubKey = key
            ) match
              case Left(ex)     => fail(ex)
              case Right(false) => fail("invalid")
              case Right(true)  => assertEquals(id, "master0") // ok
  }

  test("Metadata 6451 (CompressedECKey)") {
    val cardanoPrismEntry = MainnetExamples.metadata_6451.toCardanoPrismEntry.getOrElse(???)
    val tmp = MaybeOperation.fromProto("tx", -1, cardanoPrismEntry.content)
    val mySignedPrismOperation = tmp.head.asInstanceOf[MySignedPrismOperation[OP]]

    PrismPublicKey
      .filterECKey(mySignedPrismOperation.operation.asInstanceOf[CreateDidOP].publicKeys)
      .find(_.usage == PrismKeyUsage.MasterKeyUsage) match
      case None                                            => fail("Missing MASTER_KEY")
      case Some(UncompressedECKey(id, usage, curve, x, y)) => fail("Expeting CompressedEcKeyData")
      case Some(CompressedECKey(id, usage, curve, data)) =>
        CrytoUtil.unsafeFromCompressed(data) match
          case Left(error) => fail("No key: error")
          case Right(key) =>
            CrytoUtil.checkECDSASignature(
              msg = mySignedPrismOperation.protobuf.toByteArray,
              sig = mySignedPrismOperation.signature,
              pubKey = key
            ) match
              case Left(ex)     => fail(ex)
              case Right(false) => fail("invalid")
              case Right(true)  => assertEquals(id, "master0") // ok
  }

  // test("Metadata 6452 (UncompressedECKey)") {
  //   val cardanoPrismEntry = MainnetExamples.metadata_6452.toCardanoPrismEntry.getOrElse(???)
  //   val tmp = MaybeOperation.fromProto("tx", -1, cardanoPrismEntry.content)
  //   val mySignedPrismOperation = tmp.head.asInstanceOf[MySignedPrismOperation[OP]]
  //   PrismPublicKey
  //     .filterECKey(mySignedPrismOperation.operation.asInstanceOf[CreateDidOP].publicKeys)
  //     .find(_.usage == PrismKeyUsage.MasterKeyUsage) match
  //     case None                                          => fail("Missing MASTER_KEY")
  //     case Some(CompressedECKey(id, usage, curve, data)) => fail("Expeting UncompressedECKey")
  //     case Some(UncompressedECKey(id, usage, curve, x, y)) =>
  //       CrytoUtil.unsafeFromByteCoordinates(x, y) match
  //         case Left(error) => fail("No key: error")
  //         case Right(key) =>
  //           CrytoUtil.checkECDSASignature(
  //             msg = mySignedPrismOperation.protobuf.toByteArray,
  //             sig = mySignedPrismOperation.signature,
  //             pubKey = key
  //           ) match
  //             case Left(ex)     => fail(ex)
  //             case Right(false) => fail("invalid")
  //             case Right(true)  => assertEquals(id, "master-0") // ok
  // }

  test("unsafeFromByteCoordinates") {
    val x = Array(114, 112, -99, -18, 41, 124, -35, 121, 88, -107, -16, -5, -123, -91, -2, 13, 92, 26, -112, 95, 95,
      -128, -81, -46, -22, 14, 102, 40, 52, -17, -12, -27).map(_.toByte)
    val y = Array(-112, 79, -5, -23, 73, 2, 112, -15, -18, 77, -18, -8, -91, -75, -23, 68, 43, -5, 81, 21, 49, 33, -33,
      -46, -82, 48, 20, -50, 21, -84, -124, 95).map(_.toByte)
    val key = CrytoUtil.unsafeFromByteCoordinates(x, y)
    assert(key.isRight)
  }

  test("Generate Key sign and verify") {
    val (privateKeyParams, publicKeyParams) = CrytoUtil.generateKeyPair
    val data = Seq.range(1, 100).map(_.toByte).toArray

    val ecParameterSpec = EC5Util.convertToSpec(privateKeyParams.getParameters)
    val ecPrivateKeySpec = ECPrivateKeySpec(privateKeyParams.getD, ecParameterSpec)
    val privatekey = KeyFactory.getInstance("EC").generatePrivate(ecPrivateKeySpec)

    val ecPoint = EC5Util.convertPoint(publicKeyParams.getQ)
    val ecPublicKeySpec = ECPublicKeySpec(ecPoint, ecParameterSpec)
    val publicKey = KeyFactory.getInstance("EC").generatePublic(ecPublicKeySpec)

    val maybeSig = CrytoUtil.signECDSASignature(msg = data, privatekey)
    maybeSig match
      case Left(value) => fail(value)
      case Right(sig) =>
        val v = CrytoUtil.checkECDSASignature(msg = data, sig = sig, pubKey = publicKey)
        v match
          case Left(value)  => fail(value)
          case Right(value) => assert(value) //
  }

// Vectors from https://crypto.stackexchange.com/a/21206
  def keys = Seq(
    (
      "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798",
      "483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8"
    ),
    (
      "c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5",
      "1ae168fea63dc339a3c58419466ceaeef7f632653266d0e1236431a950cfe52a"
    ),
    (
      "f9308a019258c31049344f85f89d5229b531c845836f99b08601f113bce036f9",
      "388f7b0f632de8140fe337e62a37f3566500a99934c2231b6cb9fd7584b8e672"
    ),
    (
      "2f8bde4d1a07209355b4a7250a5c5128e88b84bddc619ab7cba8d569b240efe4",
      "d8ac222636e5e3d6d4dba9dda6c9c426f788271bab0d6840dca87d3aa6ac62d6"
    ),
    (
      "fff97bd5755eeea420453a14355235d382f6472f8568a18b2f057a1460297556",
      "ae12777aacfbb620f3be96017f45c560de80f0f6518fe4a03c870c36b075f297"
    ),
    (
      "5cbdf0646e5db4eaa398f365f2ea7a0e3d419b7e0330e39ce92bddedcac4f9bc",
      "6aebca40ba255960a3178d6d861a54dba813d0b813fde7b5a5082628087264da"
    ),
    (
      "2f01e5e15cca351daff3843fb70f3c2f0a1bdd05e5af888a67784ef3e10a2a01",
      "5c4da8a741539949293d082a132d13b4c2e213d6ba5b7617b5da2cb76cbde904"
    ),
    (
      "acd484e2f0c7f65309ad178a9f559abde09796974c57e714c35f110dfc27ccbe",
      "cc338921b0a7d9fd64380971763b61e9add888a4375f8e0f05cc262ac64f9c37"
    ),
    (
      "a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7",
      "893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7"
    ),
    (
      "774ae7f858a9411e5ef4246b70c65aac5649980be5c17891bbec17895da008cb",
      "d984a032eb6b5e190243dd56d7b7b365372db1e2dff9d6a8301d74c9c953c61b"
    ),
    (
      "d01115d548e7561b15c38f004d734633687cf4419620095bc5b0f47070afe85a",
      "a9f34ffdc815e0d7a8b64537e17bd81579238c5dd9a86d526b051b13f4062327"
    ),
    (
      "f28773c2d975288bc7d1d205c3748651b075fbc6610e58cddeeddf8f19405aa8",
      "0ab0902e8d880a89758212eb65cdaf473a1a06da521fa91f29b5cb52db03ed81"
    ),
    (
      "499fdf9e895e719cfd64e67f07d38e3226aa7b63678949e6e49b241a60e823e4",
      "cac2f6c4b54e855190f044e4a7b3d464464279c27a3f95bcc65f40d403a13f5b"
    ),
    (
      "d7924d4f7d43ea965a465ae3095ff41131e5946f3c85f79e44adbcf8e27e080e",
      "581e2872a86c72a683842ec228cc6defea40af2bd896d3a5c504dc9ff6a26b58"
    ),
    (
      "e60fce93b59e9ec53011aabc21c23e97b2a31369b87a5ae9c44ee89e2a6dec0a",
      "f7e3507399e595929db99f34f57937101296891e44d23f0be1f32cce69616821"
    ),
    (
      "defdea4cdb677750a420fee807eacf21eb9898ae79b9768766e4faa04a2d4a34",
      "4211ab0694635168e997b0ead2a93daeced1f4a04a95c0f6cfb199f69e56eb77"
    ),
    (
      "5601570cb47f238d2b0286db4a990fa0f3ba28d1a319f5e7cf55c2a2444da7cc",
      "c136c1dc0cbeb930e9e298043589351d81d8e0bc736ae2a1f5192e5e8b061d58"
    ),
    (
      "2b4ea0a797a443d293ef5cff444f4979f06acfebd7e86d277475656138385b6c",
      "85e89bc037945d93b343083b5a1c86131a01f60c50269763b570c854e5c09b7a"
    ),
    (
      "4ce119c96e2fa357200b559b2f7dd5a5f02d5290aff74b03f3e471b273211c97",
      "12ba26dcb10ec1625da61fa10a844c676162948271d96967450288ee9233dc3a"
    ),
    (
      "a90cc3d3f3e146daadfc74ca1372207cb4b725ae708cef713a98edd73d99ef29",
      "5a79d6b289610c68bc3b47f3d72f9788a26a06868b4d8e433e1e2ad76fb7dc76"
    ),
    (
      "e5a2636bcfd412ebf36ec45b19bfb68a1bc5f8632e678132b885f7df99c5e9b3",
      "736c1ce161ae27b405cafd2a7520370153c2c861ac51d6c1d5985d9606b45f39"
    ),
    (
      "a6b594b38fb3e77c6edf78161fade2041f4e09fd8497db776e546c41567feb3c",
      "71444009192228730cd8237a490feba2afe3d27d7cc1136bc97e439d13330d55"
    ),
    (
      "00000000000000000000003b78ce563f89a0ed9414f5aa28ad0d96d6795f9c63",
      "3f3979bf72ae8202983dc989aec7f2ff2ed91bdd69ce02fc0700ca100e59ddf3"
    ),
    (
      "e24ce4beee294aa6350faa67512b99d388693ae4e7f53d19882a6ea169fc1ce1",
      "8b71e83545fc2b5872589f99d948c03108d36797c4de363ebd3ff6a9e1a95b10"
    ),
    (
      "4ce119c96e2fa357200b559b2f7dd5a5f02d5290aff74b03f3e471b273211c97",
      "ed45d9234ef13e9da259e05ef57bb3989e9d6b7d8e269698bafd77106dcc1ff5"
    ),
    (
      "2b4ea0a797a443d293ef5cff444f4979f06acfebd7e86d277475656138385b6c",
      "7a17643fc86ba26c4cbcf7c4a5e379ece5fe09f3afd9689c4a8f37aa1a3f60b5"
    ),
    (
      "5601570cb47f238d2b0286db4a990fa0f3ba28d1a319f5e7cf55c2a2444da7cc",
      "3ec93e23f34146cf161d67fbca76cae27e271f438c951d5e0ae6d1a074f9ded7"
    ),
    (
      "defdea4cdb677750a420fee807eacf21eb9898ae79b9768766e4faa04a2d4a34",
      "bdee54f96b9cae9716684f152d56c251312e0b5fb56a3f09304e660861a910b8"
    ),
    (
      "e60fce93b59e9ec53011aabc21c23e97b2a31369b87a5ae9c44ee89e2a6dec0a",
      "081caf8c661a6a6d624660cb0a86c8efed6976e1bb2dc0f41e0cd330969e940e"
    ),
    (
      "d7924d4f7d43ea965a465ae3095ff41131e5946f3c85f79e44adbcf8e27e080e",
      "a7e1d78d57938d597c7bd13dd733921015bf50d427692c5a3afb235f095d90d7"
    ),
    (
      "499fdf9e895e719cfd64e67f07d38e3226aa7b63678949e6e49b241a60e823e4",
      "353d093b4ab17aae6f0fbb1b584c2b9bb9bd863d85c06a4339a0bf2afc5ebcd4"
    ),
    (
      "f28773c2d975288bc7d1d205c3748651b075fbc6610e58cddeeddf8f19405aa8",
      "f54f6fd17277f5768a7ded149a3250b8c5e5f925ade056e0d64a34ac24fc0eae"
    ),
    (
      "d01115d548e7561b15c38f004d734633687cf4419620095bc5b0f47070afe85a",
      "560cb00237ea1f285749bac81e8427ea86dc73a2265792ad94fae4eb0bf9d908"
    ),
    (
      "774ae7f858a9411e5ef4246b70c65aac5649980be5c17891bbec17895da008cb",
      "267b5fcd1494a1e6fdbc22a928484c9ac8d24e1d20062957cfe28b3536ac3614"
    ),
    (
      "a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7",
      "76c545bdabe643d85c4938196c5db3969086b3d127885ea6c3411ac3fc8c9358"
    ),
    (
      "acd484e2f0c7f65309ad178a9f559abde09796974c57e714c35f110dfc27ccbe",
      "33cc76de4f5826029bc7f68e89c49e165227775bc8a071f0fa33d9d439b05ff8"
    ),
    (
      "2f01e5e15cca351daff3843fb70f3c2f0a1bdd05e5af888a67784ef3e10a2a01",
      "a3b25758beac66b6d6c2f7d5ecd2ec4b3d1dec2945a489e84a25d3479342132b"
    ),
    (
      "5cbdf0646e5db4eaa398f365f2ea7a0e3d419b7e0330e39ce92bddedcac4f9bc",
      "951435bf45daa69f5ce8729279e5ab2457ec2f47ec02184a5af7d9d6f78d9755"
    ),
    (
      "fff97bd5755eeea420453a14355235d382f6472f8568a18b2f057a1460297556",
      "51ed8885530449df0c4169fe80ba3a9f217f0f09ae701b5fc378f3c84f8a0998"
    ),
    (
      "2f8bde4d1a07209355b4a7250a5c5128e88b84bddc619ab7cba8d569b240efe4",
      "2753ddd9c91a1c292b24562259363bd90877d8e454f297bf235782c459539959"
    ),
    (
      "e493dbf1c10d80f3581e4904930b1404cc6c13900ee0758474fa94abe8c4cd13",
      "ae1266c15f2baa48a9bd1df6715aebb7269851cc404201bf30168422b88c630d"
    ),
    (
      "f9308a019258c31049344f85f89d5229b531c845836f99b08601f113bce036f9",
      "c77084f09cd217ebf01cc819d5c80ca99aff5666cb3ddce4934602897b4715bd"
    ),
    (
      "c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5",
      "e51e970159c23cc65c3a7be6b99315110809cd9acd992f1edc9bce55af301705"
    ),
    (
      "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798",
      "b7c52588d95c3b9aa25b0403f1eef75702e84bb7597aabe663b82f6f04ef2777"
    )
  ).map((x, y) =>
    val bytesX = x.sliding(2, 2).map(Integer.parseInt(_, 16).toByte).toArray
    val bytesY = y.sliding(2, 2).map(Integer.parseInt(_, 16).toByte).toArray

    (bytesX, bytesY)
  )
}
