resolvers ++= Resolver.sonatypeOssRepos("public")
resolvers ++= Resolver.sonatypeOssRepos("snapshots")

import org.scalajs.linker.interface.{ModuleInitializer, ModuleSplitStyle}
import scala.sys.process._

inThisBuild(
  Seq(
    scalaVersion := "3.3.6", // Also update docs/publishWebsite.sh and any ref to scala-3.3.6
    // scalaVersion := "3.4.2", // Also update docs/publishWebsite.sh and any ref to scala-3.4.2
  )
)
// publish config
inThisBuild(
  Seq(
    Test / publishArtifact := false,
    // pomIncludeRepository := (_ => false),
    organization := "app.fmgp",
    homepage := Some(url("https://github.com/FabioPinheiro/scala-did")),
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
      // url ("https://github.com/FabioPinheiro/scala-did" + "/blob/master/LICENSE")
    ),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/FabioPinheiro/scala-did"),
        "scm:git:git@github.com:FabioPinheiro/scala-did.git"
      )
    ),
    developers := List(
      Developer("FabioPinheiro", "Fabio Pinheiro", "fabiomgpinheiro@gmail.com", url("http://fmgp.app"))
    ),
    // updateOptions := updateOptions.value.withLatestSnapshots(false),
    versionScheme := Some("early-semver"), // https://www.scala-sbt.org/1.x/docs/Publishing.html#Version+scheme
  )
)
lazy val notYetPublishedConfigure: Project => Project = _.settings(
  publish / skip := true
)

// ### publish Github ###
lazy val publishConfigure: Project => Project = _.settings(
  // For publish to Github
  // sonatypeSnapshotResolver := MavenRepository("sonatype-snapshots", s"https://${sonatypeCredentialHost.value}")
)
// inThisBuild(
//   Seq(
//     sonatypeCredentialHost := "maven.pkg.github.com/FabioPinheiro/scala-did",
//     sonatypeRepository := "https://maven.pkg.github.com",
//     fork := true,
//     Test / fork := false, // If true we get a Error: `test / test` tasks in a Scala.js project require `test / fork := false`.
//     run / connectInput := true,
//   ) ++ scala.util.Properties
//     .envOrNone("PACKAGES_GITHUB_TOKEN")
//     .map(passwd =>
//       credentials += Credentials(
//         "GitHub Package Registry",
//         "maven.pkg.github.com",
//         "FabioPinheiro",
//         passwd
//       )
//     )
// )

/** https://docs.scala-lang.org/scala3/guides/scaladoc/settings.html */
lazy val docConfigure: Project => Project =
  _.settings(
    autoAPIMappings := true,
    Compile / doc / target := {
      val path = rootPaths.value.apply("BASE").toFile() /
        "docs" / "target" / "scaladoc" / "apis" / name.value / baseDirectory.value.getName
      // REMOVE println(path.getAbsolutePath())
      path
    },
    apiURL := Some(url(s"https://doc.did.fmgp.app/api/")), // or s"https://javadoc.io/doc/app.fmgp/${name.value}_3"
  )

// Test Framework config
inThisBuild {
  val MUnitFramework = new TestFramework("munit.Framework")
  val testConfig: Tests.Argument =
    if (sys.env.get("SKIP_INTEGRATION_TEST").isDefined)
      Tests.Argument(MUnitFramework, "--exclude-tags=IntregrationTest")
    else
      Tests.Argument(MUnitFramework)

  Seq(Test / testOptions += testConfig)
}

/** Custom commands:
  *   - doc
  *   - docs/mdoc
  *   - docs/laikaSite
  *   - docs/unidoc
  *   - docs/previewSite
  */
lazy val docs = project
  .in(file("docs"))
  .settings(skip / publish := false) // Docs should not be publish as a jar but rather as a website
  .enablePlugins(MdocPlugin)
  .settings(
    mdocVariables := Map("VERSION" -> version.value),
    mdocIn := baseDirectory.value / "src",
    mdocExtraArguments := Seq("--no-link-hygiene"),
    scalacOptions ~= {
      def disableUnusedWarningsForMdoc(options: Seq[String]): Seq[String] =
        options.map { opt =>
          if (opt.startsWith("-Ywarn-unused")) opt + ",-locals,-explicits" else opt
        }
      disableUnusedWarningsForMdoc
    }
  )
  .enablePlugins(LaikaPlugin)
  .settings(
    name := "scala-did-docs",
    laikaTheme := ManualSettings.themeProvider(version.value),
    laikaConfig := ManualSettings.config,
    laikaExtensions := Seq(laika.format.Markdown.GitHubFlavor, laika.config.SyntaxHighlighting),
    Laika / sourceDirectories := Seq(mdocOut.value),
    Laika / target := baseDirectory.value / "target",
    laikaSite / target := target.value / "site" / "did-doc",
    laikaIncludeAPI := false,
  )
  .enablePlugins(ScalaUnidocPlugin)
  .settings(
    ScalaUnidoc / unidoc / target := target.value / "scaladoc" / "unidoc",
    ScalaUnidoc / unidoc / unidocProjectFilter := {
      inProjects(
        did.jvm,
        didFramework.jvm,
        didImp.jvm,
        multiformats.jvm,
        didResolverPeer.jvm,
        didResolverPrism.jvm,
        didPrismNode,
        didResolverWeb.jvm,
        didUniresolver.jvm,
      ), // or inAnyProject -- inProjects(...)
    }
  )
  .dependsOn(did.jvm, didCommProtocols.jvm, didImp.jvm, didResolverPeer.jvm)

