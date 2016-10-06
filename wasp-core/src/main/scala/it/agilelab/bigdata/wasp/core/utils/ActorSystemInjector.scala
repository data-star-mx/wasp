package it.agilelab.bigdata.wasp.core.utils

import com.typesafe.config.ConfigFactory
import it.agilelab.bigdata.wasp.core.WaspSystem

trait ActorSystemInjector {

  def actorSystemName = "Wasp"

  def actorSystemConfig = ConfigFactory.parseString("akka.remote.netty.tcp.port = 2551")

  WaspSystem.initializeActorSystem(actorSystemName, actorSystemConfig)
}