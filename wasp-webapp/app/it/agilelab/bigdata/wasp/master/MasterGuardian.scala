package it.agilelab.bigdata.wasp.master

import java.util.Calendar

import akka.actor.{Actor, ActorRef, PoisonPill, Props, actorRef2Scala}
import akka.pattern.ask
import com.typesafe.config.ConfigFactory
import it.agilelab.bigdata.wasp.consumers._
import it.agilelab.bigdata.wasp.consumers.consumers.{ConsumersMasterGuardian, RestartConsumers}
import it.agilelab.bigdata.wasp.consumers.readers.KafkaReader
import it.agilelab.bigdata.wasp.core.WaspSystem
import it.agilelab.bigdata.wasp.core.WaspSystem.{??, actorSystem, timeout}
import it.agilelab.bigdata.wasp.core.bl._
import it.agilelab.bigdata.wasp.core.cluster.ClusterAwareNodeGuardian
import it.agilelab.bigdata.wasp.core.logging.WaspLogger
import it.agilelab.bigdata.wasp.core.models.{BatchJobModel, PipegraphModel, ProducerModel}
import it.agilelab.bigdata.wasp.producers.InternalLogProducerGuardian

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt, MILLISECONDS}
import scala.concurrent.{Await, Future, future}

object MasterGuardian {

  if (WaspSystem.masterActor == null)
    WaspSystem.masterActor = actorSystem.actorOf(Props(new MasterGuardian(ConfigBL)))

  val timeToFirst = ceilDayTime(System.currentTimeMillis) - System.currentTimeMillis
  
  // at midnight, restart all active pipelines (this causes new timed indices creation and consumers redirection on new indices)
  val rollingTask = actorSystem.scheduler.schedule(Duration.apply(timeToFirst, MILLISECONDS), 24 hours) {
    ??[Boolean](WaspSystem.masterActor, RestartPipegraphs)
  }

  // TODO: configurable timeout & handle this better
  lazy val consumer = try {
      Await.result(actorSystem.actorSelection(ConsumersMasterGuardian.name).resolveOne(), 2 second)
    } catch {
      case e: Exception => actorSystem.actorOf(Props(new ConsumersMasterGuardian(ConfigBL, writers.SparkWriterFactoryDefault, KafkaReader)), ConsumersMasterGuardian.name)
    }
  //lazy val batchGuardian = try {
//      Await.result(actorSystem.actorSelection(batch.BatchMasterGuardian.name).resolveOne(), 2 second)
//    } catch {
//      case e: Exception =>actorSystem.actorOf(Props(new batch.BatchMasterGuardian(ConfigBL, None, writers.SparkWriterFactoryDefault)), batch.BatchMasterGuardian.name)
//    }

  private def ceilDayTime(time: Long): Long = {

    val cal = Calendar.getInstance()
    cal.setTimeInMillis(time)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.add(Calendar.DATE, 1)
    cal.getTime.getTime
  }
}

class MasterGuardian(env: {val producerBL: ProducerBL; val pipegraphBL: PipegraphBL; val batchJobBL: BatchJobBL; val batchSchedulerBL: BatchSchedulersBL; }, val classLoader: Option[ClassLoader] = None) extends ClusterAwareNodeGuardian {

  import MasterGuardian._

  lazy val logger = WaspLogger(this.getClass.getName)

  lazy val batchGuardian = try {
    println("************** try actorSelection")
    Await.result(actorSystem.actorSelection(batch.BatchMasterGuardian.name).resolveOne(), 2 second)
  } catch {
    case e: Exception => println("**************** new BatchMasterGuardian"); actorSystem.actorOf(Props(new batch.BatchMasterGuardian(ConfigBL, classLoader, writers.SparkWriterFactoryDefault)), batch.BatchMasterGuardian.name);
  }

  // TODO just for Class Loader debug.
  // logger.error("Framework ClassLoader"+this.getClass.getClassLoader.toString())

  val producers: Map[String, ActorRef] = Await.result(
  env.producerBL.getAll.map((producers: List[ProducerModel]) => {
    producers.map(producer => {
      if(producer.name == "LoggerProducer")
        producer._id.get.stringify -> WaspSystem.loggerActor.get
      else
        producer._id.get.stringify -> actorSystem.actorOf(Props( classLoader.map(cl => cl.loadClass(producer.className)).getOrElse(Class.forName(producer.className)), ConfigBL), producer.name)
    }).toMap
  }), WaspSystem.timeout.duration)

  // on startup not-system pipegraphs and associated consumers are deactivated
  val result1 = Await.result(env.pipegraphBL.getSystemPipegraphs(false).flatMap {
    case Nil => future {
      List(false)
    }
    case pipegraphs => setPipegraphsActive(pipegraphs, isActive = false)
  }, 20 seconds)