/** Versions */
lazy val V = new {
  val scalajsJavaSecureRandom = "1.0.0"

  // FIXME another bug in the test framework https://github.com/scalameta/munit/issues/554
  val munit = "1.1.1" // "0.7.29"

  // https://mvnrepository.com/artifact/org.scala-js/scalajs-dom
  val scalajsDom = "2.8.0"
  // val scalajsLogging = "1.1.2-SNAPSHOT" //"1.1.2"

  // https://mvnrepository.com/artifact/dev.zio/zio
  val zio = "2.1.19"
  val zioJson = "0.7.44"
  val zioMunitTest = "0.4.0"
  val zioHttp = "3.3.3" // FIXES CORS https://github.com/zio/zio-http/pull/2490
  val zioPrelude = "1.0.0-RC21"

  // https://mvnrepository.com/artifact/io.github.cquiroz/scala-java-time
  val scalaJavaTime = "2.5.0"

  val bullet = "1.16.1"

  val logbackClassic = "1.2.10"
  val scalaLogging = "3.9.4"

  val bouncycastle = "1.80"
  val nimbusJoseJwt = "10.3.1"

  val laika = "1.0.0"

  val laminar = "16.0.0"
  val waypoint = "7.0.0"
  val upickle = "3.1.3"

  val identusApollo = "1.8.0" // "1.7.1"
}

/** NPM Dependencies */
lazy val NPM = new { // When update the dependencies also update in package.json
  val sha256 = "js-sha256" -> "0.11.0"
  val jose = "jose" -> "5.10.0"

  // val elliptic = "elliptic" -> "6.6.1"
  // val ellipticType = "@types/elliptic" -> "6.4.18"

  val nobleCurves = "@noble/curves" -> "1.9.2"
  val appoloJS = "@hyperledger/identus-apollo" -> ("^" + V.identusApollo)

}

/** Dependencies */
lazy val D = new {

  /** The [[java.security.SecureRandom]] is used by the [[java.util.UUID.randomUUID()]] method in [[MsgId]].
    *
    * See more https://github.com/scala-js/scala-js-java-securerandom
    */
  val scalajsJavaSecureRandom = Def.setting(
    ("org.scala-js" %%% "scalajs-java-securerandom" % V.scalajsJavaSecureRandom)
      .cross(CrossVersion.for3Use2_13)
  )

  /* Depend on the scalajs-dom library. It provides static types for the browser DOM APIs. */
  val scalajsDom = Def.setting("org.scala-js" %%% "scalajs-dom" % V.scalajsDom)

  val zio = Def.setting("dev.zio" %%% "zio" % V.zio)
  val zioStreams = Def.setting("dev.zio" %%% "zio-streams" % V.zio)
  val zioJson = Def.setting("dev.zio" %%% "zio-json" % V.zioJson)
  val ziohttp = Def.setting("dev.zio" %% "zio-http" % V.zioHttp)
  val zioPrelude = Def.setting("dev.zio" %%% "zio-prelude" % V.zioPrelude)
  // val zioTest = Def.setting("dev.zio" %%% "zio-test" % V.zio % Test)
  // val zioTestSBT = Def.setting("dev.zio" %%% "zio-test-sbt" % V.zio % Test)
  // val zioTestMagnolia = Def.setting("dev.zio" %%% "zio-test-magnolia" % V.zio % Test)
  val zioMunitTest = Def.setting("com.github.poslegm" %%% "munit-zio" % V.zioMunitTest % Test)

  // Needed for ZIO
  val scalaJavaT = Def.setting("io.github.cquiroz" %%% "scala-java-time" % V.scalaJavaTime)
  val scalaJavaTZ = Def.setting("io.github.cquiroz" %%% "scala-java-time-tzdb" % V.scalaJavaTime)

  val bullet = Def.setting("io.bullet" %%% "borer-core" % V.bullet) // CBOR

  // Test DID comm
  // val didcomm = Def.setting("org.didcommx" % "didcomm" % "0.3.1")

  // Cryto
  // https://mvnrepository.com/artifact/com.nimbusds/nimbus-jose-jwt
  // https://bitbucket.org/connect2id/nimbus-jose-jwt/branches/compare/release-9.35%0Drelease-9.32
  val bouncycastle_bcprov = // https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on
    Def.setting("org.bouncycastle" % "bcprov-jdk18on" % V.bouncycastle)
  val bouncycastle_bcpkix = // https://mvnrepository.com/artifact/org.bouncycastle/bcpkix-jdk18on
    Def.setting("org.bouncycastle" % "bcpkix-jdk18on" % V.bouncycastle)

  val nimbusJoseJwt = Def.setting("com.nimbusds" % "nimbus-jose-jwt" % V.nimbusJoseJwt)

  val apollo = "org.hyperledger.identus" % "apollo-jvm" % V.identusApollo

  // For munit https://scalameta.org/munit/docs/getting-started.html#scalajs-setup
  val munit = Def.setting("org.scalameta" %%% "munit" % V.munit % Test)

  val laika = Def.setting("org.typelevel" %%% "laika-core" % V.laika) // JVM & JS

  // For WEBAPP
  val laminar = Def.setting("com.raquo" %%% "laminar" % V.laminar)
  val waypoint = Def.setting("com.raquo" %%% "waypoint" % V.waypoint)
  val upickle = Def.setting("com.lihaoyi" %%% "upickle" % V.upickle)
}

