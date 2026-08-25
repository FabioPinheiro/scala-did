import org.scalajs.linker.interface.{ModuleInitializer, ModuleSplitStyle}

inThisBuild(
  Seq(
    scalaVersion := "3.3.8", // Also update docs/publishWebsite.sh and any ref to scala-3.3.8
    organization := "app.fmgp.sandbox",
    version := "1.0-SNAPSHOT",
    licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer("FabioPinheiro", "Fabio Pinheiro", "fabiomgpinheiro@gmail.com", url("http://fmgp.app"))
    ),
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-source",
      "future",
      "-Wconf:msg=pattern selector should be an instance of Matchable:s",
      "-deprecation",
      "-unchecked",
      "-feature",
      "-language:implicitConversions",
      "-Xfatal-warnings",
      "-Xmax-inlines",
      "43",
    ),
  )
)

lazy val V = new {
  val scalaDidVersion = "0.1.1"
  // https://mvnrepository.com/artifact/org.scala-js/scalajs-dom
  val scalajsDom = "2.8.1"
  val zio = "2.1.26"
  val zioJson = "0.10.0"
  val zioHttp = "3.11.4" // With fix CORS https://github.com/zio/zio-http/pull/2490
  val zioStreams = "2.1.26"
  val zioMunitTest = "0.4.0"
  val munit = "1.3.5"
  val laminar = "17.2.1"
  val waypoint = "9.0.0"
  val upickle = "4.4.3"
}

/* REMOVE?
/** NPM Dependencies */
lazy val NPM = new { // When update the dependencies also update in package.json
  val sha256 = "js-sha256" -> "0.11.0"
  val jose = "jose" -> "5.10.0"

  // val elliptic = "elliptic" -> "6.6.1"
  // val ellipticType = "@types/elliptic" -> "6.4.18"

  val nobleCurves = "@noble/curves" -> "1.9.7"
  val appoloJS = "@hyperledger/identus-apollo" -> ("^" + V.identusApollo)

}
 */

lazy val D = new {
  val scalajsDom = Def.setting("org.scala-js" %%% "scalajs-dom" % V.scalajsDom)
  val zio = Def.setting("dev.zio" %%% "zio" % V.zio)
  val zioJson = Def.setting("dev.zio" %%% "zio-json" % V.zioJson)
  val zioStreams = Def.setting("dev.zio" %%% "zio-streams" % V.zioStreams)
  val zioHttp = Def.setting("dev.zio" %% "zio-http" % V.zioHttp)
  val munit = Def.setting("org.scalameta" %%% "munit" % V.munit % Test)
  val zioMunitTest = Def.setting("com.github.poslegm" %%% "munit-zio" % V.zioMunitTest % Test)
  val laminar = Def.setting("com.raquo" %%% "laminar" % V.laminar)
  val waypoint = Def.setting("com.raquo" %%% "waypoint" % V.waypoint)
  val upickle = Def.setting("com.lihaoyi" %%% "upickle" % V.upickle)
}

lazy val scalaDidModules = Seq(
  "did",
  "did-framework",
  "did-imp",
  "did-method-peer",
  "did-method-prism",
  // did-method-web is not published for 0.1.1; Global delegates did:web resolution to Uniresolver.
  "did-uniresolver",
)

lazy val jsHeader =
  """/* FMGP Scala-DID Sandbox
    | * https://github.com/FabioPinheiro/scala-did-sandbox
    | */""".stripMargin.trim() + "\n"

lazy val scalaJSViteConfigure: Project => Project =
  _.enablePlugins(ScalaJSPlugin)
    .enablePlugins(ScalablyTypedConverterExternalNpmPlugin)
    .settings(
      scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule).withJSHeader(jsHeader) },
      // Tell ScalablyTyped that we manage `npm install` ourselves
      externalNpm := rootPaths.value.apply("BASE").toFile(),
      stShortModuleNames := true,
    )

lazy val buildInfoConfigure: Project => Project = _.enablePlugins(BuildInfoPlugin).settings(
  buildInfoPackage := "fmgp",
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "scala-did-sandbox",
    publish / skip := true,
  )
  .aggregate(didExample.js, didExample.jvm, serviceworker, webapp, backend)

lazy val didExample = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-example"))
  .settings(
    name := "did-example",
    publish / skip := true,
    libraryDependencies ++= scalaDidModules.map(module => "app.fmgp" %%% module % V.scalaDidVersion),
  )