  val result2 = Await.result(env.producerBL.getActiveProducers().flatMap {
    case Nil => future {
      List(false)
    }
    case caseProducers => setProducersActive(caseProducers, isActive = false)
  }, 20 seconds)

  private def setPipegraphsActive(pipegraphs: List[PipegraphModel], isActive: Boolean): Future[List[Boolean]] = {
    val list: List[Future[Boolean]] = pipegraphs.map(pipegraph => env.pipegraphBL.setIsActive(pipegraph, isActive).map(_.ok))
    val matrix: Future[List[Boolean]] = Future.sequence(list)
    matrix
  }

  private def setProducersActive(producers: List[ProducerModel], isActive: Boolean): Future[List[Boolean]] = {
    val list: List[Future[Boolean]] = producers.map(producer => env.producerBL.setIsActive(producer, isActive).map(_.ok))
    val matrix: Future[List[Boolean]] = Future.sequence(list)
    matrix
  }

  logger.info("initial pipegraphs and producers setup: " + result1 + " - " + result2)

  // TODO manage error in pipegraph initialization
  // auto-start raw pipegraph
  val conf = ConfigFactory.load
  val systemPipegraphsStart = conf.getString("systempipegraphs.start")
  if (systemPipegraphsStart == "true") {
    env.pipegraphBL.getByName("RawPipegraph").map {
      case None => logger.error("RawPipegraph not found")
      case Some(pipegraph) => self.actorRef ! StartPipegraph(pipegraph._id.get.stringify)
    }

    // auto-start logger pipegraph
    env.pipegraphBL.getByName("LoggerPipegraph").map {
      case None => logger.error("LoggerPipegraph not found")
      case Some(pipegraph) => self.actorRef ! StartPipegraph(pipegraph._id.get.stringify)
    }
  } else {
    env.pipegraphBL.getSystemPipegraphs().map(_ foreach (p => env.pipegraphBL.setIsActive(p, isActive = false)))
  }

  logger.info("Batch schedulers initializing ...")
  batchGuardian ! batch.StartSchedulersMessage()

  // TODO try without sender parenthesis
  def initialized: Actor.Receive = {
    //case message: RemovePipegraph => call(message, onPipegraph(message.id, removePipegraph))
    case message: StartPipegraph    => call(sender(), message, onPipegraph(message.id, startPipegraph))
    case message: StopPipegraph     => call(sender(), message, onPipegraph(message.id, stopPipegraph))
    case RestartPipegraphs          => call(sender(), RestartPipegraphs, onRestartPipegraphs())
    case message: StartProducer     => call(sender(), message, onProducer(message.id, startProducer))
    case message: StopProducer      => call(sender(), message, onProducer(message.id, stopProducer))
    case message: StartETL          => call(sender(), message, onEtl(message.id, message.etlName, startEtl))
    case message: StopETL           => call(sender(), message, onEtl(message.id, message.etlName, stopEtl))
    case message: StartBatchJob     => call(sender(), message, onBatchJob(message.id, startBatchJob))
    case message: StartPendingBatchJobs => call(sender(), message, startPendingBatchJobs())
    case message: batch.BatchJobProcessedMessage => //TODO gestione batchJob finito?
    //case message: Any           => logger.error("unknown message: " + message)
  }

  private def call[T <: MasterGuardianMessage](sender: ActorRef, message: T, future: Future[Either[String, String]]) = {
    future.map(result => {
      logger.info(message + ": " + result)
      sender ! result
    })
  }

  private def onPipegraph(id: String, f: PipegraphModel => Future[Either[String, String]]) =
    env.pipegraphBL.getById(id).flatMap {
      case None => future {
        Right("Pipegraph not retrieved")
      }
      case Some(pipegraph) => f(pipegraph)
    }.recover {
      case e: Throwable => Right(manageThrowable("Pipegraph not started.", e))
    }
  
  private def onRestartPipegraphs(): Future[Either[String, String]] = {
    consumer ! RestartConsumers
    future(Left("Pipegraphs restart started."))
  }
  
  private def onProducer(id: String, f: ProducerModel => Future[Either[String, String]]) =
    env.producerBL.getById(id).flatMap {
      case None => future {
        Right("Producer not retrieved")
      }
      case Some(producer) => f(producer)
    }.recover {
      case e: Throwable => Right(manageThrowable("Producer not started.", e))
    }


  private def onEtl(idPipegraph: String, etlName: String, f: (PipegraphModel, String) => Future[Either[String, String]]) =
    env.pipegraphBL.getById(idPipegraph).flatMap {
      case None => future {
        Right("ETL not retrieved")
      }
      case Some(pipegraph) => f(pipegraph, etlName)
    }.recover {
      case e: Throwable => Right(manageThrowable("ETL not started.", e))
    }

