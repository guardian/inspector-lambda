scalaVersion := "2.12.4"

name := "inspector-lambda"
organization := "com.gu"

val awsSdkVersion = "1.12.484"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-events" % "3.11.1",
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.2",
  "com.amazonaws" % "aws-java-sdk-lambda" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-config" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-ec2" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-inspector" % awsSdkVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "ch.qos.logback" %  "logback-classic" % "1.4.7",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
  "org.scalacheck" %% "scalacheck" % "1.17.0" % Test
)

scalacOptions := Seq("-unchecked", "-deprecation")
assemblyJarName in assembly := s"inspector-lambda.jar"
assemblyMergeStrategy in assembly := {
    case PathList(ps @ _*) if ps.last endsWith "module-info.class" => MergeStrategy.first
    case other: Any => MergeStrategy.defaultMergeStrategy(other)
  }