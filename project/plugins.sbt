val scalaJSVersion = sys.env.getOrElse("SCALAJS_VERSION", "1.22.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.2")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.4.2")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.12.1")
