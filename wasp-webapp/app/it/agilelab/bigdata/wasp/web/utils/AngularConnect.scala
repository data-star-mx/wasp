package it.agilelab.bigdata.wasp.web.utils

import play.api.libs.json.JsValue
import play.api.mvc.Results
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.data.Form
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json.Json
import play.api.libs.json.JsPath
import play.api.libs.json.JsObject


object AngularConnect {
	case class AngularQueryRQ[Q](
		search: Option[Q],
		page: Option[Int],
		pageSize: Option[Int],
		params: Option[Seq[String]])
	
	case class AngularQueryRS[Q, T](
	    search: Option[Q],
		page: Option[Int],
		pageSize: Option[Int],
		dataCount: Int,
		pageData: Seq[T])
	object AngularQueryRS {
		def fromRQ[Q, T](rq: AngularQueryRQ[Q], dataCount: Int, pageData: Seq[T]) =
			AngularQueryRS(rq.search, rq.page, rq.pageSize, dataCount, pageData)
	}
	
	case class AngularSingleDeleteRQ[T](
      data: T)
  
  
  
	case class AngularDeleteRQ[T](
	    pageData: Seq[T])
	    
	case class AngularDeleteRS[T](
	    pageData: Seq[T])
	    
	
	
	case class AngularUpdateRQ[T](
	    pageData: Seq[T])
	
	case class AngularUpdateRS[T](
	    pageData: Seq[T])
	    
	    
	case class AngularEditRQ(
	    data: JsValue)
	
	case class AngularEditRS(
	    data: JsValue)  	    
  
	import play.api.libs.functional.syntax._

	
	implicit def queryRqReads[Q](implicit readsq: play.api.libs.json.Reads[Q]) : Reads[AngularQueryRQ[Q]] = (
	  (JsPath \ "search").readNullable[Q] and
	  (JsPath \ "page").readNullable[Int] and
	  (JsPath \ "pageSize").readNullable[Int] and
	  (JsPath \ "params").readNullable[Seq[String]]
	  
	)(AngularQueryRQ.apply[Q] _)	
	
	implicit def queryRqWrites[Q](implicit writesq: play.api.libs.json.Writes[Q]) : Writes[AngularQueryRQ[Q]] = (
	  (JsPath \ "search").writeNullable[Q] and
	  (JsPath \ "page").writeNullable[Int] and
	  (JsPath \ "pageSize").writeNullable[Int] and
	  (JsPath \ "params").writeNullable[Seq[String]]
	)(unlift(AngularQueryRQ.unapply[Q]))
	
	// NOTE: this 'manual' reads and writes for complex types are required because of lack of support (or bug) giving 'No apply function found matching unapply parameters' error in 
	// 			Json.scala -> JsMacroImpl.scala	
	implicit def queryRsReads[Q, T](implicit readsq: play.api.libs.json.Reads[Q], readst: play.api.libs.json.Reads[T]) : Reads[AngularQueryRS[Q, T]] = (
	  (JsPath \ "search").readNullable[Q] and
	  (JsPath \ "page").readNullable[Int] and
	  (JsPath \ "pageSize").readNullable[Int] and
	  (JsPath \ "dataCount").read[Int] and
	  (JsPath \ "pageData").read[Seq[T]]
	)(AngularQueryRS.apply[Q, T] _)

	implicit def queryRsWrites[Q, T](implicit writesq: play.api.libs.json.Writes[Q], writest: play.api.libs.json.Writes[T]) : Writes[AngularQueryRS[Q, T]] = (
	  (JsPath \ "search").writeNullable[Q] and
	  (JsPath \ "page").writeNullable[Int] and
	  (JsPath \ "pageSize").writeNullable[Int] and
	  (JsPath \ "dataCount").write[Int] and
	  (JsPath \ "pageData").write[Seq[T]]
	)(unlift(AngularQueryRS.unapply[Q, T]))
	
	// D'OH: Json combinators doesn't work for single field case class.
	// http://stackoverflow.com/questions/14754092/how-to-turn-json-to-case-class-when-case-class-has-only-one-field
	// using alternatives
	implicit def deleteRqReads[T](implicit reads: play.api.libs.json.Reads[T]) : Reads[AngularDeleteRQ[T]] = 
		(JsPath \ "pageData").read[Seq[T]].map{ s => AngularDeleteRQ(s) }

	implicit def deleteRqWrites[T](implicit writes: play.api.libs.json.Writes[T]) : Writes[AngularDeleteRQ[T]] =
	    (JsPath \ "pageData").write[Seq[T]].contramap { (delrq: AngularDeleteRQ[T]) => delrq.pageData }
	    
	implicit def deleteRsReads[T](implicit reads: play.api.libs.json.Reads[T]) : Reads[AngularDeleteRS[T]] = 
		(JsPath \ "pageData").read[Seq[T]].map{ s => AngularDeleteRS(s) }

	implicit def deleteRsWrites[T](implicit writes: play.api.libs.json.Writes[T]) : Writes[AngularDeleteRS[T]] =
	    (JsPath \ "pageData").write[Seq[T]].contramap { (delrq: AngularDeleteRS[T]) => delrq.pageData }	

  implicit def deleteSingleRqReads[T](implicit reads: play.api.libs.json.Reads[T]) : Reads[AngularSingleDeleteRQ[T]] = 
    (JsPath \ "data").read[T].map{ s => AngularSingleDeleteRQ(s) }

  implicit def deleteSingleRqWrites[T](implicit writes: play.api.libs.json.Writes[T]) : Writes[AngularSingleDeleteRQ[T]] =
      (JsPath \ "data").write[T].contramap { (delrq: AngularSingleDeleteRQ[T]) => delrq.data }
      
  

	implicit def updateRqReads[T](implicit reads: play.api.libs.json.Reads[T]) : Reads[AngularUpdateRQ[T]] = 
		(JsPath \ "pageData").read[Seq[T]].map{ s => AngularUpdateRQ(s) }

	implicit def updateRqWrites[T](implicit writes: play.api.libs.json.Writes[T]) : Writes[AngularUpdateRQ[T]] =
	    (JsPath \ "pageData").write[Seq[T]].contramap { (delrq: AngularUpdateRQ[T]) => delrq.pageData }
	    
	implicit def updateRsReads[T](implicit reads: play.api.libs.json.Reads[T]) : Reads[AngularUpdateRS[T]] = 
		(JsPath \ "pageData").read[Seq[T]].map{ s => AngularUpdateRS(s) }

	implicit def updateRsWrites[T](implicit writes: play.api.libs.json.Writes[T]) : Writes[AngularUpdateRS[T]] =
	    (JsPath \ "pageData").write[Seq[T]].contramap { (delrq: AngularUpdateRS[T]) => delrq.pageData }
	
	implicit def editRqReads = Json.reads[AngularEditRQ] 
	
	implicit def editRqWrites = Json.writes[AngularEditRQ] 
	
	implicit def editRsReads = Json.reads[AngularEditRS] 
	
	implicit def editRsWrites = Json.writes[AngularEditRS] 
}