inThisBuild(
  Seq(
    // ### https://docs.scala-lang.org/scala3/guides/migration/options-new.html
    // ### https://docs.scala-lang.org/scala3/guides/migration/options-lookup.html
    scalacOptions ++=
      Seq("-encoding", "UTF-8") ++ // source files are in UTF-8
        Seq(
          "-deprecation", // warn about use of deprecated APIs
          "-unchecked", // warn about unchecked type parameters
          "-feature", // warn about misused language features (Note we are using 'language:implicitConversions')
          // TODO "-Yexplicit-nulls",
          // "-Ysafe-init", // https://dotty.epfl.ch/docs/reference/other-new-features/safe-initialization.html
          "-language:implicitConversions", // we can use with the flag '-feature'
          // NO NEED ATM "-language:reflectiveCalls",
          // "-Xprint-diff",
          // "-Xprint-diff-del",
          // "-Xprint-inline",
          // NO NEED ATM "-Xsemanticdb"
          // NO NEED ATM "-Ykind-projector"
        ) ++ {
          if (true) Seq("-Xfatal-warnings") else Seq("-rewrite", "-source", "3.4-migration")
        } ++
        // Because DeriveJson(Decoder/Encoder).gen[DidFail] exceeded maximal number of successive inlines (default is 32)
        Seq("-Xmax-inlines", "43")

      // ### commonSettings ###
      // Compile / doc / sources := Nil,
      // ### setupTestConfig ### //lazy val settingsFlags: Seq[sbt.Def.SettingsDefinition] = ???
      // libraryDependencies += D.munit.value, // BUG? "JS's Tests does not stop"
  )
)

lazy val setupTestConfig: Seq[sbt.Def.SettingsDefinition] = Seq(
  libraryDependencies ++= Seq(D.munit.value, D.zioMunitTest.value),
)
lazy val jsHeader =
  """/* FMGP scala-did examples and tool
    | * https://github.com/FabioPinheiro/scala-did
    | * Copyright: Fabio Pinheiro - fabiomgpinheiro@gmail.com
    | */""".stripMargin.trim() + "\n"

lazy val scalaJSLibConfigure: Project => Project =
  _.enablePlugins(ScalaJSPlugin)
    .enablePlugins(ScalablyTypedConverterGenSourcePlugin)
    .settings(
      stShortModuleNames := true, // ShortModuleNames
      stOutputPackage := "fmgp.typings", // shade into another package
      useYarn := true,
    )

lazy val scalaJSViteConfigure: Project => Project =
  _.enablePlugins(ScalaJSPlugin)
    .enablePlugins(ScalablyTypedConverterExternalNpmPlugin)
    .settings(
      /* Configure Scala.js to emit modules in the optimal way to
       * connect to Vite's incremental reload.
       * - emit ECMAScript modules
       * - emit as many small modules as possible for classes in the "livechart" package
       * - emit as few (large) modules as possible for all other classes
       *   (in particular, for the standard library)
       */
      scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule).withJSHeader(jsHeader) },
      // .withSourceMap(false) // disabled because it somehow triggers warnings and errors

      // Tell ScalablyTyped that we manage `npm install` ourselves
      externalNpm := rootPaths.value.apply("BASE").toFile(),
      // ShortModuleNames
      stShortModuleNames := true,
      // TODO REMOVE webpackBundlingMode := BundlingMode.LibraryAndApplication(), // BundlingMode.Application,
      // TODO useYarn := true
    )

