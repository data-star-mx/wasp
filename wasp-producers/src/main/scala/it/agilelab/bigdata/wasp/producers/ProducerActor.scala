package it.agilelab.bigdata.wasp.producers

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable}
import it.agilelab.bigdata.wasp.core.SystemPipegraphs._
import it.agilelab.bigdata.wasp.core.WaspEvent.WaspMessageEnvelope
import it.agilelab.bigdata.wasp.core.logging.WaspLogger
import it.agilelab.bigdata.wasp.core.models.TopicModel
import it.agilelab.bigdata.wasp.core.utils.{AvroToJsonUtil, BSONFormats}

case object StopMainTask

case object StartMainTask

abstract class ProducerActor[T](val kafka_router: ActorRef, val topic: Option[TopicModel]) extends Actor with ActorLogging {

  val logger = WaspLogger(this.getClass.getName)
  implicit val system = context.system
  implicit val contextPlay = scala.concurrent.ExecutionContext.Implicits.global
  var task: Option[Cancellable] = None

  def generateRawOutputJsonMessage(input: T): String

  def generateOutputJsonMessage(input: T): String

  def stopMainTask() = task.map(_.cancel())

  def mainTask()

  //TODO occhio che abbiamo la partition key schianatata, quindi usiamo sempre e solo una partizione
  val partitionKey = "partitionKey"

  val rawTopicSchema = BSONFormats.toString(rawTopic.schema)
  val topicSchema = topic.map(t => BSONFormats.toString(t.schema))

  override def postStop() {
    log.info(s"Stopping actor ${this.getClass.getName}")
    stopMainTask()
    super.postStop()
  }

  def receive: Actor.Receive = {
    case StopMainTask => stopMainTask()
    case StartMainTask => mainTask()
  }

  /**
   * Method to send to Kafka a specific message to be added to the raw topic and eventually to a custom topic.
   *
   * System pipelines won't write to the raw topic.
   */
  def sendMessage(input: T) = {

    if (topic.isEmpty) {
      val rawJson = generateRawOutputJsonMessage(input)
      //TODO: Add rawSchema from system raw pipeline
      try {
        kafka_router ! WaspMessageEnvelope[String, Array[Byte]](rawTopic.name, partitionKey, AvroToJsonUtil.jsonToAvro(rawJson, rawTopicSchema))
      } catch {
        case e: Throwable => logger.error("Exception sending message to kafka", e)
      }
    }

    topic.foreach { p =>
      val customJson = generateOutputJsonMessage(input)
      try {
        kafka_router ! WaspMessageEnvelope[String, Array[Byte]](p.name, partitionKey, AvroToJsonUtil.jsonToAvro(customJson, topicSchema.get))
      } catch {
        case e: Throwable => logger.error("Exception sending message to kafka", e)
      }
    }
  }
}