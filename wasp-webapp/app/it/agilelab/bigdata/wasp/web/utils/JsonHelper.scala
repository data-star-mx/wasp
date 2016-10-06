package it.agilelab.bigdata.wasp.web.utils

import scala.concurrent.duration.DurationInt
import scala.reflect.runtime.universe._
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.mvc._

trait JsonHelper {
  
  
  def buildJsonErrorResponse(errorMsg : Option[String] = None, s : Results.Status = Results.Ok) =
    buildCMResponse(s)(false, errorMsg, null)

  def buildJsonErrorResponseExt(errorMsg : Option[String], s : Results.Status, buildFunction : () => (String, JsValueWrapper)*) =
    buildCMResponseExt(s)(false, errorMsg, buildFunction : _*)

  def buildJsonOKResponse(buildFunction : () => (String, JsValueWrapper) = null, s : Results.Status = Results.Ok) =
    buildCMResponse(s)(true, None, buildFunction)

  def buildJsonOKResponseExt(buildFunction : () => (String, JsValueWrapper)*) =
    buildCMResponseExt(Results.Ok)(true, None, buildFunction : _*)

  def jsonObjToResponse(json : JsObject, s : Results.Status = Results.Ok) : Result = {
    val jsonStr = json.toString
    s(toJson(json))
  }

  protected def buildCMResponse(s : Results.Status)(isOk : Boolean, errorMsg : Option[String], buildFunction : () => (String, JsValueWrapper)) : Result =
    if (buildFunction == null) buildCMResponseExt(s)(isOk, errorMsg)
    else buildCMResponseExt(s)(isOk, errorMsg, buildFunction)

  protected def buildCMResponseExt(s : Results.Status)(isOk : Boolean, errorMsg : Option[String], buildFunction : () => (String, JsValueWrapper)*) : Result =
    jsonObjToResponse(Json.obj(
      List[(String, JsValueWrapper)]("Result" -> (if (!isOk) "KO" else "OK")) ++
        errorMsg.map[(String, JsValueWrapper)](m => "ErrorMsg" -> m).toList ++
        buildFunction.map(b => b()) : _*), s)
}