lazy val buildInfoConfigure: Project => Project = _.enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoPackage := "fmgp",
    // buildInfoObject := "BuildInfo",
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      sbtVersion,
      // BuildInfoKey.action("buildTime") { System.currentTimeMillis }, // re-computed each time at compile
    ),
  )

addCommandAlias(
  "testJVM",
  ";didJVM/test; didCommProtocolsJVM/test; didFrameworkJVM/test; didImpJVM/test; " +
    "didResolverPeerJVM/test; didResolverPrismJVM/test; didResolverWebJVM/test; didUniresolverJVM/test; " +
    "multiformatsJVM/test"
)
addCommandAlias(
  "testJS",
  ";didJS/test;  didCommProtocolsJS/test;  didFrameworkJS/test;  didImpJS/test;  " +
    "didResolverPeerJS/test;  didResolverPrismJS/test;  didResolverWebJS/test;  didUniresolverJS/test;  " +
    "multiformatsJS/test"
)
addCommandAlias("testAll", ";testJVM;testJS")
addCommandAlias("docAll", "doc;docs/unidoc")
addCommandAlias("siteAll", "docs/mdoc;docs/laikaSite")
addCommandAlias("assemblyAll", "docAll;siteAll;installFrontend;fullPackAll;buildFrontend;demoJVM/assembly")
addCommandAlias("live", "fastPackAll;~demoJVM/reStart") // Missing the buildFrontend
addCommandAlias("ciJobLib", "compile;testAll")
addCommandAlias("ciJobFrontend", "installFrontend;fullPackAll;buildFrontend")

// Note fastPackAll and fullPackAll needs installFrontend (scala-did/node_modules/typescript/lib must exist)
addCommandAlias("fastPackAll", "serviceworker/fastLinkJS;webapp/fastLinkJS")
addCommandAlias("fullPackAll", "serviceworker/fullLinkJS;webapp/fullLinkJS")
lazy val installFrontend = taskKey[Unit]("Install all NPM package")
installFrontend := {
  val npmInstall = Process("npm" :: "install" :: Nil)
  val log = streams.value.log
  if ((npmInstall !) == 0) { log.success("NPM package install successful!") }
  else { throw new IllegalStateException("NPM package install failed!") }
}

lazy val buildFrontend = taskKey[Unit]("Execute frontend scripts")
buildFrontend := {
  // val npmInstall = Process("npm" :: "install" :: Nil)
  val npmBuild = Process("npm" :: "run" :: "build" :: Nil)
  val log = streams.value.log
  if (( /*npmInstall #&&*/ npmBuild !) == 0) { log.success("frontend build successful!") }
  else { throw new IllegalStateException("frontend build failed!") }
}

lazy val root = project
  .in(file("."))
  .settings(publish / skip := true)
  .aggregate(did.js, did.jvm) // publish
  .aggregate(didCommProtocols.js, didCommProtocols.jvm) // publish
  .aggregate(didFramework.js, didFramework.jvm) // publish
  .aggregate(didImp.js, didImp.jvm) // publish
  .aggregate(multiformats.js, multiformats.jvm) // publish
  .aggregate(didResolverPeer.js, didResolverPeer.jvm) // publish
  .aggregate(didResolverPrism.js, didResolverPrism.jvm) // publish
  .aggregate(didPrismNode) // NOT publish
  .aggregate(didResolverWeb.js, didResolverWeb.jvm) // publish
  .aggregate(didUniresolver.js, didUniresolver.jvm) // NOT publish
  .aggregate(docs) // just to aggregate the command clean

// Move to a new repository
lazy val all = project
  .in(file("allProjects"))
  .aggregate(root)
  .aggregate(didExperiments.js, didExperiments.jvm) // NOT publish
  .aggregate(didExample.js, didExample.jvm)
  .aggregate(serviceworker)
  .aggregate(webapp)
  .aggregate(demo.jvm, demo.js)

lazy val did = crossProject(JSPlatform, JVMPlatform)
  .in(file("did"))
  .configure(publishConfigure)
  .settings((setupTestConfig): _*)
  .settings(Test / scalacOptions -= "-Ysafe-init") // TODO REMOVE Cannot prove the method argument is hot.
  .settings(
    name := "did",
    libraryDependencies += D.zioJson.value,
    libraryDependencies += D.zio.value, // Just to force a version bump (including scalajsDom)
  )
  .jsConfigure(scalaJSLibConfigure)
  .jsSettings(libraryDependencies += D.scalajsDom.value) // just to really really force a version bump
  .jsSettings(libraryDependencies += D.scalajsJavaSecureRandom.value.cross(CrossVersion.for3Use2_13))
  .jsSettings(
    Compile / npmDependencies += NPM.sha256,
    /* say we want to minimize all but keep these very specific things */
    stMinimize := Selection.All,
    stMinimizeKeep ++= List(
      "jsSha256.mod.^",
      "jsSha256.mod.sha256",
      "std.global.TextEncoder",
    ),
  )
  .configure(docConfigure)

