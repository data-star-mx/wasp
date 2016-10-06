package it.agilelab.bigdata.wasp.web.utils

import play.api.i18n.Lang
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
import it.agilelab.bigdata.wasp.web.forms.ExtendedForm
import scala.concurrent.Await
import scala.concurrent._
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global
import play.twirl.api.Html
import play.api.Play.current
import play.api.i18n.Messages.Implicits._


trait AngularHelper extends JsonHelper {
	// classes used to 'speak' with utility user


	sealed trait AngularDataRequest[Q] {
		val search: Option[Q]
		val page: Option[Int]
		val pageSize: Option[Int]
		def isPaged = page.isDefined && pageSize.isDefined
	}
	
	sealed trait AngularDataResponse[T] {
		val data: Seq[T]
		val dataTotalCount: Option[Int]
	}	
	
	// NOTE: page should be 1-based index
  	case class DataRequest[Q](
  	    override val search: Option[Q]) extends AngularDataRequest[Q] {
  		override val page = None
  		override val pageSize = None
  	}
  	case class DataResponse[T](
  	    override val data: Seq[T]) extends AngularDataResponse[T] {
  		override val dataTotalCount = None
  	}
  	    
  	case class PagedDataRequest[Q](
  	    override val search: Option[Q],
  	    pg: Int,
  	    pgs: Int
  	    ) extends AngularDataRequest[Q] {
  		override val page = Some(pg)
  		override val pageSize = Some(pgs)
  	}
  	
  	case class PagedDataRequestWithParams[Q](
  	    override val search: Option[Q],
  	    pg: Int,
  	    pgs: Int,
  	    params: Seq[String]
  	    ) extends AngularDataRequest[Q] {
  		override val page = Some(pg)
  		override val pageSize = Some(pgs)
  	}
  	
  	
  	
  	
  	case class PagedDataResponse[T](
  	    override val data: Seq[T],
  	    pc: Int) extends AngularDataResponse[T] {
  		override val dataTotalCount = Some(pc)
  	}
  
	import AngularConnect._

	def Angular(f: JsValue => Result)(implicit request: Request[AnyContent]) = {
		request.body.asJson.map { json =>
  	  	 	f(json)
  	  	} getOrElse Results.BadRequest
	}
  
  def AngularAsync(f: JsValue => Future[Result])(implicit request: Request[AnyContent]) = {
    request.body.asJson.map { json =>
          f(json)
        } getOrElse future { Results.BadRequest }
  }
	
	def AngularModel[T](f: T => Result)(implicit reads: Reads[T], request: Request[AnyContent]) = {
		Angular { json =>
			json.validate[T].map(
			    vm => f(vm)
			).recoverTotal(
			    err => Results.BadRequest
			)
		}
	}
  
  def AngularModelAsync[T](f: T => Future[Result])(implicit reads: Reads[T], request: Request[AnyContent]) = {
    AngularAsync { json =>
      json.validate[T].map(
          vm => f(vm)
      ).recoverTotal(
          err => future { Results.BadRequest }
      )
    }
  }
	
	def AngularForm[T](frm: Form[T])(f: T => Result)(implicit writet: Writes[T], request: Request[AnyContent]) = {
		Angular { json =>
			frm.bind(json).fold(
  	  	 		formWithErrors => AngularError(formWithErrors.errorsAsJson),
  	  	 		vm => f(vm)
  	  		)
		}
	}
  
  def AngularFormAsync[T](frm: Form[T])(f: T => Future[Result])(implicit writet: Writes[T], request: Request[AnyContent]) = {
    AngularAsync { json =>
      frm.bind(json).fold(
            formWithErrors => future { AngularError(formWithErrors.errorsAsJson) },
            vm => f(vm)
          )
    }
  }
	
	def AngularExtendedForm[T](frm: ExtendedForm[T])(f: T => Result)(implicit writet: Writes[T], request: Request[AnyContent]) = {
		Angular { json =>
			frm.bind(json).fold(
  	  	 		formWithErrors => AngularError(formWithErrors.errorsAsJson),
  	  	 		vm => f(vm)
  	  		)
		}
	}	
	
	/*
	 * the [Q] type is the search argument type (can be a string, but other types are allowed too)
	 * the [T] type is the data type for the query
	 * 
	 * in function f, Request can be pattern matched and checked against DataRequest and PagedDataRequest
	 */
	def AngularQuery[Q, T](f: AngularDataRequest[Q] => Either[AngularDataResponse[T], Result])(implicit readq : Reads[Q], writeq : Writes[Q], writet: Writes[T], request: Request[AnyContent]) = {
		import AngularConnect._
		
		AngularModel[AngularQueryRQ[Q]] { json =>
		  	val rsp =	json match {
		  					case AngularQueryRQ(q, None, _, _) => f(DataRequest(q))
		  					case AngularQueryRQ(q, Some(p), Some(pc), None) => f(PagedDataRequest(q, p, pc))
		  					case AngularQueryRQ(q, Some(p), Some(pc), Some(params)) => f(PagedDataRequestWithParams(q, p, pc, params))
		  					case _ => Right(AngularError("Invalid request"))
						}
		  	
		  	rsp match {
		  	  	case Left(adrsp) => AngularOk(Json.toJson(AngularQueryRS.fromRQ(json, adrsp.dataTotalCount.getOrElse(0), adrsp.data)))
		  	  	case Right(result) => result
		  	}
		}
	}
	
	
	/*
	 * the [T] type is the data type for the delete
	 * 
	 * returns the deleted data
	 * 
	 * 
	 * function f should return the deleted data, ore else a custom result (using Left / Right (Either))
	 */
	def AngularDelete[T](f: Seq[T] => Either[Seq[T], Result])(implicit readt : Reads[T], writet: Writes[T], request: Request[AnyContent]) = {
		import AngularConnect._
		
		AngularModel[AngularDeleteRQ[T]] { json =>
		  	val rsp =	f(json.pageData)
		  	
		  	rsp match {
		  	  	case Left(adrsp) => AngularOk(Json.toJson(AngularDeleteRS(adrsp)))
		  	  	case Right(result) => result
		  	}
		}
	}
  
