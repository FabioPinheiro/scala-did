import org.scalajs.linker.interface.{ModuleInitializer, ModuleSplitStyle}

import scala.sys.process.Process

inThisBuild(
  Seq(
    scalaVersion := "3.3.8",
    organization := "app.fmgp",
    homepage := Some(url("https://github.com/FabioPinheiro/cardano-prism-cli")),
    licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/FabioPinheiro/cardano-prism-cli"),
        "scm:git:git@github.com:FabioPinheiro/cardano-prism-cli.git",
      )
    ),
    developers := List(
      Developer("FabioPinheiro", "Fabio Pinheiro", "fabiomgpinheiro@gmail.com", url("https://fmgp.app"))
    ),
    version := "1.0",
    versionScheme := Some("early-semver"),
  )
)

lazy val scalaDidVersion = "0.1.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "cardano-prism-cli-root",
    publish / skip := true,
  )
  .aggregate(cardanoPrismCli, cardanoPrismCip30Webapp)

lazy val cardanoPrismCli = project
  .in(file("cardano-prism-cli"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "cardano-prism-cli",
    publish / skip := false,
    buildInfoPackage := "fmgp.did.method.prism.cli",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    libraryDependencies ++= Seq(
      "app.fmgp" %% "did" % scalaDidVersion,
      "app.fmgp" %% "did-method-prism" % scalaDidVersion,
      "app.fmgp" %% "did-method-peer" % scalaDidVersion,
      "app.fmgp" %% "did-uniresolver" % scalaDidVersion,
      "dev.zio" %% "zio" % "2.1.26",
      "dev.zio" %% "zio-cli" % "0.8.2",
      "dev.zio" %% "zio-http" % "3.11.4",
      "dev.zio" %% "zio-json" % "0.10.0",
      "org.reactivemongo" %% "reactivemongo" % "1.1.0-RC19",
    ),
    assembly / mainClass := Some("fmgp.did.method.prism.cli.PrismCli"),
    assembly / assemblyJarName := "cardano-prism.jar",
    Compile / resourceGenerators += Def.task {
      val bundleJs = (cardanoPrismCip30Webapp / cip30Bundle).value
      val bundleMap = file(bundleJs.getPath + ".map")
      val resourceDir = (Compile / resourceManaged).value / "cip30"
      val outJs = resourceDir / "bundle.js"
      val outMap = resourceDir / "bundle.js.map"

      IO.createDirectory(resourceDir)
      IO.copyFile(bundleJs, outJs)
      if (bundleMap.isFile) {
        IO.copyFile(bundleMap, outMap)
        Seq(outJs, outMap)
      } else Seq(outJs)
    }.taskValue,
  )

lazy val cip30Bundle =
  taskKey[File]("Build the esbuild bundle of the Cardano PRISM CIP-30 webapp")

lazy val cardanoPrismCip30Webapp = project
  .in(file("cardano-prism-cip30-webapp"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "cardano-prism-cip30-webapp",
    publish / skip := true,
    libraryDependencies ++= Seq(
      "app.fmgp" %%% "did-method-prism" % scalaDidVersion,
      "com.raquo" %%% "laminar" % "17.2.1",
      "com.raquo" %%% "waypoint" % "9.0.0",
      "com.lihaoyi" %%% "upickle" % "4.4.3",
      "org.scala-js" %%% "scalajs-dom" % "2.8.1",
      "org.scalus" %%% "scalus-cardano-ledger" % "1.1.0",
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    scalaJSLinkerConfig ~= {
      _.withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("fmgp.did.method.prism.cip30")))
    },
    Compile / scalaJSModuleInitializers += {
      ModuleInitializer
        .mainMethod("fmgp.did.method.prism.cip30.Cip30App", "main")
        .withModuleID("cip30webapp")
    },
    Test / test := {},
    cip30Bundle := {
      val log = streams.value.log
      val _ = (Compile / fullLinkJS).value
      val scalaJsDir = (Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value
      val bundleDir = baseDirectory.value
      val bundleJs = bundleDir / "dist" / "bundle.js"
      val environment = Seq("CIP30_SCALAJS_DIR" -> scalaJsDir.getAbsolutePath)

      log.info(s"cip30Bundle: npm ci in $bundleDir")
      if (Process(Seq("npm", "ci"), bundleDir, environment *).! != 0)
        sys.error("cip30Bundle: npm ci failed")

      log.info(s"cip30Bundle: node build.js in $bundleDir")
      if (Process(Seq("node", "build.js"), bundleDir, environment *).! != 0)
        sys.error("cip30Bundle: esbuild bundle failed")
      if (!bundleJs.isFile)
        sys.error(s"cip30Bundle: expected $bundleJs to exist after build")
      bundleJs
    },
    cleanFiles ++= Seq(
      baseDirectory.value / "dist",
      baseDirectory.value / ".entry.generated.js",
      baseDirectory.value / ".empty-stub.js",
      baseDirectory.value / "node_modules",
    ),
  )

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
  // GraalVM native-image hints (reflect-config.json etc) — read only by `native-image`,
  // not at runtime on the JVM. netty-transport ships them and reactivemongo-shaded
  // bundles its own (different) copy of the same files. Pick first.
  case PathList("META-INF", "native-image", _*) => MergeStrategy.first
  case path                                     =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(path)
}
