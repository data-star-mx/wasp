package it.agilelab.bigdata.wasp.web.controllers

import akka.util.Timeout
import it.agilelab.bigdata.wasp.core.bl.ConfigBL
import it.agilelab.bigdata.wasp.core.logging.WaspLogger
import it.agilelab.bigdata.wasp.core.models._
import it.agilelab.bigdata.wasp.core.utils.{BSONFormats}
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.Json
import play.api.mvc._
import reactivemongo.bson._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt


/**
 * Created by vitoressa on 14/09/15.
 */
object MlModels_C extends WaspController {

  val logger = WaspLogger("MlModels")

  import BSONFormats._

  implicit def mlF = Json.format[MlModelOnlyInfo]
  implicit val timeout = Timeout(30 seconds)

  val mlModelForm = Form(
    mapping(
      "name" -> text,
      "version" -> text,
      "description" -> text,
      "timestamp" -> optional(longNumber),
      "className" -> optional(text),
      "modelFileId" -> optional(text),
      "favorite" -> boolean,
      "_id" -> optional(text)
    )((name, version, description, timestamp, className, modelFileId, favorite, _id) =>
      MlModelOnlyInfo(_id = _id.map(BSONObjectID(_)), name = name, version = version, description = description, timestamp = timestamp, className = className, modelFileId = modelFileId.map(BSONObjectID(_)), favorite = favorite)
      )(r => Some(r.name, r.version, r.description, r.timestamp, r.className, r.modelFileId.map(_.toString()), r.favorite, r._id.map(_.toString()))))


  def getById(id: String) = Action.async { implicit request =>
  {
    ConfigBL.mlModelBL.getById(id).map {
      case Some(b) => AngularOk(Json.toJson(b))
      case None => AngularError(s"MlModel {$id} not found")
    }
  }
  }

  def getAll = Action.async { implicit request =>
  {
    ConfigBL.mlModelBL.getAll.map(
      res => AngularOk(Json.toJson(res)))
  }
  }

  def insert = Action.async { implicit request =>
  {
    AngularEditAsync[MlModelOnlyInfo](mlModelForm)(mlModelOnlyInfo => {
      ConfigBL.mlModelBL.saveMlModelOnlyInfo(mlModelOnlyInfo).map { lastErr => WriteResultToAngularRes[MlModelOnlyInfo](lastErr, errorIfNotUpdated = false, Some(mlModelOnlyInfo)) }
    })
  }
  }

  def update = Action.async { implicit request =>
  {
    AngularEditAsync[MlModelOnlyInfo](mlModelForm)(mlModelOnlyInfo => {
      ConfigBL.mlModelBL.updateMlModelOnlyInfo(mlModelOnlyInfo).map(lastErr => WriteResultToAngularRes[MlModelOnlyInfo](lastErr, errorIfNotUpdated = false, Some(mlModelOnlyInfo)))
    })

  }
  }

  def delete(id: String) = Action.async { implicit request =>
  {
    AngularSingleDeleteByIDAsync[MlModelOnlyInfo](
      ConfigBL.mlModelBL.delete(id).map {
        case Some(lastErr) => WriteResultToAngularResOnDelete(lastErr)
        case None => Right(AngularError(s"MlModel {$id} not found"))
      })

  }

  }

}