package it.agilelab.bigdata.wasp.consumers.writers

import it.agilelab.bigdata.wasp.core.WaspSystem
import it.agilelab.bigdata.wasp.core.WaspSystem._
import it.agilelab.bigdata.wasp.core.bl.TopicBL
import it.agilelab.bigdata.wasp.core.kafka.{CheckOrCreateTopic, WaspKafkaWriter}
import it.agilelab.bigdata.wasp.core.models.configuration.TinyKafkaConfig
import it.agilelab.bigdata.wasp.core.utils.{AvroToJsonUtil, BSONFormats, ConfigManager}
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream

import scala.concurrent.Await


class KafkaSparkStreamingWriter(env: {val topicBL: TopicBL}, ssc: StreamingContext, id: String)
  extends SparkStreamingWriter {

  override def write(stream: DStream[String]): Unit = {
    val topicFut = env.topicBL.getById(id)
    val topicOpt = Await.result(topicFut, timeout.duration)
    topicOpt.foreach(topic => {

      if (??[Boolean](WaspSystem.getKafkaAdminActor, CheckOrCreateTopic(topic.name))) {

        val schemaB = ssc.sparkContext.broadcast(BSONFormats.toString(topic.schema))
        val configB = ssc.sparkContext.broadcast(ConfigManager.getKafkaConfig.toTinyConfig())
        val topicNameB = ssc.sparkContext.broadcast(topic.name)

        stream.foreachRDD(rdd => {
          rdd.foreachPartition(partitionOfRecords => {
            val writer = WorkerKafkaWriter.writer(configB.value)

            partitionOfRecords.foreach(record => {
              val bytes = AvroToJsonUtil.jsonToAvro(record, schemaB.value)
              writer.send(topicNameB.value, "partitionKey", bytes)
            })

            writer.close()
          })
        })

      } else {
        throw new Exception("Error creating topic " + topic.name)
        //TODO handle errors
      }
    })
  }
}

object WorkerKafkaWriter {
	//lazy producer creation allows to create a kafka conection per worker instead of per partition
	def writer(config: TinyKafkaConfig): WaspKafkaWriter[String, Array[Byte]] = {
		ProducerObject.config = config
		//thread safe
		ProducerObject.writer
	}
	
	object ProducerObject {
		var config: TinyKafkaConfig = _
		lazy val writer = new WaspKafkaWriter[String, Array[Byte]](config)
	}
	
}