package it.agilelab.bigdata.wasp.web.controllers

import akka.util.Timeout
import it.agilelab.bigdata.wasp.core.bl.ConfigBL
import it.agilelab.bigdata.wasp.core.logging.WaspLogger
import it.agilelab.bigdata.wasp.core.models.TopicModel
import it.agilelab.bigdata.wasp.core.utils.{BSONFormats}
import play.api.libs.json._
import play.api.mvc.Action

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

object Topic_C extends WaspController {

  val logger = WaspLogger("Topic")

  val topicBL = ConfigBL.topicBL
  import BSONFormats._
  implicit def pF = Json.format[TopicModel]
  implicit val timeout = Timeout(15 seconds)

  def getById(id: String) = Action.async { implicit request =>
    {
      topicBL.getById(id).map(
        res => res match {
          case Some(p) => AngularOk(Json.toJson(p))
          case None    => AngularError(s"Topic {${id}} not found")
        })
    }
  }

  def getAll = Action.async { implicit request =>
    {
      ConfigBL.topicBL.getAll.map(
        res => AngularOk(Json.toJson(res)))
    }
  }

}