lazy val didCommProtocols = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-comm-protocols"))
  .configure(publishConfigure)
  .settings((setupTestConfig): _*)
  .settings(name := "did-comm-protocols")
  .jsConfigure(scalaJSLibConfigure) // Because of didJS now uses NPM libs
  .dependsOn(did % "compile;test->test")
  .configure(docConfigure)

lazy val didExperiments = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-experiments"))
  .settings(publish / skip := true)
  .settings(Test / scalacOptions -= "-Ysafe-init") // TODO REMOVE Cannot prove the method argument is hot.
  .settings(
    name := "did-experiments",
    libraryDependencies += D.zioPrelude.value, // just for the hash (is this over power?)
    libraryDependencies += D.zioMunitTest.value,
  )
  .dependsOn(did % "compile;test->test")
  .jsConfigure(scalaJSLibConfigure) // Because of didJS now uses NPM libs
  .configure(docConfigure)

lazy val didFramework = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-framework"))
  .configure(publishConfigure)
  .settings(
    name := "did-framework",
    libraryDependencies += D.zioMunitTest.value,
  )
  .dependsOn(did, didCommProtocols)
  .jvmSettings(libraryDependencies += D.ziohttp.value)
  .jsSettings(libraryDependencies += D.scalajsDom.value)
  .jsConfigure(scalaJSLibConfigure) // Because of didJS now uses NPM libs
  .configure(docConfigure)

lazy val didImp = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-imp"))
  .configure(publishConfigure)
  .settings((setupTestConfig): _*)
  .settings(Test / scalacOptions -= "-Ysafe-init") // TODO REMOVE Cannot prove the method argument is hot.
  .settings(name := "did-imp")
  .settings(libraryDependencies += D.zioMunitTest.value)
  .jvmSettings( // Add JVM-specific settings here
    libraryDependencies += D.bouncycastle_bcprov.value,
    libraryDependencies += D.bouncycastle_bcpkix.value,
    libraryDependencies += D.nimbusJoseJwt.value,

    // BUT have vulnerabilities in the dependencies: CVE-2023-2976
    libraryDependencies += "com.google.crypto.tink" % "tink" % "1.18.0", // https://mvnrepository.com/artifact/com.google.crypto.tink/tink/1.10.0
    // To fix vulnerabilitie https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-2976
    libraryDependencies += "com.google.protobuf" % "protobuf-java" % "4.31.1",
  )
  .jsConfigure(scalaJSLibConfigure)
  .jsSettings(
    Compile / npmDependencies += NPM.jose,
    /* say we want to minimize all but keep these very specific things */
    stMinimize := Selection.All,
    stMinimizeKeep ++= List(
      "jose.mod.^",
      "jose.mod.importJWK",
      // "jose.mod.jwtDecrypt",
      "jose.mod.jwtVerify",
      "jose.mod.generalVerify",
      "jose.mod.SignJWT",
      "jose.mod.GeneralSign",
      "jose.typesMod.JWK",
      "jose.typesMod.KeyLike",
      "jose.typesMod.CompactJWSHeaderParameters",
      "jose.typesMod.JWTHeaderParameters",
      "jose.typesMod.JWTPayload",
      "jose.typesMod.GeneralJWSInput",
      "jose.mod.generateKeyPair",
      "jose.mod.exportJWK",
      "jose.KeyGenerateKeyPairMod.GenerateKeyPairResult",
      "jose.KeyGenerateKeyPairMod.GenerateKeyPairOptions",
      "jose.mod.errors.JWSSignatureVerificationFailed",
    ),
  )
  .jsSettings( // Add JS-specific settings here
    // Test / scalaJSUseMainModuleInitializer := true, Test / scalaJSUseTestModuleInitializer := false, Test / mainClass := Some("fmgp.crypto.MainTestJS")
    Test / parallelExecution := false,
    Test / testOptions += Tests.Argument("--exclude-tags=JsUnsupported"),
  )
  .dependsOn(did % "compile;test->test")
  .configure(docConfigure)

/** This is a copy of https://github.com/fluency03/scala-multibase to support crossProject
  *
  * "com.github.fluency03" % "scala-multibase_2.12" % "0.0.1"
  */
