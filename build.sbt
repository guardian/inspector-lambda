scalaVersion := "2.12.4"

name := "inspector-lambda"
organization := "com.gu"
version := "0.1.0"

val awsSdkVersion = "1.11.258"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-events" % "1.3.0",
  "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
  "com.amazonaws" % "aws-java-sdk-lambda" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-config" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-ec2" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-elasticloadbalancing" % awsSdkVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0",
  "ch.qos.logback" %  "logback-classic" % "1.1.7"
)

scalacOptions := Seq("-unchecked", "-deprecation")
assemblyJarName in assembly := "inspector-lambda.jar"
