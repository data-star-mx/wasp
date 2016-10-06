package it.agilelab.bigdata.wasp.web.controllers

import akka.pattern.ask
import akka.util.Timeout
import it.agilelab.bigdata.wasp.core.WaspSystem.masterActor
import it.agilelab.bigdata.wasp.core.bl.ConfigBL
import it.agilelab.bigdata.wasp.core.logging.WaspLogger
import it.agilelab.bigdata.wasp.core.models._
import it.agilelab.bigdata.wasp.core.utils.{BSONFormats}
import it.agilelab.bigdata.wasp.master.StartBatchJob
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
object BatchJob_C extends WaspController {

  val logger = WaspLogger("BatchJob")

  import BSONFormats._

  implicit def wtF = Json.format[WriteType]
  implicit def wF = Json.format[WriterModel]
  implicit def sF = Json.format[StrategyModel]
  implicit def rmF = Json.format[ReaderModel]
  implicit def mF = Json.format[MlModelOnlyInfo]
  implicit def eF = Json.format[ETLModel]
  implicit def bF = Json.format[BatchJobModel]
  implicit val timeout = Timeout(30 seconds)

  val batchjobForm: Form[BatchJobModel] = Form(
    mapping(
      "name" -> text,
      "description" -> text,
      "owner" -> text,
      "system" -> boolean,
      "etl" -> mapping(
        "name" -> text,
        "inputs" -> list(mapping(
          "id" -> text,
          "name" -> text,
          "readerType" -> text
        )((id, name, readerType) => ReaderModel(BSONObjectID(id), name, readerType))(r => Some(r.id.stringify, r.name, r.readerType))),
        "output" -> mapping(
          "id" -> text,
          "name" -> text,
          "writerType" -> mapping(
            "wtype" -> text,
            "product" -> text
          )((wtype, product) => new WriteType(wtype, product))(w => Some(w.wtype, w.product))
        )((id, name, writeType) => WriterModel(BSONObjectID(id), name, writeType))(w => Some(w.id.stringify, w.name, w.writerType)),
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
        ((name, inputs, output, mlModels, strategy) => ETLModel(name, inputs, output, mlModels, strategy))(e => Some(e.name, e.inputs, e.output, e.mlModels, e.strategy)),
      "state" -> text,
      "id" -> optional(text))(
        (name, description, owner, system, etl, state, id) => BatchJobModel(name, description, owner, system, now.getTime, etl, state, id.map(_id => BSONObjectID(_id))))(
        p => Some(p.name, p.description, p.owner, p.system, p.etl, p.state, p._id.map(id => id.toString()))))

  def getById(id: String) = Action.async { implicit request =>
    {
      ConfigBL.batchJobBL.getById(id).map {
        case Some(b) => AngularOk(Json.toJson(b))
        case None => AngularError(s"BatchJob {${id}} not found")
      }
    }
  }

  def getAll = Action.async { implicit request =>
    {
      ConfigBL.batchJobBL.getAll.map(
        res => AngularOk(Json.toJson(res)))
    }
  }

  def insert = Action.async { implicit request =>
    {
      AngularEditAsync[BatchJobModel](batchjobForm)(batchjob => {
        ConfigBL.batchJobBL.insert(batchjob).map { lastErr => WriteResultToAngularRes[BatchJobModel](lastErr, errorIfNotUpdated = false, Some(batchjob)) }
      })
    }
  }

  def start(id: String) = Action.async { implicit request =>
  {
    logger.info(s"Starting BatchJob $id . Sending to masterActor the StartBatchJob message")
    val res = masterActor ? StartBatchJob(id)
    res.map {
      case Left(msg: String) => AngularOk(msg)
      case Right(err: String) => AngularError(err)
    }
  }

  }

  def update = Action.async { implicit request =>
  {
    AngularEditAsync[BatchJobModel](batchjobForm)(batchjob => {
      ConfigBL.batchJobBL.update(batchjob).map(lastErr => WriteResultToAngularRes[BatchJobModel](lastErr, errorIfNotUpdated = false, Some(batchjob)))
    })

  }
  }

  def delete(id: String) = Action.async { implicit request =>
  {
    AngularSingleDeleteByIDAsync[BatchJobModel](
      ConfigBL.batchJobBL.deleteById(id).map(
        lastErr => WriteResultToAngularResOnDelete(lastErr)))
  }
  }

}