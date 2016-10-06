import sbt._
import sbt.Keys._

import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import play.sbt.PlayImport.PlayKeys._

object WaspBuild extends Build {

  import Settings._

  lazy val Wasp = Project(
    id = "Wasp",
    base = file("."),
    settings = defaultSettings
  ).enablePlugins(JavaAppPackaging)
    .dependsOn(Core, Consumers, Producers, Launcher, WebApp, Examples)
    .aggregate(Core, Consumers, Producers, Launcher, WebApp, Examples)

  lazy val WebApp = Project(
    id = "WebApp",
    base = file("./wasp-webapp"),
    settings = defaultSettings
  ).enablePlugins(play.PlayScala)
    .settings {
      libraryDependencies ++= Dependencies.wasp_web
      // add play assets jar to published artifacts
      packagedArtifacts in publishLocal := {
        val artifacts: Map[sbt.Artifact, java.io.File] =
          (packagedArtifacts in publishLocal).value
        val assets: java.io.File = (playPackageAssets in Compile).value
        artifacts + (Artifact(moduleName.value, "jar", "jar", "assets") -> assets)
      }
    }
    .dependsOn(Core, Consumers, Producers)

  lazy val Core = Project(
    id = "Core",
    base = file("./wasp-core"),
    settings = defaultSettings
  ).settings {
    libraryDependencies ++= Dependencies.wasp_core
  }.configs(IntegrationTest)

  lazy val Consumers = Project(
    id = "Consumers",
    base = file("./wasp-consumers"),
    settings = defaultSettings
  ).settings {
    libraryDependencies ++= Dependencies.wasp_consumers
  }.dependsOn(Core).configs(IntegrationTest)

  lazy val Producers = Project(
    id = "Producers",
    base = file("./wasp-producers"),
    settings = defaultSettings
  ).settings {
    libraryDependencies ++= Dependencies.wasp_producers
  }.dependsOn(Core).configs(IntegrationTest)

  lazy val Launcher = Project(
    id = "Launcher",
    base = file("./wasp-launcher"),
    settings = defaultSettings
  ).settings {
    libraryDependencies ++= Dependencies.play
  }.dependsOn(Core, Consumers, WebApp).configs(IntegrationTest)

  lazy val Examples = Project(
    id = "Examples",
    base = file("./wasp-examples"),
    settings = defaultSettings
  ).settings {
    libraryDependencies ++= Dependencies.wasp_examples
  }.dependsOn(Core, Launcher)

}

object Dependencies {
  import Versions._

  implicit class Exclude(module: ModuleID) {
    def log4jExclude: ModuleID =
      module excludeAll (ExclusionRule("log4j"))

    def embeddedExclusions: ModuleID =
      module.log4jExclude
        .excludeAll(ExclusionRule("org.apache.spark"))
        .excludeAll(ExclusionRule("com.typesafe"))

    def driverExclusions: ModuleID =
      module.log4jExclude
        .exclude("com.google.guava", "guava")
        .excludeAll(ExclusionRule("org.slf4j"))

    def sparkExclusions: ModuleID =
      module.log4jExclude
        .exclude("com.google.guava", "guava")
        .exclude("org.apache.spark", "spark-core")
        .exclude("org.slf4j", "slf4j-log4j12")

    def kafkaExclusions: ModuleID =
      module
        .excludeAll(ExclusionRule("org.slf4j"))
        .exclude("com.sun.jmx", "jmxri")
        .exclude("com.sun.jdmk", "jmxtools")
        .exclude("net.sf.jopt-simple", "jopt-simple")

    def sparkSolrExclusions: ModuleID =
      module.excludeAll(
        ExclusionRule(organization = "org.eclipse.jetty"),
        ExclusionRule(organization = "javax.servlet"),
        ExclusionRule(organization = "org.eclipse.jetty.orbit")
      )
  }

  object Compile {

    //TODO togliere tutte le dipendenze che non servono nel core e spostarle nel dev
    val playcore = "com.typesafe.play" %% "play" % Play
    val playws = "com.typesafe.play" %% "play-ws" % Play
    val playcache = "com.typesafe.play" %% "play-cache" % Play
    val playjson = "com.typesafe.play" %% "play-json" % Play
    val playserver = "com.typesafe.play" %% "play-netty-server" % Play
    val akkaStream = "com.typesafe.akka" %% "akka-stream-experimental" % AkkaStreams
    val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core-experimental" % AkkaStreams
    val akkaActor = "com.typesafe.akka" %% "akka-actor" % Akka
    val akkaCluster = "com.typesafe.akka" %% "akka-cluster" % Akka
    val akkaRemote = "com.typesafe.akka" %% "akka-remote" % Akka
    val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % Akka
    val akkaCamel = "com.typesafe.akka" % "akka-camel_2.10" % Akka
    val avro = "org.apache.avro" % "avro" % Avro

