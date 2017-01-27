package it.agilelab.bigdata.wasp.pipegraph.metro.models

import com.typesafe.config.{Config, ConfigFactory}
import it.agilelab.bigdata.wasp.core.WaspSystem
import it.agilelab.bigdata.wasp.core.models.{StrategyModel, _}
import reactivemongo.bson.BSONObjectID

/**
  * Created by matbovet on 18/07/2016.
  */
object MetroPipegraphModelRT {

  lazy val metroPipegraphName = "MetroPipegraph6RT"

  lazy val metroPipegraph = MetroPipegraphRT()

  lazy val conf: Config = ConfigFactory.load

  private[wasp] object MetroPipegraphRT {

    def apply() =
      PipegraphModel(
        name = MetroPipegraphModelRT.metroPipegraphName,
        description = "Los Angeles Metro Pipegraph RT",
        owner = "user",
        system = false,
        creationTime = WaspSystem.now,
        etl = Nil,
        rt = List(
          RTModel("do nuffin",
            List(ReaderModel(MetroTopicModel.metroTopic._id.get,
              MetroTopicModel.metroTopic.name,
              TopicModel.readerType)),
            isActive = true,
            Some(StrategyModel
            ("it.agilelab.bigdata.wasp.pipegraph.metro.strategies.MetroRtStrategy", None)),
            None
          )
        ),
        dashboard = None,
        isActive = false,
        _id = Some(BSONObjectID.generate))

  }

}
