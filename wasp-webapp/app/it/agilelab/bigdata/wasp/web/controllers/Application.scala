package it.agilelab.bigdata.wasp.web.controllers

import it.agilelab.bigdata.wasp.core.models.BSONConversionHelper
import play.api._
import play.api.mvc._
import play.twirl.api.Html
import it.agilelab.bigdata.wasp.web.utils.AngularHelper




object Application extends WaspController {

  
  def javascriptRoutes = Action { implicit request =>
	  Ok(
	    Routes.javascriptRouter("jsRoutes")(
				routes.javascript.Topic_C.getById,
	      routes.javascript.Pipegraph_C.getAll,
	      routes.javascript.Pipegraph_C.getById,
        routes.javascript.Pipegraph_C.getByName,
	      routes.javascript.Pipegraph_C.insert,
	      routes.javascript.Pipegraph_C.update,
	      routes.javascript.Pipegraph_C.delete,
	      routes.javascript.Pipegraph_C.start,
	      routes.javascript.Pipegraph_C.stop,
				routes.javascript.BatchJob_C.getAll,
				routes.javascript.BatchJob_C.getById,
				routes.javascript.BatchJob_C.insert,
				routes.javascript.BatchJob_C.start,
				routes.javascript.BatchJob_C.update,
				routes.javascript.BatchJob_C.delete,
				routes.javascript.Index_C.getByName,
				routes.javascript.MlModels_C.getById,
				routes.javascript.MlModels_C.getAll,
				routes.javascript.MlModels_C.update,
				routes.javascript.MlModels_C.delete,
	      routes.javascript.Producer_C.getAll,
	      routes.javascript.Producer_C.getById,  
	      routes.javascript.Producer_C.update,
	      routes.javascript.Producer_C.start,
	      routes.javascript.Producer_C.stop,
	      routes.javascript.Configuration_C.getKafka,
	      routes.javascript.Configuration_C.getSparkBatch,
	      routes.javascript.Configuration_C.getSparkStreaming,
	      routes.javascript.Configuration_C.getES,
	      routes.javascript.Configuration_C.getSolr,
				routes.javascript.BatchJob_C.getById
	    )
	  ).as("text/javascript")
	}

}


