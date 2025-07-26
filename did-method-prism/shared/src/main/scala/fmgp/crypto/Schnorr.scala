package fmgp.crypto

object Schnorr {
  def rsValuesFromDEREncoded(bytes: Array[Byte]): (Array[Byte], Array[Byte]) = {
    assert(bytes(0) == 0x30) // DEREncoded
    assert(bytes.length == bytes(1) + 2)

    // r-value
    val r_aux = 2
    assert(bytes(r_aux) == 0x02) // DEREncoded
    val rrLength = bytes(r_aux + 1)
    val rrStart = rrLength match
      case 0x20 => r_aux + 2
      case 0x21 => r_aux + 2 + 1
      case _ =>
        assert(false, "unexpected length for r-value")
        ???

    // s-value
    val s_aux = rrStart + 0x20
    assert(bytes(s_aux) == 0x02) // DEREncoded
    val ssLength = bytes(s_aux + 1)
    val ssStart = ssLength match
      case 0x20 => s_aux + 2
      case 0x21 => s_aux + 2 + 1
      case _ =>
        assert(false, "unexpected length for s-value")
        ???

    val r = bytes.slice(rrStart, rrStart + 0x20)
    assert(r.size == 0x20)
    val s = bytes.slice(ssStart, ssStart + 0x20)
    assert(s.size == 0x20)

    assert(ssStart + 0x20 == bytes.length)

    (r, s)
  }

}
