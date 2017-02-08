import com.typesafe.sbt.packager.MappingsHelper._

name := "wasp"

scalaVersion := "2.10.6"

// populate spark-lib
libraryDependencies ++= Dependencies.sparkLibJava ++ Dependencies.sparkLibScala
// define helpers
val sparkLibPath = "spark-lib/managed/"
val sparkLibListPath = "spark-lib.list"
def getJarFilesFromClasspath = (classpath: Seq[Attributed[File]], modules: Set[(String, String)]) => {
  val dummyModuleID = new ModuleID("", "", "", None)
  val sparkLibDependencies = classpath.filter {
    dep =>
      val m = dep.metadata.get(AttributeKey[ModuleID]("module-id")).getOrElse(dummyModuleID)
      modules((m.organization, m.name))
  }
  val sparkLibJarFiles = sparkLibDependencies.map {
    dep => dep.data.getAbsoluteFile
  }
  sparkLibJarFiles
}
// define tasks
lazy val populateSparkLib = taskKey[Seq[File]]("Populates the spark-lib directory")
populateSparkLib := {
  val log = streams.value.log
  val scalaSuffix = "_" + scalaVersion.value.split("\\.").take(2).mkString(".")
  
  log.info("Populating spark-lib directory: \"" + sparkLibPath + "\".")
  
  log.info("Cleaning spark-lib... ")
  IO.delete(new File(sparkLibPath))
  log.info("Done cleaning.")
  
  log.info("Adding dependency jars to spark-lib...")
  val sparkLibModules = {
    Dependencies.sparkLibJava.map(m => (m.organization, m.name)) ++
    Dependencies.sparkLibScala.map(m => (m.organization, m.name + scalaSuffix))
  }.toSet
  val sparkLibJarFiles = getJarFilesFromClasspath((dependencyClasspath in Compile).value, sparkLibModules)
  log.info("Adding: " + sparkLibJarFiles.mkString(","))
  IO.copy(sparkLibJarFiles.map(jar => (jar, new File(sparkLibPath + jar.getName))))
  log.info("Done adding dependency jars.")
  
  log.info("Adding WASP jars to spark-lib...")
  val waspProducts = (exportedProducts in Compile in Core).value ++
    (exportedProducts in Compile in Consumers).value ++
    (exportedProducts in Compile in Producers).value
  val waspJarFiles = waspProducts.map(dep => dep.data.getAbsoluteFile)
  log.info("Adding: " + waspJarFiles.mkString(","))
  IO.copy(waspJarFiles.map(jar => (jar, new File(sparkLibPath + jar.getName))))
  log.info("Done adding WASP jars.")
  
  Seq.empty[File]
}
lazy val createSparkLibList = taskKey[Seq[File]]("Creates a list of the dependencies included in the spark-lib directory")
createSparkLibList := {
  val log = streams.value.log
  val scalaSuffix = "_" + scalaVersion.value.split("\\.").take(2).mkString(".")
  
  log.info("Creating spark-lib list: \"" + sparkLibListPath + "\".")
  
  log.info("Cleaning existing spark-lib list... ")
  IO.delete(new File(sparkLibListPath))
  log.info("Done cleaning.")
  
  log.info("Writing spark-lib list...")
  val sparkLibModules = {
    Dependencies.sparkLibJava.map(m => (m.organization, m.name)) ++
    Dependencies.sparkLibScala.map(m => (m.organization, m.name + scalaSuffix))
  }.toSet
  val sparkLibDependenciesList = sparkLibModules.map(module => module._1 + " " + module._2).mkString("\n")
  val sparkLibList = new File(sparkLibListPath)
  IO.write(sparkLibList, sparkLibDependenciesList)
  log.info("Done writing spark-lib list.")
  
  Seq.empty[File]
}
// add tasks to resource generators
resourceGenerators in Compile += populateSparkLib.taskValue
resourceGenerators in Compile += createSparkLibList.taskValue
// force order
packageBin in Universal <<= (packageBin in Universal).dependsOn(populateSparkLib)
packageBin in Compile <<= (packageBin in Compile).dependsOn(createSparkLibList)

// add the spark-lib directory to the zip
mappings in Universal ++= directory(sparkLibPath)

// add the spark-lib list to the jar
mappings in (Compile, packageBin) += {
  (baseDirectory.value / sparkLibListPath) -> sparkLibListPath
}

// set the main class to the framework default launcher
//mainClass in Compile := Some("it.agilelab.bigdata.wasp.launcher.FrameworkLauncher")
mainClass in Compile := Some("it.agilelab.bigdata.wasp.pipegraph.metro.launchers.MetroLauncher")

// append conf dirs environment variables to classpath
scriptClasspath += ":$HADOOP_CONF_DIR:$YARN_CONF_DIR:$HBASE_CONF_DIR"

// set the name of the zip file
packageName in Universal := name.value

fork in run := true

dependencyOverrides in ThisBuild ++= Set(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.4"
)