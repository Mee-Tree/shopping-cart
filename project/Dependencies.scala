import sbt._

object Dependencies {

  object V {
    val cats              = "2.9.0"
    val `cats-effect`     = "3.4.9"
    val `cats-retry`      = "3.1.0"
    val circe             = "0.14.2"
    val ciris             = "2.3.2"
    val derevo            = "0.13.0"
    val fs2               = "3.2.14"
    val http4s            = "0.23.1"
    val `http4s-jwt-auth` = "1.2.2"
    val `javax-crypto`    = "1.0.1"
    val monocle           = "3.2.0"
    val newtype           = "0.4.4"
    val redis4cats        = "1.4.1"
    val refined           = "0.9.29"
    val skunk             = "0.3.2"
    val squants           = "1.8.3"

    // Logging
    val logback  = "1.4.7"
    val log4cats = "2.6.0"

    // Testing
    val weaver = "0.8.3"

    // Scalafix
    val `organize-imports` = "0.6.0"

    // Compiler plugins
    val `better-monadic-for` = "0.3.1"
    val `kind-projector`     = "0.13.2"

  }

  object Libs {
    def cats(artifact: String)       = "org.typelevel"       %% s"cats-$artifact"       % V.cats
    def circe(artifact: String)      = "io.circe"            %% s"circe-$artifact"      % V.circe
    def ciris(artifact: String)      = "is.cir"              %% artifact                % V.ciris
    def derevo(artifact: String)     = "tf.tofu"             %% s"derevo-$artifact"     % V.derevo
    def http4s(artifact: String)     = "org.http4s"          %% s"http4s-$artifact"     % V.http4s
    def monocle(artifact: String)    = "dev.optics"          %% s"monocle-$artifact"    % V.monocle
    def redis4cats(artifact: String) = "dev.profunktor"      %% s"redis4cats-$artifact" % V.redis4cats
    def refined(artifact: String)    = "eu.timepit"          %% artifact                % V.refined
    def skunk(artifact: String)      = "org.tpolecat"        %% s"skunk-$artifact"      % V.skunk
    def log4cats(artifact: String)   = "org.typelevel"       %% s"log4cats-$artifact"   % V.log4cats
    def weaver(artifact: String)     = "com.disneystreaming" %% s"weaver-$artifact"     % V.weaver

    val `cats-core`   = cats("core")
    val `cats-effect` = "org.typelevel"    %% "cats-effect" % V.`cats-effect`
    val `cats-retry`  = "com.github.cb372" %% "cats-retry"  % V.`cats-retry`

    val `monocle-core` = monocle("core")
    val fs2            = "co.fs2"        %% "fs2-core" % V.fs2
    val newtype        = "io.estatico"   %% "newtype"  % V.newtype
    val squants        = "org.typelevel" %% "squants"  % V.squants

    val `circe-core`    = circe("core")
    val `circe-parser`  = circe("parser")
    val `circe-generic` = circe("generic")
    val `circe-refined` = circe("refined")

    val `ciris-core`       = ciris("ciris")
    val `ciris-enumeratum` = ciris("ciris-enumeratum")
    val `ciris-refined`    = ciris("ciris-refined")

    val `derevo-core`  = derevo("core")
    val `derevo-cats`  = derevo("cats")
    val `derevo-circe` = derevo("circe-magnolia")

    val `http4s-dsl`    = http4s("dsl")
    val `http4s-circe`  = http4s("circe")
    val `http4s-client` = http4s("ember-client")
    val `http4s-server` = http4s("ember-server")

    val `http4s-jwt-auth` = "dev.profunktor"  %% "http4s-jwt-auth" % V.`http4s-jwt-auth`
    val `javax-crypto`    = "javax.xml.crypto" % "jsr105-api"      % V.`javax-crypto`

    val `redis4cats-effects`  = redis4cats("effects")
    val `redis4cats-log4cats` = redis4cats("log4cats")

    val `refined-core` = refined("refined")
    val `refined-cats` = refined("refined-cats")

    val `skunk-core`  = skunk("core")
    val `skunk-circe` = skunk("circe")

    // Logging
    val logback          = "ch.qos.logback" % "logback-classic" % V.logback
    val `log4cats-slf4j` = log4cats("slf4j")
    val `log4cats-noop`  = log4cats("noop")

    // Testing
    val `cats-laws`          = cats("laws")
    val `monocle-law`        = monocle("law")
    val `refined-scalacheck` = refined("refined-scalacheck")
    val `weaver-scalacheck`  = weaver("scalacheck")
    val `weaver-discipline`  = weaver("discipline")
    val `weaver-cats`        = weaver("cats")

    // Scalafix
    val `organize-imports` = "com.github.liancheng" %% "organize-imports" % V.`organize-imports`

    val core = Seq(
      `cats-core`,
      `cats-effect`,
      `cats-retry`,
      `circe-core`,
      `circe-generic`,
      `circe-parser`,
      `circe-refined`,
      `ciris-core`,
      `ciris-enumeratum`,
      `ciris-refined`,
      `derevo-core`,
      `derevo-cats`,
      `derevo-circe`,
      fs2,
      `http4s-dsl`,
      `http4s-circe`,
      `http4s-client`,
      `http4s-server`,
      `http4s-jwt-auth`,
      `javax-crypto`,
      `monocle-core`,
      newtype,
      `redis4cats-effects`,
      `redis4cats-log4cats`,
      `refined-core`,
      `refined-cats`,
      squants,
      `skunk-core`,
      `skunk-circe`,
      `log4cats-slf4j`,
      logback % Runtime
    )

    val test = Seq(
      `cats-laws`,
      `monocle-law`,
      `refined-scalacheck`,
      `weaver-scalacheck`,
      `weaver-discipline`,
      `weaver-cats`,
      `log4cats-noop`
    )
      .map(_ % Test)

    val scalafix = Seq(`organize-imports`)
  }

  object CompilerPlugins {
    val `better-monadic-for` = "com.olegpy" %% "better-monadic-for" % V.`better-monadic-for`
    val `kind-projector`     =
      "org.typelevel" % "kind-projector" % V.`kind-projector` cross CrossVersion.full

    val core = Seq(`better-monadic-for`, `kind-projector`)
      .map(compilerPlugin)

    val test = core
  }
}
