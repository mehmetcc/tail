val TapirVersion = "1.11.9"

val ZioTestVersion    = "2.1.6"
val ZioLoggingVersion = "2.3.0"
val ZioConfigVersion  = "4.0.2"

lazy val rootProject = (project in file(".")).settings(
  Seq(
    name         := "tail",
    version      := "0.1.0-SNAPSHOT",
    organization := "org.mehmetcc",
    scalaVersion := "2.13.14",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir"   %% "tapir-zio-http-server"    % TapirVersion,
      "com.softwaremill.sttp.tapir"   %% "tapir-prometheus-metrics" % TapirVersion,
      "com.softwaremill.sttp.tapir"   %% "tapir-json-zio"           % TapirVersion,
      "ch.qos.logback"                 % "logback-classic"          % "1.5.12",
      "dev.zio"                       %% "zio-logging"              % ZioLoggingVersion,
      "dev.zio"                       %% "zio-logging-slf4j"        % ZioLoggingVersion,
      "dev.zio"                       %% "zio-config"               % ZioConfigVersion,
      "dev.zio"                       %% "zio-config-typesafe"      % ZioConfigVersion,
      "dev.zio"                       %% "zio-config-magnolia"      % ZioConfigVersion,
      "dev.zio"                       %% "zio-kafka"                % "2.7.4",
      "com.softwaremill.sttp.tapir"   %% "tapir-sttp-stub-server"   % TapirVersion   % Test,
      "dev.zio"                       %% "zio-test"                 % ZioTestVersion % Test,
      "dev.zio"                       %% "zio-test-sbt"             % ZioTestVersion % Test,
      "com.softwaremill.sttp.client3" %% "zio-json"                 % "3.10.1"       % Test
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
)
