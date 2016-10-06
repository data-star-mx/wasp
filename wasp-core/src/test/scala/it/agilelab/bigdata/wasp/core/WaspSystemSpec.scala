package it.agilelab.bigdata.wasp.core

import akka.actor.ActorSystem
import it.agilelab.bigdata.wasp.core.bl.AllBLsTestWrapper
import it.agilelab.bigdata.wasp.core.utils.WaspDB
import org.scalatest.FlatSpec
import org.scalatest.concurrent.ScalaFutures

/**
 * Created by Mattia Bertorello on 07/10/15.
 */
class WaspSystemSpec extends FlatSpec  with ScalaFutures {
  behavior of "Wasp System"

  it should "initialize all system" in {
    println("prova")
    implicit val system: ActorSystem = ActorSystem()
    WaspDB.DBInitialization(system)
    WaspSystem.systemInitialization(system, force = true)

  }


}