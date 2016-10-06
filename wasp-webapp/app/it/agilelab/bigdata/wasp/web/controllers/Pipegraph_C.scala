package it.agilelab.bigdata.wasp.web.controllers

import akka.pattern.ask
import akka.util.Timeout
import it.agilelab.bigdata.wasp.core.WaspSystem.masterActor
import it.agilelab.bigdata.wasp.core.bl.ConfigBL
import it.agilelab.bigdata.wasp.core.logging.WaspLogger
import it.agilelab.bigdata.wasp.core.models._
import it.agilelab.bigdata.wasp.core.utils.{BSONFormats}
import it.agilelab.bigdata.wasp.master.{StartPipegraph, StopPipegraph}
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.Json
import play.api.mvc._
import reactivemongo.bson._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

object Pipegraph_C extends WaspController {

  val logger = WaspLogger("Pipegraph")

  import BSONFormats._

  implicit def tF = Json.format[TopicModel]
  implicit def iF = Json.format[IndexModel]
  implicit def wtF = Json.format[WriteType]
  implicit def wF = Json.format[WriterModel]
  implicit def sF = Json.format[StrategyModel]
  implicit def rmF = Json.format[ReaderModel]
  implicit def mF = Json.format[MlModelOnlyInfo]
  implicit def eF = Json.format[ETLModel]
  implicit def rF = Json.format[RTModel]
  implicit def kF = Json.format[DashboardModel]
  implicit def pF = Json.format[PipegraphModel]
  implicit val timeout = Timeout(30 seconds)

  val pipegraphForm = Form(
    mapping(
      "name" -> text,
      "description" -> text,
      "owner" -> text,
      "system" -> boolean,
      "isActive" -> boolean,
      "etl" -> list(mapping(
        "name" -> text,
        "inputs" -> list(mapping(
          "id" -> text,
          "name" -> text,
          "readerType"  -> text
        )((id, name, readerType) => ReaderModel(BSONObjectID(id), name, readerType))(r => Some(r.id.stringify, r.name, r.readerType))),
        "output" -> mapping(
          "id" -> text,
          "name" -> text,
          "writerType" -> mapping(
            "wtype" -> text,
            "product" -> text
          )((wtype, product) => new WriteType(wtype, product))(w => Some(w.wtype, w.product))
        )((id, name, writerType) => WriterModel(BSONObjectID(id), name, writerType))(w => Some(w.id.stringify, w.name, w.writerType)),
        "mlModels" -> list(mapping(
          "id" -> optional(text),
          "name" -> text,
          "version" -> text,
          "timestamp" -> optional(longNumber),
          "description" -> text
        )((id, name, version, timestamp, description) => MlModelOnlyInfo(_id = id.map(BSONObjectID(_)), name = name, version = version, timestamp = timestamp, description = description))(w => Some(w._id.map(_.toString()), w.name, w.version, w.timestamp, w.description))),
        "strategy" -> optional(mapping(
          "className" -> text,
          "configuration" -> optional(text))(
            (className, configuration) =>
              StrategyModel(className, configuration.flatMap(c => fromString(c))))(
            s => Some(s.className, s.configuration.map(c => BSONFormats.toString(c))))))
        ((name, inputs, output, mlModels, strategy) => ETLModel(name, inputs, output, mlModels, strategy))(e => Some(e.name, e.inputs, e.output, e.mlModels, e.strategy))
      ),
      "rt" -> list(mapping(
        "name" -> text,
        "inputs" -> list(mapping(
          "id" -> text,
          "name" -> text,
          "readerType"  -> text
        )((id, name, readerType) => ReaderModel(BSONObjectID(id), name, readerType))(r => Some(r.id.stringify, r.name, r.readerType)))
        )((name, inputs) => RTModel(name, inputs))(rt => Some(rt.name, rt.inputs))),
      "dashboard" -> optional(mapping(
        "url" -> text,
        "needsFilterBox" -> boolean)(DashboardModel.apply)(DashboardModel.unapply)),
      "id" -> optional(text))(
        (name, description, owner, system, isActive, etl, rt, dashboard, id) => PipegraphModel(name, description, owner, system, now.getTime, etl, rt, dashboard, isActive, id.map(_id => BSONObjectID(_id))))(
          p => Some(p.name, p.description, p.owner, p.system, p.isActive, p.etl, p.rt, p.dashboard, p._id.map(id => id.toString()))))

  def getById(id: String) = Action.async { implicit request =>
    {
      ConfigBL.pipegraphBL.getById(id).map(
        res => res match {
          case Some(p) => AngularOk(Json.toJson(p))
          case None    => AngularError(s"Pipegraph {${id}} not found")
        })
    }
  }

  def getByName(name: String) = Action.async { implicit request =>
    {
      ConfigBL.pipegraphBL.getByName(name).map(
        res => res match {
          case Some(p) => AngularOk(Json.toJson(p))
          case None    => AngularError(s"Pipegraph {${name}} not found")
        })
    }
  }

  def getAll = Action.async { implicit request =>
    {
      ConfigBL.pipegraphBL.getAll.map(
        res => AngularOk(Json.toJson(res)))
    }
  }

  def insert = Action.async { implicit request =>
    {
      AngularEditAsync[PipegraphModel](pipegraphForm)(pipegraph => {
        ConfigBL.pipegraphBL.insert(pipegraph).map { lastErr => WriteResultToAngularRes[PipegraphModel](lastErr, false, Some(pipegraph)) }
      })
    }
  }

  def update = Action.async { implicit request =>
    {
      AngularEditAsync[PipegraphModel](pipegraphForm)(pipegraph => {
        ConfigBL.pipegraphBL.update(pipegraph).map(lastErr => WriteResultToAngularRes[PipegraphModel](lastErr, false, Some(pipegraph)))
      })

    }
  }

  def delete(id: String) = Action.async { implicit request =>
    {
      AngularSingleDeleteByIDAsync[PipegraphModel](
        ConfigBL.pipegraphBL.deleteById(id).map(
          lastErr => WriteResultToAngularResOnDelete(lastErr)))
    }
  }

  def start(id: String) = Action.async { implicit request =>
    {

      val res = masterActor ? StartPipegraph(id)
      res.mapTo[Either[String, String]].map(r => r match {
        case Left(msg: String)  => AngularOk(msg)
        case Right(err: String) => AngularError(err)
      })
    }
  }

  def stop(id: String) = Action.async { implicit request =>
    {
      val res = masterActor ? StopPipegraph(id)
      res.mapTo[Either[String, String]].map(r => r match {
        case Left(msg: String)  => AngularOk(msg)
        case Right(err: String) => AngularError(err)
      })
    }
  }
}