lazy val multiformats =
  crossProject(JSPlatform, JVMPlatform)
    .in(file("multiformats"))
    .configure(publishConfigure)
    .settings(Test / scalacOptions -= "-Ysafe-init") // TODO REMOVE Cannot prove the method argument is hot.
    .settings(
      name := "multiformats",
      libraryDependencies += D.munit.value,
      libraryDependencies += D.zioMunitTest.value,
    )
    .configure(docConfigure)

lazy val didResolverPeer = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-method-peer"))
  .configure(publishConfigure)
  .settings(
    name := "did-method-peer",
    libraryDependencies += D.munit.value,
    libraryDependencies += D.zioMunitTest.value,
  )
  .jvmSettings( // See dependencyTree ->  didResolverPeerJVM/Test/dependencyTree
    libraryDependencies += "org.didcommx" % "didcomm" % "0.3.2" % Test,
    libraryDependencies += "org.didcommx" % "peerdid" % "0.5.0" % Test,
    resolvers += "jitpack" at "https://jitpack.io", // For java-multibase
    libraryDependencies += "com.github.multiformats" % "java-multibase" % "1.2.0" % Test, // FIX CVE-2020-15250 in peerdid
    libraryDependencies += "org.bouncycastle" % "bcprov-jdk18on" % "1.81" % Test,
    libraryDependencies += "org.bouncycastle" % "bcpkix-jdk18on" % "1.81" % Test,
    libraryDependencies += D.nimbusJoseJwt.value % Test,
    // libraryDependencies += "com.nimbusds" % "nimbus-jose-jwt" % "9.16-preview.1" % Test,
  )
  .jsConfigure(scalaJSLibConfigure)
  .dependsOn(did, multiformats)
  .dependsOn(didImp % "test->test") // To generate keys for tests
  .configure(docConfigure)

lazy val didResolverPrism = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-method-prism"))
  .configure(publishConfigure)
  .settings(
    name := "did-method-prism",
    libraryDependencies += D.bullet.value,
    libraryDependencies += D.munit.value,
    libraryDependencies += D.zioMunitTest.value,
  )
  .jvmSettings(libraryDependencies += D.ziohttp.value)
  .jvmSettings( // Add JVM-specific settings here
    libraryDependencies += D.bouncycastle_bcprov.value,
    libraryDependencies += D.bouncycastle_bcpkix.value,
    libraryDependencies += D.nimbusJoseJwt.value,
    // BUT have vulnerabilities in the dependencies: CVE-2023-2976
    libraryDependencies += "com.google.crypto.tink" % "tink" % "1.18.0", // https://mvnrepository.com/artifact/com.google.crypto.tink/tink/1.10.0
    // To fix vulnerabilitie https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-2976
    libraryDependencies += "com.google.protobuf" % "protobuf-java" % "4.29.5",
    // https://oss.sonatype.org/#nexus-search;quick~com.bloxbean.cardano
    libraryDependencies += "com.bloxbean.cardano" % "cardano-client-lib" % "0.6.6",
    libraryDependencies += "com.bloxbean.cardano" % "cardano-client-backend-blockfrost" % "0.6.6",
  )
  .jvmSettings( // PoC for a prism-cli tooling // TODO Move to a new repo
    // assembly / mainClass := Some("fmgp.did.method.prism.vdr.Indexer"),
    // assembly / assemblyJarName := "prism-Indexer.jar",

    // run / fork := true,
    // run / connectInput := true,
    libraryDependencies += "dev.zio" %% "zio-cli" % "0.7.2",
    assembly / mainClass := Some("fmgp.did.method.prism.cli.PrismCli"),
    assembly / assemblyJarName := "cardano-prism.jar",
  )
  .settings(
    Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"),
    // Compile / PB.protoSources := Seq(file("did-method-prism/shared/src/main/protobuf")), // to avoid the https://github.com/epfl-lara/smart/blob/master/.sbtopts (line 1)
    // Compile / PB.protoSources := Seq(baseDirectory.value / "shared/src/main/protobuf"), // /Users/fabio/workspace/scala-did/did-method-prism/jvm/shared/src/main/protobuf
    Compile / PB.protoSources := Seq(
      rootPaths.value.apply("BASE").toFile() / "did-method-prism" / "shared/src/main/protobuf"
    ),
    // (optional) If you need scalapb/scalapb.proto or anything from google/protobuf/*.proto
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
      // The following needed only if you include scalapb/scalapb.proto:
      "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
    )
  )
  .jsConfigure(scalaJSLibConfigure)
  .jsSettings(
    // stReactEnableTreeShaking := Selection.NoneExcept("@noble/curves"),
    // stFlavour := Flavour.Slinky,
    Compile / npmDependencies ++= Seq(NPM.nobleCurves),
    // Compile / npmDependencies ++= Seq(NPM.elliptic, NPM.ellipticType),
    // stMinimize := Selection.All,
    // stMinimizeKeep ++= List(..
    // https://developers.cardano.org/docs/get-started/cardano-serialization-lib/overview/
  )
  // Apollo
  .jvmSettings(libraryDependencies += D.apollo)
  // .jsSettings(
  //   Compile / npmDependencies ++= Seq(NPM.appoloJS),
  //   stIgnore += "node",
  // )
  .dependsOn(did, multiformats)
  .configure(docConfigure)

