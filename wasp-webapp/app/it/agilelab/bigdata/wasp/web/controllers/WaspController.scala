package it.agilelab.bigdata.wasp.web.controllers

import it.agilelab.bigdata.wasp.core.models.BSONConversionHelper
import play.api._
import play.api.mvc._
import it.agilelab.bigdata.wasp.web.utils.MongoAngularHelper
import it.agilelab.bigdata.wasp.web.utils.BaseUtils

abstract class WaspController extends Controller with MongoAngularHelper with BSONConversionHelper with BaseUtils