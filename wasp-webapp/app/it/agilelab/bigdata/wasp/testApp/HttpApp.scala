package it.agilelab.bigdata.wasp.testApp

import it.agilelab.bigdata.wasp.core.WaspSystem
import it.agilelab.bigdata.wasp.core.logging.WaspLogger
import it.agilelab.bigdata.wasp.core.models.{IndexModel, TopicModel}
import it.agilelab.bigdata.wasp.web.Global
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID

object HttpApp extends App with WaspSystem {

  def loggerActorProps = Global.loggerActorProps
  val logger = WaspLogger("HttpApp")

  val avroCustom = s"""
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

  val jsonES = s"""
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

  val originaljson = Json.parse(jsonES)
  val id_pipeline = Some(BSONObjectID.generate)
  val id_producer = Some(BSONObjectID.generate)


}