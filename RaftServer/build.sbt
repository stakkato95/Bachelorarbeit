name := "RaftServer"

version := "1.0"

lazy val `raftserver` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"

scalaVersion := "2.13.4"
val AkkaVersion = "2.6.10"

libraryDependencies ++= Seq(
  jdbc,
  ehcache,
  ws,
  specs2 % Test,
  guice,
  "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion
)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

      