name := "SwayDB.benchmark"

version := "0.1"

scalaVersion := "2.13.1"

//resolvers += Opts.resolver.sonatypeReleases

libraryDependencies ++=
  Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "io.swaydb" %% "swaydb" % "0.12-RC3",
    "org.rocksdb" % "rocksdbjni" % "6.26.1"
  )

mainClass in assembly := Some("swaydb.benchmark.app.Start")

assemblyOutputPath in assembly := baseDirectory.value.toPath.resolve("swaydb-benchmark.jar").toFile
