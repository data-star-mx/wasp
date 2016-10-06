package it.agilelab.bigdata.wasp.web.controllers

import it.agilelab.bigdata.wasp.core.bl.ConfigBL
import it.agilelab.bigdata.wasp.core.logging.WaspLogger
import it.agilelab.bigdata.wasp.core.models._
import it.agilelab.bigdata.wasp.core.utils.BSONFormats
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.Json
import play.api.mvc._
import reactivemongo.bson._

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by vitoressa on 12/10/15.
 */
object Index_C extends WaspController {

  val logger = WaspLogger("Index")

  import BSONFormats._

  implicit def tF = Json.format[TopicModel]
  implicit def iF = Json.format[IndexModel]
  implicit def wtF = Json.format[WriteType]
  implicit def wF = Json.format[WriterModel]


  def getByName(name : String)= Action.async { implicit request =>
    {
      ConfigBL.indexBL.getByName(name).map {
        case Some(b) => AngularOk(Json.toJson(b))
        case None => AngularError(s"Index {${name}} not found")
      }
    }
  }

}
