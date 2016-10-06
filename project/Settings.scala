import scala.language.postfixOps

import sbt._
import sbt.Keys._

object Settings extends Build {

  lazy val buildSettings = Seq(
    name := "Wasp",
    normalizedName := "Wasp",
    organization := "it.agilelab.bigdata.wasp",
    organizationHomepage := Some(url("http://www.agilelab.it")),
    scalaVersion := Versions.Scala,
    homepage := Some(url("http://www.agilelab.it"))
    //licenses := Seq(("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
  )

  override lazy val settings = super.settings ++ buildSettings

  val parentSettings = buildSettings ++ Seq(
    publishArtifact := false,
    publish := {}
  )
	
	val customResolvers = Seq("gphat" at "https://raw.github.com/gphat/mvn-repo/master/releases/",
		"Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/", Resolver.mavenLocal,
    "Bintray" at "https://dl.bintray.com/agile-lab-dev/Spark-Solr/",
    "Restlet Maven repository" at "https://maven.restlet.com/",
		Resolver.sonatypeRepo("releases"))

  lazy val defaultSettings = testSettings ++ Seq(
	  resolvers ++= customResolvers,
    autoCompilerPlugins := true,
    // export jars instead of classes for run/etc (needed to export wasp jars into spark-lib/managed)
    exportJars := true,
      // removed "-Xfatal-warnings" as temporary workaround for log4j fatal error.
    scalacOptions ++= Seq("-encoding", "UTF-8", s"-target:jvm-${Versions.JDK}", "-feature", "-language:_", "-deprecation", "-unchecked", "-Xlint"),
    javacOptions in Compile ++= Seq("-encoding", "UTF-8", "-source", Versions.JDK, "-target", Versions.JDK, "-Xlint:deprecation", "-Xlint:unchecked"),
    run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run)),
    ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet,
    parallelExecution in ThisBuild := false,
    parallelExecution in Global := false
  )

  val tests = inConfig(Test)(Defaults.testTasks) ++ inConfig(IntegrationTest)(Defaults.itSettings)

  val testOptionSettings = Seq(
    Tests.Argument(TestFrameworks.ScalaTest, "-oDF")
  )

  lazy val testSettings = tests ++ Seq(
    parallelExecution in Test := false,
    parallelExecution in IntegrationTest := false,
    testOptions in Test ++= testOptionSettings,
    testOptions in IntegrationTest ++= testOptionSettings,
    fork in Test := true,
    fork in IntegrationTest := true,
    (compile in IntegrationTest) <<= (compile in Test, compile in IntegrationTest) map { (_, c) => c },
    managedClasspath in IntegrationTest <<= Classpaths.concat(managedClasspath in IntegrationTest, exportedProducts in Test)
  )


}
