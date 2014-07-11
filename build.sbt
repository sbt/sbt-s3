import bintray.Keys._

name := "sbt-s3"

description := "S3 Plugin for sbt"

version := "0.8"

organization := "com.typesafe.sbt"

organizationName := "Typesafe"

sbtPlugin := true

startYear := Some(2013)

libraryDependencies ++= Seq("com.amazonaws" % "aws-java-sdk" % "1.3.29",
                            "commons-lang" % "commons-lang" % "2.6")

scalacOptions in (Compile,doc) <++= (name,description,version,sourceDirectory) map {(n,d,v,s) =>
   Opts.doc.title(n+": "+d) ++ Opts.doc.version(v) ++ Seq("-doc-root-content", (s / "main/rootdoc.txt").getAbsolutePath())}

publishMavenStyle := false

sbtVersion in Global := "0.13.1"

scalaVersion in Global := "2.10.3"

bintrayPublishSettings

repository in bintray := "sbt-plugins"

licenses += ("BSD", url("http://directory.fsf.org/wiki/License:BSD_4Clause"))

bintrayOrganization in bintray := None
