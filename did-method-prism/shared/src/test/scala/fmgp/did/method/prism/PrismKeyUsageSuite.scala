package fmgp.did.method.prism

import munit.*
import fmgp.did.method.prism.proto.PrismKeyUsage
import _root_.proto.prism.KeyUsage

/** didResolverPrismJVM/testOnly fmgp.did.method.prism.PrismKeyUsageSuite */
class PrismKeyUsageSuite extends FunSuite {

  test("PrismKeyUsage is aligned with proto") {
    assertEquals(PrismKeyUsage.MasterKeyUsage.protoEnum, KeyUsage.MASTER_KEY.index)
    assertEquals(PrismKeyUsage.IssuingKeyUsage.protoEnum, KeyUsage.ISSUING_KEY.index)
    assertEquals(PrismKeyUsage.KeyAgreementKeyUsage.protoEnum, KeyUsage.KEY_AGREEMENT_KEY.index)
    assertEquals(PrismKeyUsage.AuthenticationKeyUsage.protoEnum, KeyUsage.AUTHENTICATION_KEY.index)
    assertEquals(PrismKeyUsage.RevocationKeyUsage.protoEnum, KeyUsage.REVOCATION_KEY.index)
    assertEquals(PrismKeyUsage.CapabilityinvocationKeyUsage.protoEnum, KeyUsage.CAPABILITY_INVOCATION_KEY.index)
    assertEquals(PrismKeyUsage.CapabilitydelegationKeyUsage.protoEnum, KeyUsage.CAPABILITY_DELEGATION_KEY.index)
    assertEquals(PrismKeyUsage.VdrKeyUsage.protoEnum, KeyUsage.VDR_KEY.index)
  }
}
