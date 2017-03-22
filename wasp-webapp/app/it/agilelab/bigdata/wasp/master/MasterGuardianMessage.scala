package it.agilelab.bigdata.wasp.master

import it.agilelab.bigdata.wasp.core.WaspMessage

abstract class MasterGuardianMessage(val id:String) extends WaspMessage {}

abstract class PipegraphMessage(id:String) extends MasterGuardianMessage(id)
abstract class ProducerMessage(id:String) extends MasterGuardianMessage(id)
abstract class ETLMessage(id:String, val etlName:String) extends MasterGuardianMessage(id)
abstract class BatchJobMessage(id:String) extends MasterGuardianMessage(id)

case class RemovePipegraph(override val id:String) extends PipegraphMessage(id)
case class StartPipegraph(override val id : String) extends PipegraphMessage(id)
case class StopPipegraph(override val id:String) extends PipegraphMessage(id)
case object RestartPipegraphs extends MasterGuardianMessage(null)
case class StartProducer(override val id:String) extends ProducerMessage(id)
case class StopProducer(override val id:String) extends ProducerMessage(id)
case class StartETL(override val id:String, override val etlName: String) extends ETLMessage(id, etlName)
case class StopETL(override val id:String, override val etlName: String) extends ETLMessage(id, etlName)

case class StartBatchJob(override val id:String) extends BatchJobMessage(id)
case class StartPendingBatchJobs(override val id: String) extends MasterGuardianMessage(id)