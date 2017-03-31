package it.agilelab.bigdata.wasp.pipegraph.metro.models

import it.agilelab.bigdata.wasp.core.models.{ProducerModel, TopicModel}
import it.agilelab.bigdata.wasp.pipegraph.metro.producers.MetroProducer
import it.agilelab.bigdata.wasp.core.utils.BSONFormats
import reactivemongo.bson.BSONObjectID

/**
  * Created by matbovet on 18/07/2016.
  */
object MetroProducerModel {

  /**
    * LOS ANGELES METRO TRACKING PRODUCER
    */
  lazy val metroProducer = ProducerModel(
    "MetroProducer",
    classOf[MetroProducer].getName,
    Some(MetroTopicModel.metroTopic._id.get),
    isActive = false,
    None,
    isRemote = false,
    Some(BSONObjectID.generate)
  )
}
