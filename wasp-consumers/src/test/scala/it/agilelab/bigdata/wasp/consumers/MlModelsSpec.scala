package it.agilelab.bigdata.wasp.consumers

import akka.actor.{Actor, ActorSystem}
import akka.testkit.TestActorRef
import akka.util.Timeout
import it.agilelab.bigdata.wasp.consumers.MlModels.{MlModelsBroadcastDB, MlModelsDB, TransformerWithInfo}
import it.agilelab.bigdata.wasp.consumers.batch.BatchJobActor
import it.agilelab.bigdata.wasp.consumers.consumers.ConsumerEtlActor
import it.agilelab.bigdata.wasp.consumers.utils.{SparkWriterTestWrapper, StreamingReaderTestWrapper}
import it.agilelab.bigdata.wasp.core.WaspSystem
import it.agilelab.bigdata.wasp.core.bl.{AllBLsTestWrapper, BatchJobBL, MlModelBL}
import it.agilelab.bigdata.wasp.core.models._
import it.agilelab.bigdata.wasp.core.utils.BSONFormats
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.Params
import org.scalatest.Matchers._
import org.scalatest._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}

/**
 * Created by Mattia Bertorello on 30/09/15.
 */
class MlModelsSpec extends SparkFlatSpec with BeforeAndAfter {
  behavior of "machine learing models interaction"
  var mlModelBLInt: MlModelBL = null
  before {
    mlModelBLInt = new AllBLsTestWrapper().mlModelBL
  }

  it should "write and read a transformer with info" in {


  val mlModelsDB = new MlModelsDB(new {val mlModelBL: MlModelBL = mlModelBLInt})
    val transformer: Transformer with Params = MlModelsTransformerCreation.createTransfomer(sqlContext)
    val transformerWithInfoList = generateMlModelsOnlyInfo().map(TransformerWithInfo.create(_, transformer))

    mlModelsDB.write(transformerWithInfoList)


    val transformWithInfo: Future[TransformerWithInfo] = mlModelsDB.read("id1", "1")

    whenReady(transformWithInfo) { s =>

      s.transformer.explainParams() should equal(transformer.explainParams())
      s.timestamp shouldBe 2l
      s.name shouldBe "id1"
    }

    val transformWithInfo1 = mlModelsDB.read("id1", "1", Some(1l))

    whenReady(transformWithInfo1) { s =>

      s.transformer.explainParams() should equal(transformer.explainParams())
      s.timestamp shouldBe 1l
    }

  }

  it should "read from MlModelsBroadcastDB" in {

    val mlModelsDB = new MlModelsDB(new {val mlModelBL: MlModelBL = mlModelBLInt})

    val transformer: Transformer with Params = MlModelsTransformerCreation.createTransfomer(sqlContext)
    val mlModels = generateMlModelsOnlyInfo()
    val transformerWithInfoList = mlModels.map(TransformerWithInfo.create(_, transformer))

    mlModelsDB.write(transformerWithInfoList)



    val mlModelsBroadcastFuture: Future[MlModelsBroadcastDB] =
      mlModelsDB.createModelsBroadcast(mlModels)(sc = sc)

    val mlModelsBroadcast: MlModelsBroadcastDB = Await.result(mlModelsBroadcastFuture, Duration.Inf)

    val t = mlModelsBroadcast.get("id1", "1")
    t shouldBe defined

    val t1 = t.get


    t1.transformer.explainParams() should equal(transformer.explainParams())
    t1.timestamp shouldBe 2l
    t1.name shouldBe "id1"

    val transformWithInfo1 = mlModelsDB.read("id1", "1", Some(1l))

    whenReady(transformWithInfo1) { s =>

      s.transformer.explainParams() should equal(transformer.explainParams())
      s.timestamp shouldBe 1l
    }


  }

  it should "save models in MlModelsBroadcastDB and read" in {

    val mlModelsDB = new MlModelsDB(new {val mlModelBL: MlModelBL = mlModelBLInt})

    val transformer: Transformer with Params = MlModelsTransformerCreation.createTransfomer(sqlContext)
    val mlModels = generateMlModelsOnlyInfo()
    val transformerWithInfoList = mlModels.map(TransformerWithInfo.create(_, transformer))

    mlModelsDB.write(transformerWithInfoList)

    val mlModelsBroadcast: MlModelsBroadcastDB = new MlModelsBroadcastDB()
    transformerWithInfoList.foreach(f = mlModelsBroadcast.addModelToSave)

    mlModelsBroadcast.getModelsToSave shouldBe transformerWithInfoList


  }


  it should "create a model from a master batch controller" in {

    implicit val system: ActorSystem = ActorSystem()

    implicit val timeout: Timeout = Timeout(15.minutes) // implicit execution context

    val testBL = new AllBLsTestWrapper
    val transformer: Transformer with Params = MlModelsTransformerCreation.createTransfomer(sqlContext)

    val sparkWriter = new SparkWriterTestWrapper
    val actorRef = TestActorRef(new BatchJobActor(testBL, sparkWriter, sc))
    val jobFut: BatchJobModel = createBatchModel()
    val batchJobBL: BatchJobBL = testBL.batchJobBL
    batchJobBL.persist(jobFut)
    actorRef ! jobFut

    val mlModelsDB = new MlModelsDB(testBL)

    whenReady(testBL.mlModelBL.getAll)(mlModelBLInt => {
      mlModelBLInt should have size 1
      mlModelBLInt.head.name shouldBe "exampleModel"
      whenReady(mlModelsDB.read(mlModelBLInt.head))((s: TransformerWithInfo) => {
        s.transformer.explainParams() should equal(transformer.explainParams())
      })
    })
    println(sparkWriter.resultSparkWriter.toSeq)

  }

