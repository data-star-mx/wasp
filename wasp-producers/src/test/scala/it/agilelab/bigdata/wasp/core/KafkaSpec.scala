package it.agilelab.bigdata.wasp.core

import akka.actor._
import akka.cluster.Cluster
import akka.testkit.TestActorRef
import it.agilelab.bigdata.wasp.core.WaspSystem._
import it.agilelab.bigdata.wasp.core.bl.{AllBLsTestWrapper, ConfigBL, ProducerBL, TopicBL}
import it.agilelab.bigdata.wasp.core.kafka.WaspKafkaReader
import it.agilelab.bigdata.wasp.core.logging.WaspLogger
import it.agilelab.bigdata.wasp.core.models.{ProducerModel, TopicModel}
import it.agilelab.bigdata.wasp.core.utils.{ConfigManager, WaspDB, BSONFormats}
import it.agilelab.bigdata.wasp.producers._
import org.scalatest.{FlatSpec, _}
import org.scalatest.concurrent.{ScalaFutures, Timeouts}
import reactivemongo.bson.BSONObjectID


/**
 * Created by Mattia Bertorello on 06/10/15.
 */
class ProducerMasterGuadianTest(env: {val producerBL: ProducerBL; val topicBL: TopicBL}, topicModel: TopicModel) extends ProducerMasterGuardian(env, topicModel._id.get.stringify) {

  override val name: String = ProducerMasterGuadianTest.name
  override def preStart(): Unit = {
    logger.info(s"${ConfigManager.getKafkaConfig}")
    logger.info(s"Starting")
  }

  override def startChildActors(): Unit = {
    logger.info("startChildActors")
    val aRef = context.actorOf(Props(new ProducerActorTest(kafka_router, Some(topicModel))))
    aRef ! StartMainTask
  }
}
object ProducerMasterGuadianTest {
  val name: String = "producerTest"
}

case class MessageTest(description: String)

class ProducerActorTest(kafka_router: ActorRef, topic: Option[TopicModel]) extends ProducerActor[MessageTest](kafka_router, topic) with ActorLogging{
  override def generateRawOutputJsonMessage(input: MessageTest): String = ???

  override def mainTask(): Unit = {
    logger.info("sendMessage")
    sendMessage(MessageTest("testDescription"))
  }

  override def generateOutputJsonMessage(input: MessageTest): String = {
    logger.info("generateOutputJsonMessage")
    s"""{"description":"${input.description}"}"""

  }
}

/**
 * This test must ave mongoDB and Kafka server up with the right configuration un MongoDB
 */
class KafkaSpec extends FlatSpec with Matchers with BeforeAndAfterAll  with ScalaFutures with WaspSystem with Timeouts {
  behavior of "Kafka test"
  WaspDB.DBInitialization(actorSystem = actorSystem)
  ConfigManager.initializeConfigs()

  val logger = WaspLogger(this.getClass.getName)
  it should "send and receive a message" in {

    val kafkaConfig = ConfigManager.getKafkaConfig


    val env = new AllBLsTestWrapper
    val groupId = "1"

    val testTopicModelSchema = s"""
    {"type":"record",
    "namespace":"Raw",
    "name":"Raw",
    "fields":[
       {"name":"description","type":"string"}
    ]}"""
    val topicModel = TopicModel(name = "testTopicModel2",
      creationTime = 1l,
      partitions =  1,
      replicas = 1,
      topicDataType = "avro", // avro, json, xml
      schema = BSONFormats.fromString(testTopicModelSchema),
      _id = Some(BSONObjectID.generate))

    val producerModel = ProducerModel(
      name = ProducerMasterGuadianTest.name,
      className = "it.agilelab.bigdata.wasp.core.ProducerNodeGuadianTest",
      isActive = true,
      _id = Some(BSONObjectID.generate),
      id_topic = topicModel._id
    )
    env.producerBL.persist(producerModel)

    env.topicBL.persist(topicModel)

    val cluster = Cluster(actorSystem)

    val producerNodeGuardianActor = actorSystem.actorOf(Props(new ProducerMasterGuadianTest(env, topicModel)), ProducerMasterGuadianTest.name)


    val result = ??[Boolean](producerNodeGuardianActor, StartProducer)

    result shouldBe true
    val t = new WaspKafkaReader(WaspKafkaReader.createConfig(kafkaConfig.connections.map(x => x.toString).toSet, groupId, zookeper = kafkaConfig.zookeeper.toString))
    val actorRefMaster = TestActorRef(new Actor {
      def receive = {
        case msg => {
            logger.info(s"Message from actor: ${msg.toString}")
            msg.toString should not be empty
          }
      }
    })

    import org.scalatest.time.SpanSugar._
    cancelAfter(10.seconds) {
      t.subscribe(topicModel.name, actorRefMaster)
    }


    val result2 = ??[Boolean](producerNodeGuardianActor, StopProducer)
    result2 shouldBe true

  }

  /**
   * Logger actor's Akka Props.
   * Subclasses are forced to give define a proper return value
   */
  override def loggerActorProps: Props  = Props(new InternalLogProducerGuardian(ConfigBL))
}
