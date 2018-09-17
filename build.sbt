name := "sbts3"

description := "S3 Plugin for sbt"

version := "0.10.4-SNAPSHOT"

isSnapshot := true

organization := "cf.janga"

organizationName := "Janga"

startYear := Some(2013)

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)

libraryDependencies ++= Seq("com.amazonaws" % "aws-java-sdk-s3" % "1.11.409",
                            "commons-lang" % "commons-lang" % "2.6")

scalacOptions in (Compile, doc) ++=
  Opts.doc.title(name.value + ": " + description.value) ++
  Opts.doc.version(version.value) ++
  Seq("-doc-root-content", (sourceDirectory.value / "main/rootdoc.txt").getAbsolutePath())

publishMavenStyle := false

crossSbtVersions := Seq("0.13.17", "1.1.6")

bintrayRepository := "sbt-plugins"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0"))

bintrayOrganization := None
