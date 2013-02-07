name := "sbt-s3"

description := "S3 Plugin for sbt"

version := "0.3"

scalaVersion := "2.9.2"

organization := "com.typesafe.sbt"

organizationName := "Typesafe"

sbtPlugin := true

startYear := Some(2013)

libraryDependencies ++= Seq("com.amazonaws" % "aws-java-sdk" % "1.3.29",
                            "commons-lang" % "commons-lang" % "2.6")

scalacOptions in (Compile,doc) <++= (name,description,version,sourceDirectory) map {(n,d,v,s) =>
   Opts.doc.title(n+": "+d) ++ Opts.doc.version(v) ++ Seq("-doc-root-content", (s / "main/rootdoc.txt").getAbsolutePath())}

publishMavenStyle := false

publishTo <<= isSnapshot(if (_) Some(Classpaths.sbtPluginSnapshots) else Some(Classpaths.sbtPluginReleases))

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