    val elasticSearch = "org.elasticsearch" % "elasticsearch" % ElasticSearch
    val elasticSearchSpark = "org.elasticsearch" %% "elasticsearch-spark" % ElasticSearchSpark
    val jodaTime = "joda-time" % "joda-time" % JodaTime % "compile;runtime"
    // ApacheV2
    val jodaConvert = "org.joda" % "joda-convert" % JodaConvert % "compile;runtime"
    // ApacheV2
    val json4sCore = "org.json4s" %% "json4s-core" % Json4s
    // ApacheV2
    val json4sJackson = "org.json4s" %% "json4s-jackson" % Json4s
    // ApacheV2
    val json4sNative = "org.json4s" %% "json4s-native" % Json4s
    // ApacheV2
    val kafka = "org.apache.kafka" %% "kafka" % Kafka kafkaExclusions
    // ApacheV2
    val kafkaClients =
      "org.apache.kafka" % "kafka-clients" % Kafka kafkaExclusions
    // ApacheV2
    val kafkaStreaming =
      "org.apache.spark" %% "spark-streaming-kafka" % Spark sparkExclusions
    // ApacheV2
    val logback = "ch.qos.logback" % "logback-classic" % Logback
    val logbackCore = "ch.qos.logback" % "logback-core" % Logback
    val reactive_mongo = "org.reactivemongo" %% "reactivemongo" % ReactiveMongo
    // MIT
    val slf4jApi = "org.slf4j" % "slf4j-api" % Slf4j
    // MIT
    val sparkML = "org.apache.spark" %% "spark-mllib" % Spark sparkExclusions
    // ApacheV2
    val sparkCatalyst =
      "org.apache.spark" %% "spark-catalyst" % Spark sparkExclusions
    // ApacheV2
    val sparkYarn = "org.apache.spark" %% "spark-yarn" % Spark sparkExclusions

    // TODO verify license
    val solr = "org.apache.solr" % "solr-solrj" % Solr
    val solrspark =
      "it.agilelab.bigdata.spark" % "spark-solr" % SolrSpark sparkSolrExclusions

    val asynchttpclient = "com.ning" % "async-http-client" % "1.9.39"

    val scaldi = "org.scaldi" %% "scaldi-akka" % "0.3.3"
    val apacheCommonsLang3 = "org.apache.commons" % "commons-lang3" % apacheCommonsLang3Version
    val camelKafka = "org.apache.camel" % "camel-kafka" % CamelKafka
    val camelWebsocket = "org.apache.camel" % "camel-websocket" % CamelWebsocket
    val camelElastic = "org.apache.camel" % "camel-elasticsearch" % CamelElasticSearch
    val camelQuartz2 = "org.apache.camel" % "camel-quartz2" % CamelQuartz2
    val zkclient = "com.101tec" % "zkclient" % "0.3"

    val typesafeConfig = "com.typesafe" % "config" % "1.3.0"

    val scalaj = "org.scalaj" %% "scalaj-http" % "1.1.4"

    val metrics = "com.yammer.metrics" % "metrics-core" % "2.2.0"

    val httpmime = "org.apache.httpcomponents" % "httpmime" % "4.3.1"
  }

  object Test {
    val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % Akka % "test,it" // ApacheV2
    val akkaClusterTest = "com.typesafe.akka" % "akka-multi-node-testkit_2.10" % Akka % "test,it"
    val scalatest = "org.scalatest" %% "scalatest" % ScalaTest % "test,it"
  }

  import Compile._

  val play = Seq(playcore, playws, playcache, playjson, playserver)

  val akka = Seq(akkaStream,
                 akkaHttpCore,
                 akkaActor,
                 akkaCluster,
                 akkaRemote,
                 akkaSlf4j,
                 akkaCamel)

  val spark = Seq(sparkCatalyst)

  val apachesolr = Seq(solr, solrspark)

  val json = Seq(json4sCore, json4sJackson, json4sNative)

  val logging = Seq(logback, slf4jApi)

  val time = Seq(jodaConvert, jodaTime)

  val test = Seq(Test.akkaTestKit, Test.akkaClusterTest, Test.scalatest)

  val elastic = Seq(elasticSearch)

  /** Module deps */
  val wasp_producers = akka ++ logging ++ test ++
      Seq(playws)

  val wasp_core = akka ++ logging ++ time ++ test ++ elastic ++ apachesolr ++
      Seq(avro,
          reactive_mongo,
          kafka,
          playjson,
          playws,
          scaldi,
          apacheCommonsLang3,
          sparkYarn,
          asynchttpclient,
          typesafeConfig)

  val wasp_test = elastic

  val wasp_web = play ++ logging ++ time ++ json ++ elastic

  val wasp_consumers = json ++ test ++
      Seq(kafka,
          kafkaStreaming,
          sparkML,
          sparkCatalyst,
          elasticSearchSpark,
          camelKafka,
          camelWebsocket,
          camelElastic,
          camelQuartz2)

  val wasp_examples = logging ++ spark ++
      Seq(playjson, playws, scalaj)

  /** These dependencies will get copied into spark-lib */
  val sparkLibJava = Seq(kafkaClients, logback, logbackCore, zkclient, metrics, httpmime, avro) ++ apachesolr
  val sparkLibScala = Seq(elasticSearchSpark, kafka, kafkaStreaming, playjson) ++ json
}