  it should "use a model from a consumer job" in {

    implicit val system: ActorSystem = ActorSystem()

    implicit val timeout: Timeout = Timeout(15.minutes) // implicit execution context

    val testBL = new AllBLsTestWrapper

    val mlModelsDB = new MlModelsDB(testBL)

    val transformer: Transformer with Params = MlModelsTransformerCreation.createTransfomer(sqlContext)
    val mlModelOnlyInfo = MlModelOnlyInfo(name = MlModelsSpecBatchModelMaker.nameModel, version = MlModelsSpecBatchModelMaker.versionModel, timestamp = Some(1l),
      modelFileId = Some(BSONObjectID.generate), className = Some("class1"))

    val transformerWithInfoList = TransformerWithInfo.create(mlModelOnlyInfo, transformer)

    mlModelsDB.write(transformerWithInfoList)


    val sparkWriter = new SparkWriterTestWrapper
    val sparkStreamingReader = new StreamingReaderTestWrapper(sc)

     val topicSchema = s"""
    {"type":"record",
    "namespace":"Test",
    "name":"Test",
    "fields":[
      ${TopicModel.schema_base},
      {"name":"label","type":"double"},
      {"name":"featuresArray","type":"array", "items": "double"}
    ]}"""

    val topicBLId = BSONObjectID.generate
    testBL.topicBL.persist(TopicModel(MlModelsSpecBatchModelMaker.nameDF, 1, BSONFormats.fromString(topicSchema).get, Some(topicBLId)))


    val etl = ETLModel(
      "write on index", List(ReaderModel(topicBLId, MlModelsSpecBatchModelMaker.nameDF, "topic")),
      WriterModel.IndexWriter(BSONObjectID.generate, "TestIndex"), List(mlModelOnlyInfo) , Some(StrategyModel("it.agilelab.bigdata.wasp.consumers.MlModelsSpecConsumerModelMaker")))

    val actorRefMaster = TestActorRef(new Actor {
      def receive = {
        case msg => println(msg)
      }
    })


    sparkStreamingReader.addRDDToStreaming(Map(
    "label" -> 1.0,
    "featuresArray" -> Seq(0.0, 1.2, -0.5)
    ))
    val actorRef = TestActorRef(new ConsumerEtlActor(testBL, sparkWriter, sparkStreamingReader, ssc, etl, actorRefMaster))



    ssc.start()
    ssc.awaitTerminationOrTimeout(5000)
    println(sparkWriter.resultSparkStreamingWriter)
    sparkWriter.resultSparkStreamingWriter should have size 1

  }


  def generateMlModelsOnlyInfo() = {
    val id1 = BSONObjectID.generate
    val id2 = BSONObjectID.generate

    val mlModelOnlyInfo = MlModelOnlyInfo(name = "id1", version = "1", timestamp = Some(1l),
      modelFileId = Some(id1), className = Some("class1"))
    val mlModelOnlyInfo1 = MlModelOnlyInfo(name = "id1", version = "1", timestamp = Some(2l),
      modelFileId = Some(id2), className = Some("class1"))

    val mlModelOnlyInfo2 = MlModelOnlyInfo(name = "id2", version = "1", timestamp = Some(4l),
      modelFileId = Some(id2), className = Some("class2"))

    val mlModelOnlyInfo3 = MlModelOnlyInfo(name = "id2", version = "2", timestamp = Some(6l),
      modelFileId = Some(id2), className = Some("class2"))

    List(mlModelOnlyInfo, mlModelOnlyInfo1, mlModelOnlyInfo2, mlModelOnlyInfo3)
  }

  def createBatchModel(): BatchJobModel = {
    val batchOutputIndex = BatchOutputIndex()
    BatchJobModel(
      name = "MlModelsSpecBatchModel",
      description = "BatchJobWithModelCreationExample example",
      owner = "system",
      system = true,
      creationTime = WaspSystem.now,
      /*etl = ETLModel("write on index", List(ReaderModel(BSONObjectID.generate, "IndexReader", "index")),
        WriterModel.IndexWriter(IndexModel.loggerIndex._id.get),
        Some(StrategyModel("it.agilelab.bigdata.wasp.consumers.strategies.DummyBatchStrategy"))),*/
      etl = ETLModel(
        "empty", List(),
        WriterModel.IndexWriter(batchOutputIndex._id.get, batchOutputIndex.name), List[MlModelOnlyInfo](), Some(StrategyModel("it.agilelab.bigdata.wasp.consumers.MlModelsSpecBatchModelMaker"))),
      state = JobStateEnum.PENDING,
      _id = Some(BSONObjectID.generate))
  }
}