  def AngularSingleDeleteAsync[T](f: T => Future[Either[Result, Result]])(implicit readt : Reads[T], writet: Writes[T], request: Request[AnyContent]) = {
    import AngularConnect._
    
    AngularModelAsync[AngularSingleDeleteRQ[T]] { json =>
        val rsp = f(json.data)
        
        rsp.map( x => x match {
            case Left(result) => result
            case Right(result) => result
        })
    }
  }
  
  def AngularSingleDeleteByIDAsync[T](f: => Future[Either[Result, Result]])(implicit readt : Reads[T], writet: Writes[T], request: Request[AnyContent]) = {
    import AngularConnect._
    
        f.map( x => x match {
            case Left(result) => result
            case Right(result) => result
        })
    
  }
	
	
	/*
	 * the [T] type is the data type for the update
	 * 
	 * returns the deleted data
	 * 
	 * 
	 * function f should return the updated data, ore else a custom result (using Left / Right (Either))
	 */
	def AngularUpdate[T](f: Seq[T] => Either[Seq[T], Result])(implicit readt : Reads[T], writet: Writes[T], request: Request[AnyContent]) = {
		import AngularConnect._
		
		AngularModel[AngularUpdateRQ[T]] { json =>
		  	val rsp =	f(json.pageData)
		  	
		  	rsp match {
		  	  	case Left(adrsp) => AngularOk(Json.toJson(AngularDeleteRS(adrsp)))
		  	  	case Right(result) => result
		  	}
		}
	}
	
	/*
	 * the [T] type is the data type for the update
	 * 
	 * returns the updated data
	 * 
	 * 
	 * function f should return the updated data, ore else a custom result (using Left / Right (Either))
	 */	
	def AngularEdit[T](frm: Form[T])(f: T => Either[T, Result])(implicit writet: Writes[T], request: Request[AnyContent]) = {
		AngularModel[AngularEditRQ] { json =>
			frm.bind(json.data).fold(
  	  	 		formWithErrors => AngularError(formWithErrors.errorsAsJson),
  	  	 		vm => f(vm) match {
  	  	 		  	case Left(mod) =>  AngularOk(Json.toJson(AngularEditRS(Json.toJson(mod))))
  	  	 		  	case Right(result) => result
  	  	 		}
  	  		)
		}
	}
  
  def AngularEditAsync[T](frm: Form[T])(f: T => Future[Either[T, Result]])(implicit writet: Writes[T], request: Request[AnyContent]) = {

		AngularModelAsync[AngularEditRQ] { json =>
      frm.bind(json.data).fold(
            formWithErrors => future { AngularError(formWithErrors.errorsAsJson)} ,
            vm => f(vm).map( x => x match {
                case Left(mod) =>  AngularOk(Json.toJson(AngularEditRS(Json.toJson(mod))))
                case Right(result) => result
            })
          )
    }
  }
  
  

	
	/*
	 * the ExtendedForm version of AngularEdit
	 * 
	 */	
	def AngularExtendedEdit[T](frm: ExtendedForm[T])(f: T => Either[T, Result])(implicit writet: Writes[T], request: Request[AnyContent]) = {
		AngularModel[AngularEditRQ] { json =>
			frm.bind(json.data).fold(
  	  	 		formWithErrors => AngularError(formWithErrors.errorsAsJson),
  	  	 		vm => f(vm) match {
  	  	 		  	case Left(mod) =>  AngularOk(Json.toJson(AngularEditRS(Json.toJson(mod))))
  	  	 		  	case Right(result) => result
  	  	 		}
  	  		)
		}
	}	
  
	val AngularOk = buildAngularOkResponse(None)
	def AngularOk(msg: String) = buildAngularOkResponse(Some(Json.toJson(msg)))
	def AngularOk(json: JsValue) = buildAngularOkResponse(Some(json))
	def AngularOk(json: Option[JsValue]) = buildAngularOkResponse(json)
	
	val AngularError = buildAngularOkResponse(None)
	def AngularError(msg: String) = buildAngularErrorResponse(Some(Json.toJson(Html(msg).toString)))
	def AngularError(json: JsValue) = buildAngularErrorResponse(Some(json))
	def AngularError(json: Option[JsValue]) = buildAngularErrorResponse(json)
	
	def buildAngularOkResponse(json: Option[JsValue], s: Option[Results.Status] = None) = {
		val dat : Option[() => (String, JsValueWrapper)] = json.map(v => () => "data" -> v)
		val baseResult = buildCMResponseExt(s getOrElse Results.Ok)(true, None, dat.toList:_*);
		baseResult
	}
	
	def buildAngularErrorResponse(json: Option[JsValue], s: Option[Results.Status] = None) = {
		val dat : Option[() => (String, JsValueWrapper)] = json.map(v => () => "error" -> v)
		val baseResult = buildCMResponseExt(s getOrElse Results.Ok)(false, None, dat.toList:_*);
		baseResult
	}
}

