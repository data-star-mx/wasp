package it.agilelab.bigdata.wasp.web


import _root_.controllers.Assets
import akka.actor.Props
import it.agilelab.bigdata.wasp.core.WaspSystem
import it.agilelab.bigdata.wasp.core.WaspSystem.{actorSystem, masterActor}
import it.agilelab.bigdata.wasp.core.bl.ConfigBL
import it.agilelab.bigdata.wasp.core.logging.LoggerInjector
import it.agilelab.bigdata.wasp.core.utils.{WaspDB, ActorSystemInjector}
import it.agilelab.bigdata.wasp.master.MasterGuardian
import it.agilelab.bigdata.wasp.producers.InternalLogProducerGuardian
import org.apache.log4j.Level
import play.api.mvc.{RequestHeader, Handler}
import play.api.{Application, GlobalSettings, Logger}
import play.api.routing._
import play.api.routing.sird._
import play.api.mvc._


object Global extends GlobalBase{

  override def onRequestOverride(req: RequestHeader): Option[Handler] = {
    Logger.info("Global onRequestOverride")
    None
  }

}

trait GlobalBase extends GlobalSettings with ActorSystemInjector with LoggerInjector {

  org.apache.log4j.Logger.getLogger("org").setLevel(Level.WARN)
  org.apache.log4j.Logger.getLogger("akka").setLevel(Level.WARN)
  
  override def loggerActorProps : Props = Props(new InternalLogProducerGuardian(ConfigBL))

  override def onStart(app : Application) {

    Logger.info("starting app: " + app.configuration.getString("application.version").getOrElse("no version"))
    
    val startMaster = app.configuration.getString("master.start").getOrElse("true")
    val actorSystem = WaspSystem.actorSystem
    WaspDB.DBInitialization(actorSystem)
    WaspSystem.systemInitialization(actorSystem)
    if(startMaster == "true") {
      masterActor = WaspSystem.actorSystem.actorOf(Props(new MasterGuardian(ConfigBL)))
    }
  }

  def onRequestOverride(req: RequestHeader): Option[Handler]

  override def onRouteRequest(req: RequestHeader): Option[Handler] = {
    Logger.info(s"onRouteRequest: ${req.toString()}")
    onRequestOverride(req) orElse super.onRouteRequest(req)
  }

}
