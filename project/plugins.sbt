val scalaJSVersion = sys.env.getOrElse("SCALAJS_VERSION", "1.22.0")

// JVM/Scala.js cross-projects and Scala.js linking - https://github.com/portable-scala/sbt-crossproject/tags
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.4.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)

// Formatting
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.2")

// ScalablyTyped - https://scalablytyped.org/docs/plugin - https://github.com/ScalablyTyped/Converter/tags
resolvers += MavenRepository("sonatype-s01-snapshots", "https://s01.oss.sonatype.org/content/repositories/snapshots")
// sbt-converter 1.0.0-beta45 requires scala-parser-combinators 2.x, while
// older transitive sbt plugins still select 1.x.
libraryDependencySchemes += "org.scala-lang.modules" %% "scala-parser-combinators" % VersionScheme.Always
addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta45")

// CI - https://github.com/rtimush/sbt-updates/tags
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.7.0") // sbt> dependencyUpdates

// TEST COVERAGE - https://github.com/scoverage/sbt-scoverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.4")

// PUBLISH
libraryDependencies ++= {
  if (baseDirectory.value / "../.git" isDirectory) {
    // See https://stackoverflow.com/questions/35699543/how-to-load-dynamically-a-sbt-plugin
    val dependency = "com.github.sbt" % "sbt-ci-release" % "1.12.1"
    val sbtV = (update / sbtBinaryVersion).value // (pluginCrossBuild / sbtBinaryVersion).value
    val scalaV = (update / scalaBinaryVersion).value
    Seq(Defaults.sbtPluginExtra(dependency, sbtV, scalaV))
  } else {
    println("WARNING: sbt-ci-release plugin not loaded because git worktree")
    Seq[ModuleID]()
  }
}

// Documentation
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.9.1") // https://github.com/scalameta/mdoc/tags
addSbtPlugin("org.typelevel" % "laika-sbt" % "1.3.2") // https://typelevel.org/Laika/
addSbtPlugin("com.github.sbt" % "sbt-unidoc" % "0.6.1") // https://github.com/sbt/sbt-unidoc

// ScalaPB for did:prism
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.8")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.20"
