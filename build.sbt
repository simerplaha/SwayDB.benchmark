name := "SwayDB.benchmark"

version := "0.1"

scalaVersion := "2.12.4"

//resolvers += Opts.resolver.sonatypeSnapshots

libraryDependencies ++=
  Seq(
    "io.swaydb" %% "swaydb" % "0.1-SNAPSHOT",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
  )

mainClass in assembly := Some("benchmark.Start")

assemblyOutputPath in assembly := baseDirectory.value.toPath.resolve("swaydb-benchmark.jar").toFile