lazy val serviceworker = project
  .in(file("serviceworker"))
  .settings(
    name := "fmgp-serviceworker",
    publish / skip := true,
  )
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("fmgp.serviceworker")))
        .withJSHeader(jsHeader)
    },
    scalaJSModuleInitializers := Seq( // scalaJSUseMainModuleInitializer := true,
      ModuleInitializer.mainMethod("fmgp.serviceworker.SW", "main").withModuleID("sw")
    ),
    libraryDependencies ++= Seq(D.scalajsDom.value, D.zio.value, D.zioJson.value),
  )

lazy val webapp = project
  .in(file("webapp"))
  .settings(
    name := "fmgp-webapp",
    publish / skip := true,
  )
  .configure(scalaJSViteConfigure)
  .configure(buildInfoConfigure)
  .settings(
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("fmgp.webapp")))
        .withJSHeader(jsHeader) // TODO REVIEW this is on scalaJSViteConfigure no?
    },
    Compile / scalaJSModuleInitializers += {
      ModuleInitializer.mainMethod("fmgp.webapp.App", "main").withModuleID("webapp")
    },
    libraryDependencies ++= Seq(
      D.laminar.value,
      D.waypoint.value,
      D.upickle.value,
      D.zio.value,
      D.zioJson.value,
    ),
    // The webapp has no test sources; its browser behavior is validated by the Vite build.
    Test / test := {},
  )
  .dependsOn(didExample.js, serviceworker)

lazy val backend = project
  .in(file("backend"))
  .settings(
    name := "did-backend",
    publish / skip := true,
    libraryDependencies ++= Seq(D.zioStreams.value, D.zioHttp.value, D.munit.value, D.zioMunitTest.value),
    assembly / mainClass := Some("fmgp.did.demo.AppServer"),
    assembly / assemblyJarName := "scala-did-demo-server.jar",
    Compile / unmanagedResourceDirectories += baseDirectory.value / "src" / "main" / "extra-resources",
    Compile / unmanagedResourceDirectories += rootPaths.value.apply("BASE").toFile() / "vite" / "dist",
  )
  .dependsOn(didExample.jvm)

addCommandAlias("fullPackAll", "serviceworker/fullLinkJS;webapp/fullLinkJS")

val webjarsPattern = "(META-INF/resources/webjars/.*)".r
val bouncycastlePattern1 = "(org/bouncycastle/.*)".r
val bouncycastlePattern2 = "(META-INF/versions/9/org/bouncycastle/.*)".r
val bouncycastlePattern3 = "(META-INF/versions/11/org/bouncycastle/.*)".r
val bouncycastlePattern4 = "(META-INF/versions/15/org/bouncycastle/.*)".r
val protobufPattern1 = "(google/protobuf/.*)".r
val protobufPattern2 = "(com/google/protobuf/.*)".r

ThisBuild / assemblyMergeStrategy := {
  case "META-INF/versions/9/module-info.class"    => MergeStrategy.first
  case "META-INF/versions/11/module-info.class"   => MergeStrategy.first
  case "META-INF/io.netty.versions.properties"    => MergeStrategy.first
  case "META-INF/versions/9/OSGI-INF/MANIFEST.MF" => MergeStrategy.first
  case "META-INF/okio.kotlin_module"              => MergeStrategy.first
  case webjarsPattern(file)                       => MergeStrategy.discard
  case "module-info.class"        => MergeStrategy.first // jackson-annotations-2.16.0.jar & checker-qual-3.43.0.jar
  case bouncycastlePattern1(file) => MergeStrategy.preferProject // because of a Apollo is using very old version
  case bouncycastlePattern2(file) => MergeStrategy.preferProject // because of a Apollo is using very old version
  case bouncycastlePattern3(file) => MergeStrategy.preferProject // because of a Apollo is using very old version
  case bouncycastlePattern4(file) => MergeStrategy.preferProject // because of a Apollo is using very old version
  case protobufPattern1(file)     => MergeStrategy.preferProject // because of a Apollo is using very old version
  case protobufPattern2(file)     => MergeStrategy.preferProject // because of a Apollo is using very old version
  // others
  case PathList("META-INF", "native-image", _*) => MergeStrategy.first
  case path                                     =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(path)
}
