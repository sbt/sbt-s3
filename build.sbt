name := "sbts3"

description := "S3 Plugin for sbt"

version := "0.10.3-SNAPSHOT"

isSnapshot := true

organization := "cf.janga"

organizationName := "Janga"

sbtPlugin := true

startYear := Some(2013)

libraryDependencies ++= Seq("com.amazonaws" % "aws-java-sdk-s3" % "1.11.29",
                            "commons-lang" % "commons-lang" % "2.6")

scalacOptions in (Compile, doc) ++=
  Opts.doc.title(name.value + ": " + description.value) ++
  Opts.doc.version(version.value) ++
  Seq("-doc-root-content", (sourceDirectory.value / "main/rootdoc.txt").getAbsolutePath())

publishMavenStyle := false

crossSbtVersions := Seq("0.13.16", "1.0.2")

bintrayRepository := "sbt-plugins"

licenses += ("BSD", url("http://directory.fsf.org/wiki/License:BSD_4Clause"))

bintrayOrganization := None
