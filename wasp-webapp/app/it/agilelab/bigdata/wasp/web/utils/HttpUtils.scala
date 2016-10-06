package it.agilelab.bigdata.wasp.web.utils

import play.api.mvc.Request
import play.api.mvc.AnyContent
import java.util.Date
import java.util.Locale
import java.sql.Timestamp

object HttpUtils extends BaseUtils {
  def composeFullUrl(secure : Boolean, hostName : String, relativeUrl : String) =
    "http" + (if (secure) "s" else "") + "://" + hostName + relativeUrl

  def parseCheckStringsListForm(checkName : String, request : Request[AnyContent]) = {

    var mapcheck = request.body.asFormUrlEncoded
    if (mapcheck.isDefined) {
      if (mapcheck.get.contains(checkName)) {
        var elemCheked = mapcheck.get(checkName)
        Some(elemCheked)
      }
      else {
        None
      }
    }
    else {
      None
    }
  }

  def parseCheckIdListForm(checkName : String, request : Request[AnyContent]) = {

    var mapcheck = request.body.asFormUrlEncoded
    if (mapcheck.isDefined) {
      if (mapcheck.get.contains(checkName)) {
        var elemCheked = mapcheck.get(checkName).map(c => c.toLong)
        Some(elemCheked)
      }
      else {
        None
      }
    }
    else {
      None
    }
  }

  def getFormField(fieldName : String, request : Request[AnyContent]) = {

    var fieldmap = request.body.asFormUrlEncoded
    if (fieldmap.isDefined) {
      if (fieldmap.get.contains(fieldName)) {
        fieldmap.get(fieldName).headOption
      }
      else {
        None
      }
    }
    else {
      None
    }
  }

  def getSingleLongFieldForm(fieldName : String, request : Request[AnyContent]) = {
    var res = getFormField(fieldName, request)
    res.map(f => f.toLong)
  }

  def getSingleStringFieldForm(fieldName : String, request : Request[AnyContent]) = {
    var res = getFormField(fieldName, request)
    res.map(f => f.toString())
  }

  def getTimestampFormField(fieldName : String, request : Request[AnyContent]) = {
    var res = getFormField(fieldName, request)
    res.map(f => new Timestamp(f.toLong))
  }

  def getBooleanFormField(fieldName : String, request : Request[AnyContent]) = {
    var res = getFormField(fieldName, request)
    res.map(f => f.toBoolean)
  }

  def getDateFieldForm(fieldName : String, format : String, request : Request[AnyContent]) = {
    val res = getFormField(fieldName, request)
    res.get.parseDate(format)
  }

  def getBooleanFieldForm(fieldName : String, request : Request[AnyContent]) = {
    val res = getFormField(fieldName, request)
    res.get.toBoolean
  }
}