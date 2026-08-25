val scalaJSVersion = sys.env.getOrElse("SCALAJS_VERSION", "1.22.0")
// crossproject - https://github.com/portable-scala/sbt-crossproject/tags
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.4.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.2")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.4.2")

// scalablytyped - https://scalablytyped.org/docs/plugin - https://github.com/ScalablyTyped/Converter/tags
// resolvers += Resolver.bintrayRepo("oyvindberg", "converter") //TODO REMOVE  Bintray was shut down
resolvers += MavenRepository("sonatype-s01-snapshots", "https://s01.oss.sonatype.org/content/repositories/snapshots")
// Required for sbt-converter 1.0.0-beta45: it pulls scala-parser-combinators 2.4.0 while older sbt plugins pull 1.x,
// which makes sbt fail project loading with an eviction error.
// TODO Remove when transitive sbt plugins converge on scala-parser-combinators 2.x, or sbt-converter no longer causes this eviction.
libraryDependencySchemes += "org.scala-lang.modules" %% "scala-parser-combinators" % VersionScheme.Always
addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta45")
