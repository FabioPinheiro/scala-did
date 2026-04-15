package object fmgp {
  val JsUnsupported = munit.Tag("JsUnsupported")
  val IntregrationTest = new munit.Tag("IntregrationTest")
  val DBTest = new munit.Tag("DBTest")

  val ShowTest = new munit.Tag("ShowTest")

  /** override val munitTimeout = fmgp.ShowTestTimeout */
  val ShowTestTimeout = scala.concurrent.duration.Duration(5, "s")
}
