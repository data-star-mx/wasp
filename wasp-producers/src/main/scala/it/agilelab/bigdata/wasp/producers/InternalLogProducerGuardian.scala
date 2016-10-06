/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.agilelab.bigdata.wasp.producers

import java.util.Date

import akka.actor._
import akka.event.Logging.{Debug, Error, Info, Warning}
import it.agilelab.bigdata.wasp.core.bl.{TopicBL, ProducerBL}
import it.agilelab.bigdata.wasp.core.models.TopicModel
import it.agilelab.bigdata.wasp.core.utils.{AvroToJsonUtil, TimeFormatter}

final class InternalLogProducerGuardian(env: {val producerBL: ProducerBL; val topicBL: TopicBL}) extends ProducerMasterGuardian(env) {

  val name = "LoggerProducer"

  var producerActor: Option[ActorRef] = None

  def startChildActors() {
    logger.info("Executing startChildActor method")
    producerActor = Some(context.actorOf(Props(new InternalLogProducerActor(kafka_router, associatedTopic))))
  }

  override def initialized: Actor.Receive = super.initialized orElse loggerinitialized

  def loggerinitialized: Actor.Receive = {
    case e@(Error(_, _, _, _) | Warning(_, _, _) | Info(_, _, _) | Debug(_, _, _)) =>
      if (producerActor.isDefined)
        producerActor.get forward e
  }

}

private class InternalLogProducerActor(kafka_router: ActorRef, topic: Option[TopicModel]) extends ProducerActor[String](kafka_router, topic) {


  override def receive: Actor.Receive = super.receive orElse loggerReceive

  def loggerReceive(): Actor.Receive = {
    case Error(cause, logSource, logClass, message: Any) => sendLog(logSource, logClass.getName, "ERROR", message.toString, Some(cause.getMessage), Some(cause.getStackTraceString))
    case Warning(logSource, logClass, message: String) => sendLog(logSource, logClass.getName, "WARNING", message.toString)
    case Info(logSource, logClass, message: String) => sendLog(logSource, logClass.getName, "INFO", message.toString)
    case Debug(logSource, logClass, message: String) => sendLog(logSource, logClass.getName, "DEBUG", message.toString)
    case e: Any => println(e.toString)
  }

  def sendLog(logSource: String, logClass: String, logLevel: String, message: String, cause: Option[String] = None, stackTrace: Option[String] = None) {

    //logger.info(message)

    // TODO: This is broken for some reason. Got JSON parsing exception. Using a placeholder until fixed
    // Do NOT mark it as fixed without testing
    //val causeEx = cause.getOrElse("empty")
    //val stackEx = stackTrace.getOrElse("empty")

    val causeEx = ""
    val stackEx = ""

    val logFields = s"""
	    		"log_source":"$logSource",
	    		"log_class":"$logClass",
          "log_level":"$logLevel",
	    		"message":"${AvroToJsonUtil.convertToUTF8(message)}",
	    		"cause":"$causeEx",
	    		"stack_trace":"$stackEx"	    
	    """

    val myJson = s"""{
	     "id_event":0.0,
	     "source_name":"LoggerPipeline",
	     "topic_name":"${topic.get.name}",
	     "metric_name":"log",  
	     "timestamp":"${TimeFormatter.format(new Date())}",
	     "latitude":0.0,
	     "longitude":0.0,
	     "value":0.0,
	     "payload":"logPayload",
	     $logFields
	     }"""

    sendMessage(myJson)
  }

  // For this specific logger, we by-pass the standard log duplication mechanic
  def generateOutputJsonMessage(input: String): String = input

  def generateRawOutputJsonMessage(input: String): String = ""

  def mainTask() = {
    /*We don't have a task here because it's a system pipeline*/
  }

}