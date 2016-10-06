package it.agilelab.bigdata.wasp.consumers

import akka.actor.ActorSystem
import it.agilelab.bigdata.wasp.consumers.readers.{ElasticIndexReader}
import it.agilelab.bigdata.wasp.consumers.writers.ElasticSparkWriter
import it.agilelab.bigdata.wasp.core.WaspSystem
import it.agilelab.bigdata.wasp.core.bl.AllBLsTestWrapper
import it.agilelab.bigdata.wasp.core.elastic.{CheckIndex, RemoveIndex, Search}
import it.agilelab.bigdata.wasp.core.models.IndexModel
import it.agilelab.bigdata.wasp.core.utils.{BSONFormats, WaspDB}
import org.elasticsearch.action.search.SearchResponse
import org.scalatest.Matchers._
import org.scalatest._
import reactivemongo.bson.{BSON, BSONObjectID}
import spire.implicits

/**
 * Created by Mattia Bertorello on 06/10/15.
 */
case class SimpleData(id: String, name: String)

class ElasticWriterSpec extends SparkFlatSpec with BeforeAndAfter {
  behavior of "Elastic writer"

  val indexModelName = "testindexmodel"
  before {
    WaspSystem.actorSystem = actorSystem
    WaspDB.DBInitialization(actorSystem = actorSystem)
    WaspSystem.systemInitialization(actorSystem = actorSystem)
    if (WaspSystem.??[Boolean](WaspSystem.elasticAdminActor, CheckIndex(index = indexModelName))) {
      WaspSystem.??[Boolean](WaspSystem.elasticAdminActor, RemoveIndex(index = indexModelName))
    }
  }

  after {
    if (WaspSystem.??[Boolean](WaspSystem.elasticAdminActor, CheckIndex(index = indexModelName))) {
      WaspSystem.??[Boolean](WaspSystem.elasticAdminActor, RemoveIndex(index = indexModelName))
    }
  }

  it should "throw an exception if there is no schema" in {
    val testBL = new AllBLsTestWrapper
    val indexId = BSONObjectID.generate
    val indexModel = IndexModel(name = "testindexmodel", creationTime = 1l, schema = None, _id = Some(indexId))
    testBL.indexBL.persist(indexModel)

    val sparkWriter = new ElasticSparkWriter(testBL, sc, indexId.stringify)

    val testRDD = sc.parallelize(Seq(SimpleData("idData1", "name1"), SimpleData("idData2", "name2")))
    val sqlContext1 = sqlContext
    import sqlContext1.implicits._
    val testDataframe = testRDD.toDF()


    the[Exception] thrownBy {
      sparkWriter.write(testDataframe)
    } should have message s"There no define schema in the index configuration: $indexModel"

  }

  it should "write something on elastic" in {
    val testBL = new AllBLsTestWrapper
    val indexId = BSONObjectID.generate
    val schema = s"""
    {"simpledata":
        {"properties":{
          "id":{"type":"string","index":"not_analyzed","store":"true","enabled":"true"},
          "name":{"type":"string","index":"not_analyzed","store":"true","enabled":"true"}
        }}
    }"""

    val indexModel = IndexModel(name = "testindexmodel", creationTime = 1l, schema = BSONFormats.fromString(schema), _id = Some(indexId))
    testBL.indexBL.persist(indexModel)

    val sparkWriter = new ElasticSparkWriter(testBL, sc, indexId.stringify)

    val testRDD = sc.parallelize(Seq(SimpleData("idData1", "name1"), SimpleData("idData2", "name2")))
    val testDataframe = sqlContext.createDataFrame(testRDD)


    sparkWriter.write(testDataframe)
    val searchQuery = Search(indexModel.name, None, None, 0, 2)
    val result = WaspSystem.??[SearchResponse](WaspSystem.elasticAdminActor, searchQuery)
    result.getHits.getHits.length shouldBe 2
    result.getHits.getHits.map(_.getSourceAsString) should contain("""{"id":"idData2","name":"name2"}""")
    result.getHits.getHits.map(_.getSourceAsString) should contain("""{"id":"idData1","name":"name1"}""")
    val result1 = WaspSystem.??[Boolean](WaspSystem.elasticAdminActor, RemoveIndex(index = indexModel.name))
    result1 shouldBe true

    val result2 = WaspSystem.??[Boolean](WaspSystem.elasticAdminActor, CheckIndex(index = indexModel.name))

    result2 shouldBe false
  }

  it should "read from a query" in {
    val testBL = new AllBLsTestWrapper
    val indexId = BSONObjectID.generate
    val schema = s"""
    {"simpledata":
        {"properties":{
          "id":{"type":"string","index":"not_analyzed","store":"true","enabled":"true"},
          "name":{"type":"string","index":"not_analyzed","store":"true","enabled":"true"}
        }}
    }"""

    val indexModel = IndexModel(name = indexModelName, creationTime = 1l, schema = BSONFormats.fromString(schema), _id = Some(indexId))
    testBL.indexBL.persist(indexModel)

    val sparkWriter = new ElasticSparkWriter(testBL, sc, indexId.stringify)


    val testRDD = sc.parallelize(Seq(SimpleData("idData1", "name1"), SimpleData("idData2", "name2")))
    val testDataframe = sqlContext.createDataFrame(testRDD)

    sparkWriter.write(testDataframe)

    val reader = new ElasticIndexReader(indexModel.copy(query = Some("""{query: {query_string: {query: "id = idData1"}}}""")))

   val resultDF = reader.read(sc).toJSON.collect()
    println(resultDF.mkString("\n"))
    resultDF should have length 1
    resultDF.head shouldBe """{"id":"idData1","name":"name1"}"""

  }


  }
