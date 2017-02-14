package it.agilelab.bigdata.wasp.consumers

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.util.Timeout
import it.agilelab.bigdata.wasp.consumers.batch.BatchJobActor
import it.agilelab.bigdata.wasp.consumers.utils.SparkWriterTestWrapper
import it.agilelab.bigdata.wasp.core.WaspSystem
import it.agilelab.bigdata.wasp.core.bl.{AllBLsTestWrapper, BatchJobBL}
import it.agilelab.bigdata.wasp.core.models._
import it.agilelab.bigdata.wasp.core.utils.BSONFormats
import org.scalatest._
import org.scalatest.Matchers._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}

/**
 * Created by vitoressa on 06/10/15.
 */
class BatchJobsSpec extends SparkFlatSpec with BeforeAndAfter {
  behavior of "batches lifecycle and management"
  var batchJobBLInt: BatchJobBL = null
  before {
    batchJobBLInt = new AllBLsTestWrapper().batchJobBL
  }

  it should "execute the only the specified job" in {

    implicit val system: ActorSystem = ActorSystem()

    implicit val timeout: Timeout = Timeout(15.minutes) // implicit execution context

    val testBL = new AllBLsTestWrapper

    val jobsNumber = 3

    val sparkWriter = new SparkWriterTestWrapper
    val actorRef = TestActorRef(new BatchJobActor(testBL, None, sparkWriter, sc))
    val job: BatchJobModel = createJob()

    val otherJobs = new Array[BatchJobModel](2)
    otherJobs(0) = createJob()
    otherJobs(1) = createJob()

    val batchJobBL: BatchJobBL = testBL.batchJobBL

    batchJobBL.persist(job)

    for(otherJob <- otherJobs)
    {
      batchJobBL.persist(otherJob)
    }

    actorRef ! job

    Thread.sleep(2000L)

    val jobsFuture: Future[List[BatchJobModel]] = batchJobBL.getAll

    val resultingJobs = Await.result(jobsFuture, Duration.Inf)

    resultingJobs should not be empty
    resultingJobs.size shouldBe jobsNumber

    resultingJobs.foreach( resJob => {
      if (resJob._id == job._id ){
        resJob.state shouldBe JobStateEnum.SUCCESSFUL
      } else {
        resJob.state shouldBe JobStateEnum.PENDING
      }
    })


  }

  def createJob(): BatchJobModel = {
    val batchOutputIndex = BatchOutputIndex()
    BatchJobModel(
        name = "MlModelsSpecBatchModel",
        description = "BatchSpec for tests",
        owner = "system",
        system = true,
        creationTime = WaspSystem.now,
        /*etl = ETLModel("write on index", List(ReaderModel(BSONObjectID.generate, "IndexReader", "index")),
        WriterModel.IndexWriter(IndexModel.loggerIndex._id.get),
        Some(StrategyModel("it.agilelab.bigdata.wasp.consumers.strategies.DummyBatchStrategy"))),*/
      etl = ETLModel(
          "empty", List(),
          WriterModel.IndexWriter(batchOutputIndex._id.get, batchOutputIndex.name), List(), None, kafkaAccessType = ETLModel.KAFKA_ACCESS_TYPE_RECEIVED_BASED),
        state = JobStateEnum.PENDING,
        _id = Some(BSONObjectID.generate))
  }


}
