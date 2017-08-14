versionWithGit

name := "akka-http-fetcher"

version := "0.0.1"

scalaVersion := "2.12.3"

scalacOptions ++= List(
  "-Xlint",
  "-feature",
  "-Xmax-classfile-name", "200",
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused",
  "-Ywarn-unused-import"
//  ,
//  "-Xfatal-warnings"

)

libraryDependencies ++= {
  val akkaV = "2.4.19"
  val akkaHttpV = "10.0.9"
  val logbackContribV = "0.1.4"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV % "test",
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "ch.qos.logback" % "logback-classic" % "1.1.7"
  )
}

mainClass in sbt.Global := Some("org.fbc.experiments.crawler.Main")

assemblyMergeStrategy in assembly := {
  case "application.conf" => MergeStrategy.first
  case x: Any =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

assemblyJarName in assembly := "application.jar"
