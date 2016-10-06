package it.agilelab.bigdata.wasp.test.elasticsearch

import akka.actor.Props
import akka.pattern.ask
import it.agilelab.bigdata.wasp.core.WaspSystem
import it.agilelab.bigdata.wasp.core.WaspSystem.{actorSystem, timeout}
import it.agilelab.bigdata.wasp.core.elastic._
import it.agilelab.bigdata.wasp.core.logging.WaspLogger
import it.agilelab.bigdata.wasp.core.models.{BSONConversionHelper, PipegraphModel}
import it.agilelab.bigdata.wasp.core.utils.WaspDB
import it.agilelab.bigdata.wasp.web.Global
import reactivemongo.bson.BSONString

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object ElasticAdminTest extends App with WaspSystem with BSONConversionHelper {
  
  
  //def loggerActorName = "InternalLogProducerGuardian"
  def loggerActorProps = Global.loggerActorProps

  def test[T](message : ElasticAdminMessage) = {
    val response = Await.result(actor ? message, timeout.duration).asInstanceOf[T]
    logger.info("response for message [" + message + "]: " + response)
  }

  // test default calls 
  def test1() = {
    // add a default index
    test[Boolean](AddIndex())
    // check if default index exists
    test[Boolean](CheckIndex())
    // add a default alias to default index
    test[Boolean](AddAlias())
    // add a default mapping to default index
    test[Boolean](AddMapping())
    // remove default alias from default index
    test[Boolean](RemoveAlias())
    // remove default alias from default index
    test[Boolean](RemoveMapping())
    // remove default index index
    test[Boolean](RemoveIndex())
  }

  // test alias calls
  def test2() = {
    val addAlias = "add-alias"
    val searchAlias = "search-alias"
    val index01 = "index-01"
    val index02 = "index-02"

    // create first custom index
    test[Boolean](AddIndex(index01))
    // add custom aliases to first index
    test[Boolean](AddAlias(index01, addAlias))
    test[Boolean](AddAlias(index01, searchAlias))
    // create second custom index
    test[Boolean](AddIndex(index02))
    // add custom aliases to second index
    test[Boolean](AddAlias(index02, addAlias))
    test[Boolean](AddAlias(index02, searchAlias))
    // remove add-aliase from first index
    test[Boolean](RemoveAlias(index01, addAlias))
    // remove indexes
    test[Boolean](RemoveIndex(index01))
    test[Boolean](RemoveIndex(index02))
  }

  // test db configuration retrieval
  def test03() = {
    val document = WaspDB.getDB.getDocumentByField[PipegraphModel]("name", BSONString(PipegraphModel.loggerPipegraphName))
    Await.result(document, timeout.duration)
    //WaspDB.close()

    document.onComplete {
      case Failure(result) => println("An error has occured: " + result.getMessage)
      case Success(result) => logger.info("SUCCESS: " /*+ BSONFormats.toJSON(result.get.indexSchema.get).toString*/)
    }
  }

  // retrieve actorSystem
  //val actorSystem = Wasp.actorSystem.get
  // retrieve logger
  val logger = WaspLogger("ElasticAdminActorTest")
  
  logger.info("log di test")
  // create elastic actor
  val actor = actorSystem.actorOf(Props(new ElasticAdminActor()), "ElasticAdminActor")
  // start the elastic client
  //test[Unit](Start())
  // add a connection address
  //test[Boolean](AddNode())
  // check cluster health
  //test[ClusterHealthResponse](CheckCluster())
  // runs test functions
  test1()
  test2()
  test03()
  // remove a connection address
  //test[Boolean](RemoveNode())
  // stop the elastic client
  //test[Boolean](Stop())
  // shutdown wasp system
  WaspSystem.shutdown()
}