package it.agilelab.bigdata.wasp.test.master

import java.util.StringTokenizer

import akka.actor.Props
import it.agilelab.bigdata.wasp.core.{utils, WaspSystem}
import it.agilelab.bigdata.wasp.core.WaspSystem.{??, actorSystem, timeout}
import it.agilelab.bigdata.wasp.core.bl.ConfigBL
import it.agilelab.bigdata.wasp.core.logging.WaspLogger
import it.agilelab.bigdata.wasp.core.models._
import it.agilelab.bigdata.wasp.core.utils.WaspDB
import it.agilelab.bigdata.wasp.master._
import it.agilelab.bigdata.wasp.web.Global
import reactivemongo.bson.{BSONObjectID, BSONString}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

object MasterGuardianTest extends App with WaspSystem with BSONConversionHelper {

  
  def initializeDatabase() = {

    val id_pipeline = Some(BSONObjectID.generate)
    val id_producer = Some(BSONObjectID.generate)

    val topicSchema = s"""
      {"type":"record",
      "namespace":"Finance",
      "name":"Stock",
      "fields":[${TopicModel.schema_base},
        {"name":"stock_name","type":"string"},
        {"name":"bid","type":"double"},
        {"name":"ask","type":"double"},
        {"name":"percent_change","type":"double"},
        {"name":"volume","type":"double"},
        {"name":"currency","type":"string"}
        ]}"""

    val indexSchema = s"""
      {"yahoofinance":
          {"properties":{
            ${IndexModel.schema_base_elastic},
        "stock_name":{"type":"string","index":"not_analyzed","store":"true","enabled":"true"},
        "bid":{"type":"double","index":"not_analyzed","store":"true","enabled":"true"},
        "ask":{"type":"double","index":"not_analyzed","store":"true","enabled":"true"},
        "percent_change":{"type":"double","index":"not_analyzed","store":"true","enabled":"true"},
        "volume":{"type":"double","index":"not_analyzed","store":"true","enabled":"true"},
        "currency":{"type":"string","index":"not_analyzed","store":"true","enabled":"true"}
          }}}"""
/*
    val pipeline = PipelineModel(
      "YahooPipeline",
      "Yahoo Finance Pipeline",
      "user",
      false,
      WaspSystem.now,
      TopicModel(BSONFormats.fromString(topicSchema).get),
      IndexModel(BSONFormats.fromString(indexSchema)),
      None,
      None,
      false,
      id_pipeline)
*/
    val producer = new ProducerModel(
      "YahooFinanceProducer",
      "it.agilelab.bigdata.wasp.producers.yahoo.YahooFinanceProducer",
      id_pipeline,
      false,
      None,
      id_producer)

    println("yahoo producer insert: " + Await.ready(WaspDB.getDB.insert(producer), timeout.duration))
    //println("yahoo pipeline insert: " + Await.ready(WaspDB.getDB.insert(pipeline), timeout.duration))
  }

  def pipeline(tokenizer: StringTokenizer) =
    if (!tokenizer.hasMoreTokens)
      println("missing parameters")
    else {
      val name = tokenizer.nextToken
      //sendAndWait[Unit](StartPipeline(tokenizer.nextToken))
      val pipegraph = Await.result(WaspDB.getDB.getDocumentByField[PipegraphModel]("name", BSONString("YahooPipeline")).map(x => x match {
        case None    => null
        case Some(p) => p
      }), timeout.duration)

      println("retrieved pipeline: " + pipegraph)

      if (!tokenizer.hasMoreTokens)
        println("missing parameters")
      else
        tokenizer.nextToken match {
          case "start" => println("\n\n\n\n\n\n\n@@@ start pipeline result: " + ??(master, StartPipegraph(pipegraph._id.get.stringify)))
          case "stop"  => println("\n\n\n\n\n\n\n@@@ stop pipeline result: " + ??(master, StopPipegraph(pipegraph._id.get.stringify)))
        }
    }

