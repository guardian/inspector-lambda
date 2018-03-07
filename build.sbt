scalaVersion := "2.12.4"

val softwareVersion = "0.1.0" // If updating, also update the version number in deploy-lambda.sh

name := "inspector-lambda"
organization := "com.gu"
version := softwareVersion

val awsSdkVersion = "1.11.258"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-events" % "1.3.0",
  "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
  "com.amazonaws" % "aws-java-sdk-lambda" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-config" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-ec2" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-inspector" % awsSdkVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0",
  "ch.qos.logback" %  "logback-classic" % "1.1.7",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.4" % Test
)

scalacOptions := Seq("-unchecked", "-deprecation")
assemblyJarName in assembly := s"inspector-lambda-${softwareVersion}.jar"
