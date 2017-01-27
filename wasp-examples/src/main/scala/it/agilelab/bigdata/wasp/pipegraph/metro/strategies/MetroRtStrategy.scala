package it.agilelab.bigdata.wasp.pipegraph.metro.strategies

import it.agilelab.bigdata.wasp.consumers.strategies.StrategyRT
import it.agilelab.bigdata.wasp.core.logging.WaspLogger

import scala.util.Random

/**
  * Created by marco on 27/01/17.
  */
class MetroRtStrategy extends StrategyRT with Serializable{

  val logger = WaspLogger(this.getClass)

  val inMemoryMap = collection.mutable.Map.empty[String,String]

  override def transform(topic_name: String, input: String): String = {

    val str = Random.nextInt(10000000)

    inMemoryMap += s"${str}ciao" -> "casa"

    logger.warn(s"dude the inMemoryMap size is now ${inMemoryMap.size}")

    input

  }
}