  private def onBatchJob(id: String, f: BatchJobModel => Future[Either[String, String]]) =
    env.batchJobBL.getById(id).flatMap {
      case None => future {
        Right("BatchJob not retrieved")
      }
      case Some(batchJob) => f(batchJob)
    }.recover {
      case e: Throwable => Right(manageThrowable("BatchJob not started.", e))
    }

  private def manageThrowable(message: String, throwable: Throwable): String = {
    var result = message
    logger.error(message, throwable)

    if (throwable.getMessage != null && !throwable.getMessage.isEmpty)
      result = result + " Cause: " + throwable.getMessage

    result
  }

  private def cleanPipegraph(message: RemovePipegraph) = {
    //sender ! true
  }

  private def startPipegraph(pipegraph: PipegraphModel) = {
    // persist pipegraph as active
    setActiveAndRestart(pipegraph, active = true)
  }

  private def stopPipegraph(pipegraph: PipegraphModel): Future[Either[String, String]] = {
    setActiveAndRestart(pipegraph, active = false)
  }

  private def setActiveAndRestart(pipegraph: PipegraphModel, active: Boolean): Future[Either[String, String]] = {
    env.pipegraphBL.setIsActive(pipegraph, isActive = active).flatMap(result => {
      if (!result.ok)
        future { Right("Pipegraph '" + pipegraph.name + "' not "+ (if(active) "activated" else "disactivated")) }
      else{
        val res = ??[Boolean](consumer, consumers.RestartConsumers, Some(timeout.duration))

        res match {
          case true => future { Left("Pipegraph '" + pipegraph.name + "' " + (if(active) "started" else "stopped"))}
          case false => env.pipegraphBL.setIsActive(pipegraph, isActive = !active).map(res => Right("Pipegraph '" + pipegraph.name + "' not "+ (if(active) "started" else "stopped")))
        }
      }
    })
  }

  //TODO  implementare questa parte
  private def startEtl(pipegraph: PipegraphModel, etlName: String): Future[Either[String, String]] = {
    future { Right("ETL '" + etlName + "' not started") }
  }

  //TODO  implementare questa parte
  private def stopEtl(pipegraph: PipegraphModel, etlName: String): Future[Either[String, String]] = {
   future { Left("ETL '" + etlName + "' stopped") }
  }

  private def startProducer(producer: ProducerModel): Future[Either[String, String]] = {
    // initialise producer actor if not already present
    if (producers.isDefinedAt(producer._id.get.stringify)) {
      if (! ??[Boolean](producers(producer._id.get.stringify), it.agilelab.bigdata.wasp.producers.StartProducer))
        future {
          Right(s"Producer '${producer.name}' not started")
        }
      else
        future {
          Left(s"Producer '${producer.name}' started")
        }

    } else {
      future {
        Right(s"Producer '${producer.name}' not exists")
      }
    }

  }

  private def stopProducer(producer: ProducerModel): Future[Either[String, String]] = {
    if (!producers.isDefinedAt(producer._id.get.stringify))
      future { Right("Producer '" + producer.name + "' not initializated") }
    else if (! ??[Boolean](producers(producer._id.get.stringify), it.agilelab.bigdata.wasp.producers.StopProducer))
      future { Right("Producer '" + producer.name + "' not stopped") }
    else
      future { Left("Producer '" + producer.name + "' stopped") }
  }

  private def startBatchJob(batchJob: BatchJobModel): Future[Either[String, String]] = {
    logger.info(s"Send the message StartBatchJobMessage to batchGuardian: job to start: ${batchJob._id.get.stringify}")

    val jobFut = batchGuardian ? batch.StartBatchJobMessage(batchJob._id.get.stringify)
    val jobRes = Await.result(jobFut, WaspSystem.timeout.duration).asInstanceOf[batch.BatchJobResult]
    if (jobRes.result) {
      future {
        Left("Batch job '" + batchJob.name + "' queued or processing")
      }
    } else {
      future {
        Right("Batch job '" + batchJob.name + "' not processing")
      }
    }
  }

  private def startPendingBatchJobs(): Future[Either[String, String]] = {
    //TODO: delete log
    logger.info("Sending CheckJobsBucketMessage to Batch Guardian.")
    batchGuardian ! batch.CheckJobsBucketMessage()
    future(Left("Batch jobs checking started"))
  }

  override def postStop() {
    WaspSystem.elasticAdminActor ! PoisonPill
    WaspSystem.getKafkaAdminActor ! PoisonPill
    consumer ! PoisonPill
    batchGuardian ! PoisonPill

    super.postStop()
  }
}
