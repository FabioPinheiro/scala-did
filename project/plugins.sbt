val scalaJSVersion = sys.env.getOrElse("SCALAJS_VERSION", "1.18.2")
// crossproject - https://github.com/portable-scala/sbt-crossproject/tags
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)
addSbtPlugin("org.scala-js" % "sbt-jsdependencies" % "1.0.2")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.4")

// addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.1.0") //we can now update 1.2.0!
// [error] (update) found version conflict(s) in library dependencies; some are suspected to be binary incompatible:
// [error]
// [error] 	* org.scala-lang.modules:scala-java8-compat_2.12:1.0.0 (early-semver) is selected over 0.8.0
// [error] 	    +- org.scalablytyped.converter:sbt-converter:1.0.0-beta34 (sbtVersion=1.0, scalaVersion=2.12) (depends on 1.0.0)
// [error] 	    +- com.typesafe.akka:akka-actor_2.12:2.5.17           (depends on 0.8.0)

/** scalajs-bundler https://scalacenter.github.io/scalajs-bundler/getting-started.html
  * enablePlugins(ScalaJSBundlerPlugin)
  *
  * You need to have npm installed on your system.
  *
  * https://github.com/scalacenter/scalajs-bundler/releases
  *
  * "sbt-scalajs-bundler" or "sbt-web-scalajs-bundler"
  */
addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.21.1")

// GRPC
//resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
//addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.19")
//libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.12"
////https://mvnrepository.com/artifact/com.thesamet.scalapb.grpcweb/scalapb-grpcweb
//libraryDependencies += "com.thesamet.scalapb.grpcweb" %% "scalapb-grpcweb-code-gen" % "0.6.4"

//https://scalablytyped.org/docs/plugin
//https://github.com/ScalablyTyped/Converter/tags
resolvers += Resolver.bintrayRepo("oyvindberg", "converter")
resolvers += MavenRepository("sonatype-s01-snapshots", "https://s01.oss.sonatype.org/content/repositories/snapshots")
addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta44")

// Utils Buildinfo - https://github.com/sbt/sbt-buildinfo
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")

// CI - https://github.com/rtimush/sbt-updates/tags
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.4") // sbt> dependencyUpdates

// TEST COVERAGE - https://github.com/scoverage/sbt-scoverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.3.0") // Needs scala version 3.2.2

// PUBLISH
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.9.2")
// addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.17")
// addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1") //https://github.com/sbt/sbt-pgp#sbt-pgp

// To quick develop the demo - Revolver use for command 'reStart'
// (like the command 'run' but run on the backgroun by forking the app from sbt)
// https://github.com/spray/sbt-revolver/tags
addSbtPlugin("io.spray" % "sbt-revolver" % "0.10.0")

// https://zio.dev/howto/migrate/zio-2.x-migration-guide%20v0.9.31
//sbt "scalafixEnable; scalafixAll github:zio/zio/Zio2Upgrade?sha=series/2.x"
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.0")

// mdoc - https://github.com/scalameta/mdoc/tags
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.6.3")
addSbtPlugin("org.typelevel" % "laika-sbt" % "1.3.1") // https://typelevel.org/Laika/
addSbtPlugin("com.github.sbt" % "sbt-unidoc" % "0.5.0") // https://github.com/sbt/sbt-unidoc
// addSbtPlugin("org.typelevel" % "sbt-typelevel-site" % "0.6.2") // https://typelevel.org/sbt-typelevel/site.html

// Deploy demo - https://github.com/sbt/sbt-assembly/tags
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.1")
addSbtPlugin("com.github.sbt" % "sbt-gzip" % "2.0.0")

// To debug what the job sends to https://github.com/FabioPinheiro/scala-did/security/dependabot
// See file in .github/workflows/sbt-dependency-submission.yml
if (sys.env.get("DEPEDABOT").isDefined) {
  println(s"Adding plugin sbt-github-dependency-submission since env DEPEDABOT is defined.")
  // The reason for this is that the plugin needs the variable to be defined. We don't want to have that requirement.
  libraryDependencies += {
    val dependency = "ch.epfl.scala" % "sbt-github-dependency-submission" % "3.1.0"
    val sbtV = (pluginCrossBuild / sbtBinaryVersion).value
    val scalaV = (update / scalaBinaryVersion).value
    Defaults.sbtPluginExtra(dependency, sbtV, scalaV)
  }
} else libraryDependencies ++= Seq[ModuleID]()
