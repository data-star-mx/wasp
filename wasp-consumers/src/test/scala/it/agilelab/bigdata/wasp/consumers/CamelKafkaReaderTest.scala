package it.agilelab.bigdata.wasp.consumers

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.camel.CamelExtension
import it.agilelab.bigdata.wasp.consumers.readers.CamelKafkaReader
import it.agilelab.bigdata.wasp.core.kafka.WaspKafkaWriter
import it.agilelab.bigdata.wasp.core.utils.{WaspDB, ConfigManager}
import org.scalatest.FlatSpec

import scala.concurrent.Await

/**
 * Created by Mattia Bertorello on 13/10/15.
 */
class CamelKafkaReaderTest  extends FlatSpec {

  behavior of "Camel Kafka Reader"

  it should "receive one message" in {

    val actorSystem = ActorSystem()
    WaspDB.DBInitialization(actorSystem)
    ConfigManager.initializeConfigs()
    val conf = ConfigManager.getKafkaConfig
    val topic = "test"
    val kafkaReader = actorSystem.actorOf(Props(new CamelKafkaReader(conf, topic, "default", ActorRef.noSender)))


    //TODO Fare un producer che funziona
    val producer = new WaspKafkaWriter[String, String](WaspKafkaWriter.createConfig(
      conf.connections.map(x => x.toString).toSet, conf.batch_send_size, "async", conf.default_encoder, conf.encoder_fqcn, conf.partitioner_fqcn))

    producer.send(topic, "1", "test msg")
    producer.send(topic, "1", "test msg")
    producer.send(topic, "1", "test msg")
    producer.send(topic, "1", "test msg")
    producer.close()
    import scala.concurrent.duration._

    val camel = CamelExtension(actorSystem)
    // get a future reference to the activation of the endpoint of the Consumer Actor
    val activationFuture = camel.activationFutureFor(kafkaReader)(timeout = 10 seconds,
      executor = actorSystem.dispatcher)
    Await.result(activationFuture, 10.seconds)
    println("finish")
    Thread.sleep(10000)
  }

}
