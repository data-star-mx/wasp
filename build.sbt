import com.typesafe.sbt.packager.MappingsHelper._

name := "wasp"

version := "1.0"

scalaVersion := "2.10.6"

// populate spark-lib
libraryDependencies ++= Dependencies.sparkLibJava ++ Dependencies.sparkLibScala
// define task
lazy val populateSparkLib = taskKey[Seq[File]]("Populates the spark-lib directory")
populateSparkLib := {
  val log = streams.value.log
  val sparkLibPath = "spark-lib/managed/"
  log.info("Populating spark-lib directory: \"" + sparkLibPath + "\".")
  
  log.info("Cleaning spark-lib... ")
  IO.delete(new File(sparkLibPath))
  log.info("Done cleaning.")
  
  log.info("Adding dependency jars to spark-lib...")
  val sparkLibModules = Dependencies.sparkLibJava.map(m => (m.organization, m.name)).toSet ++
    Dependencies.sparkLibScala.map(m => (m.organization, m.name + "_2.10")).toSet // TODO fix hardcoded scala version
  val dummyModuleID = new ModuleID("", "", "", None)
  val sparkLibDependencies = (dependencyClasspath in Compile).value.filter {
      dep =>
        val m = dep.metadata.get(AttributeKey[ModuleID]("module-id")).getOrElse(dummyModuleID)
        sparkLibModules((m.organization, m.name))
    }
  val sparkLibJarFiles = sparkLibDependencies.map {
      dep => dep.data.getAbsoluteFile
    }
  log.info("Adding: " + sparkLibJarFiles.mkString(","))
  IO.copy(sparkLibJarFiles.map(jar => (jar, new File(sparkLibPath + jar.getName))))
  log.info("Done adding dependency jars.")
  
  log.info("Adding WASP jars to spark-lib...")
  val waspProducts = (exportedProducts in Compile in Core).value ++
    (exportedProducts in Compile in Consumers).value ++
    (exportedProducts in Compile in Producers).value
  val waspJarFiles = waspProducts.map {
    dep => dep.data.getAbsoluteFile
  }
  log.info("Adding: " + waspJarFiles.mkString(","))
  IO.copy(waspJarFiles.map(jar => (jar, new File(sparkLibPath + jar.getName))))
  log.info("Done adding WASP jars.")
  
  Seq.empty[File]
}
// add task to resource generators
resourceGenerators in Compile += populateSparkLib.taskValue
// force order
packageBin in Universal <<= (packageBin in Universal).dependsOn(populateSparkLib)

// add the spark-lib directory to the zip
mappings in Universal ++= directory("spark-lib/")

// set the main class to the framework default launcher
//mainClass in Compile := Some("it.agilelab.bigdata.wasp.launcher.FrameworkLauncher")
mainClass in Compile := Some("it.agilelab.bigdata.wasp.pipegraph.metro.launchers.MetroLauncher")

// append hadoop and spark conf dirs environment variables to classpath
scriptClasspath += ":$HADOOP_CONF_DIR:$YARN_CONF_DIR"

// set the name of the zip file
packageName in Universal := name.value

fork in run := true

dependencyOverrides ++= Set(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.4"
)