lazy val didPrismNode = project
  .in(file("did-method-prism-node"))
  .settings(publish / skip := true)
  .settings(
    name := "prism-node",
    libraryDependencies += D.munit.value,
    libraryDependencies += D.zioMunitTest.value,
    libraryDependencies += "dev.zio" %% "zio-logging-slf4j2-bridge" % "2.5.1"
  )
  .settings(
    Compile / PB.targets := Seq(
      scalapb.gen(grpc = true) -> (Compile / sourceManaged).value / "scalapb",
      scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value / "scalapb",
    ),
    Compile / PB.protoSources := Seq(
      rootPaths.value.apply("BASE").toFile() / "did-method-prism-node" / "src/main/protobuf"
    ),
    libraryDependencies ++= Seq(
      "io.grpc" % "grpc-netty" % "1.73.0", // https://mvnrepository.com/artifact/io.grpc/grpc-netty
      "io.netty" % "netty-handler" % "4.2.3.Final", // This is to forces a update in from "io.grpc" % "grpc-netty" % "1.73.0" -> https://mvnrepository.com/artifact/io.netty/netty-handler/4.1.110.Final
      // REMOVE // "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
      // REMOVE // The following needed only if you include scalapb/scalapb.proto:
      // REMOVE // "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "com.thesamet.scalapb" %%% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
    ),
    assembly / mainClass := Some("fmgp.prism.Node"), // TODO Move to a new repo
    assembly / assemblyJarName := "prism-node.jar", // TODO Move to a new repo
    run / fork := false
  )
  .dependsOn(didResolverPrism.jvm)

//https://w3c-ccg.github.io/did-method-web/
lazy val didResolverWeb = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-method-web"))
  .configure(notYetPublishedConfigure)
  .settings(
    name := "did-method-web",
    libraryDependencies += D.munit.value,
    libraryDependencies += D.zioMunitTest.value,
  )
  .jvmSettings(libraryDependencies += D.ziohttp.value)
  .jvmSettings( // FIXME https://github.com/zio/zio-http/issues/2280#issuecomment-2166894061
    Test / fork := true,
    Test / javaOptions := Seq("-Djava.net.preferIPv4Stack=true"),
  )
  .dependsOn(did)
  .configure(docConfigure)

//https://dev.uniresolver.io/
lazy val didUniresolver = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-uniresolver"))
  .settings(publish / skip := true)
  .configure(notYetPublishedConfigure)
  .settings(
    name := "did-uniresolver",
    libraryDependencies += D.munit.value,
    libraryDependencies += D.zioMunitTest.value,
  )
  .jvmSettings(libraryDependencies += D.ziohttp.value)
  // .enablePlugins(ScalaJSBundlerPlugin).jsSettings(Test / npmDependencies += "node-fetch" -> "3.3.0")
  .jsSettings( // TODO https://scalacenter.github.io/scalajs-bundler/reference.html#jsdom
    libraryDependencies += D.scalajsDom.value,
    // jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
    // Test / requireJsDomEnv := true,
  )
  .dependsOn(did)
  .configure(docConfigure)

lazy val serviceworker = project
  .in(file("serviceworker"))
  .settings(publish / skip := true)
  .settings(name := "fmgp-serviceworker")
  .enablePlugins(ScalaJSPlugin) // Enable the Scala.js plugin in this project
  .settings(
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("fmgp.serviceworker")))
        .withJSHeader(jsHeader)
    },
    scalaJSModuleInitializers := Seq( // scalaJSUseMainModuleInitializer := true,
      ModuleInitializer.mainMethod("fmgp.serviceworker.SW", "main").withModuleID("sw")
    ),
    libraryDependencies += D.scalajsDom.value,
    libraryDependencies ++= Seq(D.zio.value, D.zioJson.value),
  )

