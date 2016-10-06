package it.agilelab.bigdata.wasp.web.controllers

import play.api.mvc._
import play.api.libs.json.Json
import it.agilelab.bigdata.wasp.core.utils._
import it.agilelab.bigdata.wasp.core.models.configuration._

object Configuration_C extends WaspController with SparkBatchConfiguration with SparkStreamingConfiguration with ElasticConfiguration with SolrConfiguration {

  import BSONFormats._

  implicit def zF = Json.format[ConnectionConfig]
  implicit def kF = Json.format[KafkaConfigModel]
  implicit def sbF = Json.format[SparkBatchConfigModel]
  implicit def ssF = Json.format[SparkStreamingConfigModel]
  implicit def eF = Json.format[ElasticConfigModel]
  implicit def sF = Json.format[SolrConfigModel]

  def getKafka = Action { implicit request =>
    {
      AngularOk(Json.toJson(ConfigManager.getKafkaConfig))
    }
  }

  def getSparkBatch = Action { implicit request =>
    {
      AngularOk(Json.toJson(sparkBatchConfig))
    }
  }

  def getSparkStreaming = Action { implicit request =>
    {
      AngularOk(Json.toJson(sparkStreamingConfig))
    }
  }

  def getES = Action { implicit request =>
    {
      AngularOk(Json.toJson(elasticConfig))
    }
  }

  def getSolr = Action { implicit request =>
    {
      AngularOk(Json.toJson(solrConfig))
    }
  }
}