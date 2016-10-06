/*package it.agilelab.bigdata.wasp.test.logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.streaming.Milliseconds
import org.apache.spark.streaming.StreamingContext
import com.datastax.spark.connector.embedded.EmbeddedKafka
import akka.actor.ActorSystem
import akka.actor.Props
import akka.util.Timeout
import it.agilelab.bigdata.wasp.consumers.AppSettings
import it.agilelab.bigdata.wasp.core.logging.LoggerInjector
import it.agilelab.bigdata.wasp.core.logging.WaspLogger
import it.agilelab.bigdata.wasp.core.models.PipelineModel
import it.agilelab.bigdata.wasp.core.utils.KafkaConfiguration
import it.agilelab.bigdata.wasp.core.utils.SparkConfiguration
import it.agilelab.bigdata.wasp.core.utils.WaspActorSystem
import it.agilelab.bigdata.wasp.core.utils.WaspDB
import it.agilelab.bigdata.wasp.producers.InternalLogProducerGuardian
import it.agilelab.bigdata.wasp.core.utils.WaspDB._
import reactivemongo.bson.BSONString
import it.agilelab.bigdata.wasp.core.utils.BSONFormats
import it.agilelab.bigdata.wasp.consumers.elasticsearch.ElasticsearchIndexActor
import it.agilelab.bigdata.wasp.consumers.elasticsearch.ElasticsearchCreateIndex
import it.agilelab.bigdata.wasp.producers.yahoo.YahooFinanceApiNodeGuardian
import it.agilelab.bigdata.wasp.consumers.yahoo.YahooNodeGuardian
import it.agilelab.bigdata.wasp.producers.yahoo.InitializeYahooProducer
import akka.actor.PoisonPill

object LoggerApp extends App with SparkConfiguration with KafkaConfiguration  with WaspActorSystem {

  val t =  actorSystem.actorOf(Props[InternalLogProducerGuardian], "InternalLogProducerGuardian")

 //override def defineLoggerActor = t
  val settings = new AppSettings
  import settings._

  //implicit val timeout = Timeout(new FiniteDuration(5000, java.util.concurrent.TimeUnit.MILLISECONDS))
  Thread.sleep(1000)
  val log = WaspLogger(this.getClass)
  log.updateReference(Some(t))
  
  /** Configures Spark. */
  log.info("Fetching Spark configuration and initializing Spark context")

  lazy val conf = new SparkConf().setAppName(getClass.getSimpleName)
    //.set("spark.cassandra.connection.host", CassandraHosts)
    .set("spark.cleaner.ttl", sparkConfig.cleaner_ttl.toString)
    .setMaster(sparkConfig.master)

  lazy val sc = new SparkContext(conf)

  /** Creates the Spark Streaming context. */
  lazy val ssc = new StreamingContext(sc, Milliseconds(sparkConfig.streaming_batch_interval_ms))
  log.info("Spark streaming context created")

  /** Configures Kafka. */
  log.info("Fetching Kafka configuration")
  val kafkaProperties : Map[String, String] = Map("group.id" -> kafkaConfig.group_id,
    "broker.id" -> kafkaConfig.broker_id,
    "kafka.topic" -> kafkaConfig.topic_raw,
    //"auto.offset.reset" -> "largest",
    "zookeeper.connect" -> s"${kafkaConfig.zookeeper.connectionString}")
    
  /** Start Logger Streaming Actor **/

  //val guardianLogger = actorSystem.actorOf(Props(new InternalLogNodeGuardian(ssc, kafkaProperties, PipelineModel.logPipeline)), "node-guardian-logger-cons")
  while(true){
   
    log.info("Logger info consumer created")
   log.debug("Logger debug consumer created")
    log.error("Logger error consumer created")
    Thread.sleep(5000)
  }

}*/