lazy val webapp = project
  .in(file("webapp"))
  .settings(publish / skip := true)
  .settings(name := "fmgp-webapp")
  .configure(scalaJSViteConfigure)
  .settings(
    scalaJSLinkerConfig ~= {
      _.withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("fmgp.webapp")))
    },
    Compile / scalaJSModuleInitializers += {
      ModuleInitializer.mainMethod("fmgp.webapp.App", "main").withModuleID("webapp")
    },
  )
  .configure(buildInfoConfigure)
  .settings(
    libraryDependencies ++= Seq(D.laminar.value, D.waypoint.value, D.upickle.value),
    libraryDependencies ++= Seq(D.zio.value, D.zioJson.value),
  )
  // .settings( // for doc
  //   libraryDependencies += D.laika.value,
  //   Compile / sourceGenerators += {
  //     val needThis: Task[Unit] = (docs / mdoc).toTask("").taskValue
  //     val generateCode: Task[Seq[File]] = makeDocSources.taskValue
  //     needThis.flatMap(unit => generateCode)
  //     // I have no clue what I did here but types match and its working =)
  //   },
  // )
  .dependsOn(didExample.js)
  .dependsOn(serviceworker)

lazy val didExample = crossProject(JSPlatform, JVMPlatform)
  .in(file("did-example"))
  .settings(publish / skip := true)
  .dependsOn(did, didImp, didFramework, didResolverPeer, didResolverPrism, didResolverWeb, didUniresolver)

lazy val demo = crossProject(JSPlatform, JVMPlatform)
  .in(file("demo"))
  .settings(publish / skip := true)
  .settings(
    name := "did-demo",
    libraryDependencies += D.zioStreams.value,
    libraryDependencies += D.munit.value,
    libraryDependencies += D.zioMunitTest.value,
    libraryDependencies += D.laika.value,
  )
  .jvmSettings(
    reStart / mainClass := Some("fmgp.did.demo.AppServer"),
    assembly / mainClass := Some("fmgp.did.demo.AppServer"),
    assembly / assemblyJarName := "scala-did-demo-server.jar",
    libraryDependencies += D.ziohttp.value,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "src" / "main" / "extra-resources",
    Compile / unmanagedResourceDirectories += rootPaths.value.apply("BASE").toFile() / "docs" / "target" / "scaladoc",
    // Compile / unmanagedResourceDirectories += rootPaths.value.apply("BASE").toFile() / "docs" / "target" / "mdoc"
    Compile / unmanagedResourceDirectories += rootPaths.value.apply("BASE").toFile() / "docs" / "target" / "site",
    Compile / unmanagedResourceDirectories += rootPaths.value.apply("BASE").toFile() / "vite" / "dist",
  )
  .dependsOn(did, didImp, didFramework, didResolverPeer, didResolverWeb, didUniresolver, didExample)

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
//   case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
//   case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
//   case "application.conf"                            => MergeStrategy.concat
//   case "unwanted.txt"                                => MergeStrategy.discard
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

/** Copy the Documentation and Generate an Scala object to Store */
def makeDocSources = Def
  .task {
    val resourceFolder = rootPaths.value.apply("BASE").toFile() / "docs" / "target" / "mdoc"
    val log = streams.value.log
    def processFiles(files: Seq[String]): Seq[(String, String)] = files.flatMap { fileName =>
      val resourceFile = rootPaths.value.apply("BASE").toFile() / "docs" / "target" / "mdoc" / fileName
      val originalFile = rootPaths.value.apply("BASE").toFile() / "docs" / "src" / fileName // "readme.md"

      if (resourceFile.isDirectory()) {
        processFiles(resourceFile.list().map(fileName + "/" + _).toSeq)
      } else {

        // TODO do the if
        // if (!sourceFile.exists() || sourceFile.lastModified() < resourceFile.lastModified()) {
        val file =
          if (resourceFile.exists()) resourceFile
          else {
            log.warn("makeDocSources: the resourceFile does not exists. Using the originalFile")
            originalFile
          }
        val contentREAMDE = IO
          .read(file)
          .replaceAllLiterally("$", "$$")
          .replaceAllLiterally("\"\"\"", "\"\"$\"")
        // }
        val valName = "_" + fileName.toLowerCase.replace(".", "_").replace("-", "_").replace("/", "_")

        Seq(
          (
            s"""    "${fileName.toLowerCase}" -> $valName""",
            s"""  final val $valName = raw\"\"\"$contentREAMDE\"\"\""""
          )
        )
      }
    }

    val aux = processFiles(resourceFolder.list().toSeq)

    val sourceDir = (Compile / sourceManaged).value
    val sourceFile = sourceDir / "DocSource.scala"
    val scalaCode = s"""
        |package fmgp.did
        |object DocSource {
        |
        |${aux.map(_._2).mkString("\n\n")}
        |
        |  final val all = Map(
        |${aux.map(_._1).mkString(",\n")}
        |  )
        |
        |}""".stripMargin
    IO.write(sourceFile, scalaCode)
    Seq(sourceFile)
  }
