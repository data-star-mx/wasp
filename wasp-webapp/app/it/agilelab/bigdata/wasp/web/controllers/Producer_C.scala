package it.agilelab.bigdata.wasp.web.controllers

import akka.pattern.ask
import akka.util.Timeout
import it.agilelab.bigdata.wasp.core.WaspSystem.masterActor
import it.agilelab.bigdata.wasp.core.bl.ConfigBL
import it.agilelab.bigdata.wasp.core.logging.WaspLogger
import it.agilelab.bigdata.wasp.core.models.ProducerModel
import it.agilelab.bigdata.wasp.core.utils.{BSONFormats}
import it.agilelab.bigdata.wasp.master.{StartProducer, StopProducer}
import play.api.data.Form
import play.api.data.Forms.{boolean, mapping, optional, text}
import play.api.libs.json.Json
import play.api.mvc.Action
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

object Producer_C extends WaspController {

  val logger = WaspLogger("Producer")

  import BSONFormats._

  implicit def pF = Json.format[ProducerModel]
  implicit val timeout = Timeout(15 seconds)

  val producerBL = ConfigBL.producerBL
  val producerForm = Form(
    mapping(
      "name" -> text,
      "className" -> text,
      "id_topic" -> optional(text),
      "isActive" -> boolean,
      "configuration" -> optional(text),
      "id" -> optional(text))(
        (name, className, id_topic, isActive, configuration, id) =>
          ProducerModel(name, className, id_topic.map(_id => BSONObjectID(_id)), isActive, configuration.flatMap(c => fromString(c)), id.map(_id => BSONObjectID(_id))))(
          p => Some(p.name, p.className, p.id_topic.map(id => id.toString()), p.isActive, p.configuration.map(c => BSONFormats.toString(c)), (p._id.map(id => id.toString())))))

  def getById(id: String) = Action.async { implicit request =>
    {
      producerBL.getById(id).map(
        res => res match {
          case Some(p) => AngularOk(Json.toJson(p))
          case None    => AngularError(s"Producer {${id}} not found")
        })
    }
  }

  def getAll = Action.async { implicit request =>
    {
      ConfigBL.producerBL.getAll.map(
        res => AngularOk(Json.toJson(res)))
    }
  }

  def start(id: String) = Action.async { implicit request =>
    {
      val res = masterActor ? StartProducer(id)
      res.map(r => r match {
        case Left(msg: String)  => AngularOk(msg)
        case Right(err: String) => AngularError(err)
      })
    }
  }

  def stop(id: String) = Action.async { implicit request =>
    {
      val res = masterActor ? StopProducer(id)
      res.map(r => r match {
        case Left(msg: String)  => AngularOk(msg)
        case Right(err: String) => AngularError(err)
      })
    }
  }

  def update = Action.async { implicit request =>
    {
      AngularEditAsync[ProducerModel](producerForm)(producer => {
        ConfigBL.producerBL.update(producer).map(lastErr => WriteResultToAngularRes[ProducerModel](lastErr, false, Some(producer)))
      })

    }
  }

}