  def producer(tokenizer: StringTokenizer) =
    if (!tokenizer.hasMoreTokens)
      println("missing parameters")
    else {
      val name = tokenizer.nextToken
      //sendAndWait[Unit](StartPipeline(tokenizer.nextToken))
      val producer = Await.result(WaspDB.getDB.getDocumentByField[ProducerModel]("name", BSONString("YahooFinanceProducer")).map(x => x match {
        case None    => null
        case Some(p) => p
      }), timeout.duration)

      println("retrieved producer: " + producer)

      if (!tokenizer.hasMoreTokens)
        println("missing parameters")
      else
        tokenizer.nextToken match {
          case "start" => println("\n\n\n\n\n\n\n@@@ start producer result: " + ??[Any](master, StartProducer(producer._id.get.stringify)))
          case "stop"  => println("\n\n\n\n\n\n\n@@@ stop producer result: " + ??[Any](master, StopProducer(producer._id.get.stringify)))
        }
    }

  def loggerActorProps = Global.loggerActorProps

  val logger = WaspLogger("MasterGuardianTest")

  logger.info("test master")

  val master = actorSystem.actorOf(Props(new MasterGuardian(ConfigBL)))

  //initializeDatabase()

  var line: String = ""

  while ("exit" != line) {
    val tokenizer = new StringTokenizer(Console.readLine("> "))

    if (tokenizer.hasMoreTokens) try {
      tokenizer.nextToken match {
        case "dbinit"      => initializeDatabase()
        //case "clean"        => clean(tokenizer)
        case "exit"        => line = "exit"
        case "pipeline"    => pipeline(tokenizer)
        case "producer"    => producer(tokenizer)
        case value: String => println("unknown value '" + value + "'");
      }
    } catch {
      case throwable: Throwable => throwable.printStackTrace()
    }
  }

  logger.warn("stopping wasp system")
  WaspSystem.shutdown()
  logger.warn("wasp system stopped")

  /*//def loggerActorName = "InternalLogProducerGuardian"
  def loggerActorProps = Props[InternalLogProducerGuardian]

  private def evaluate(tokenizer : StringTokenizer) = {

    def sendAndWait[T](message : MasterGuardianMessage) = {
      val response = Await.result((actor ? message), 10 seconds).asInstanceOf[T]
      logger.info("response for message [" + message + "]: " + response)
    }

    def clean(tokenizer : StringTokenizer) = {
      def pipeline(tokenizer : StringTokenizer) =
        if (!tokenizer.hasMoreTokens)
          println("missing parameters")
        else
          sendAndWait[Unit](CleanPipeline(tokenizer.nextToken))

      if (!tokenizer.hasMoreTokens)
        println("missing parameters")
      else
        tokenizer.nextToken match {
          case "pipeline" => pipeline(tokenizer);
        }
    }

    def start(tokenizer : StringTokenizer) = {

      def pipeline(tokenizer : StringTokenizer) =
        if (!tokenizer.hasMoreTokens)
          println("missing parameters")
        else
          sendAndWait[Unit](StartPipeline(tokenizer.nextToken))

      if (!tokenizer.hasMoreTokens)
        println("missing parameters")
      else
        tokenizer.nextToken match {
          case "pipeline" => pipeline(tokenizer);
        }
    }

    def stop(tokenizer : StringTokenizer) = {
      // TODO to be implemanted
    }

    if (tokenizer.hasMoreTokens)
      tokenizer.nextToken match {
        case "clean"        => clean(tokenizer)
        case "start"        => start(tokenizer)
        case "stop"         => stop(tokenizer)
        case "exit"         => line = "exit"
        case value : String => println("unknown value '" + value + "'");
      }
  }

  val logger = WaspLogger("MasterGuardianTest")
  val actor = MasterGuardian()
  var line : String = _

  while ("exit" != line) 
    evaluate(new StringTokenizer(Console.readLine("> ")))

  WaspSystem.shutdown()*/
}