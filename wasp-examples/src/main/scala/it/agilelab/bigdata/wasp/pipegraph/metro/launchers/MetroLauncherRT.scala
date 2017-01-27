package it.agilelab.bigdata.wasp.pipegraph.metro.launchers

import it.agilelab.bigdata.wasp.core.models.{IndexModel, PipegraphModel, ProducerModel, TopicModel}
import it.agilelab.bigdata.wasp.core.utils.WaspDB
import it.agilelab.bigdata.wasp.launcher.WaspLauncher
import it.agilelab.bigdata.wasp.pipegraph.metro.models._

/**
  * Created by matbovet on 18/07/2016.
  */
object MetroLauncherRT extends WaspLauncher {
  override def initializeCustomWorkloads(): Unit = {
    println("Launching metro RTc!")

    WaspDB.getDB.insertIfNotExists[TopicModel](MetroTopicModel.metroTopic)
    WaspDB.getDB.insertIfNotExists[IndexModel](MetroIndexModel.metroIndex)
    WaspDB.getDB
      .insertIfNotExists[PipegraphModel](MetroPipegraphModelRT.metroPipegraph)
    WaspDB.getDB
      .insertIfNotExists[ProducerModel](MetroProducerModel.metroProducer)

  }
}
