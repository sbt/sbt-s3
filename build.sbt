name := "sbt-s3"

description := "S3 Plugin for sbt"

version := "0.9-SNAPSHOT"

isSnapshot := true

organization := "com.typesafe.sbt"

organizationName := "Typesafe"

sbtPlugin := true

startYear := Some(2013)

libraryDependencies ++= Seq("com.amazonaws" % "aws-java-sdk-s3" % "1.11.29",
                            "commons-lang" % "commons-lang" % "2.6")

scalacOptions in (Compile,doc) <++= (name,description,version,sourceDirectory) map {(n,d,v,s) =>
   Opts.doc.title(n+": "+d) ++ Opts.doc.version(v) ++ Seq("-doc-root-content", (s / "main/rootdoc.txt").getAbsolutePath())}

publishMavenStyle := false

scalaVersion in Global := "2.10.6"

bintrayRepository := "sbt-plugins"

licenses += ("BSD", url("http://directory.fsf.org/wiki/License:BSD_4Clause"))

bintrayOrganization := None
