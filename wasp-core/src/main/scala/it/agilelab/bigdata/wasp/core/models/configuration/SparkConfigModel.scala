package it.agilelab.bigdata.wasp.core.models.configuration

import it.agilelab.bigdata.wasp.core.utils.ConnectionConfig

abstract class SparkConfigModel(val master: ConnectionConfig,
                                val cleanerTtl: Int,
                                val executorMemory: Option[String],
                                val additionalJars: Option[Seq[String]],
                                val driverHostname: String,
                                val yarnJar: Option[String],
                                val blockManagerPort: Option[Int],
                                val broadcastPort: Option[Int],
                                val driverPort: Option[Int],
                                val fileserverPort: Option[Int],
                                val name: String)

case class SparkStreamingConfigModel(
    override val master: ConnectionConfig,
    override val cleanerTtl: Int,
    override val executorMemory: Option[String],
    override val additionalJars: Option[Seq[String]],
    override val driverHostname: String,
    override val yarnJar: Option[String],
    override val blockManagerPort: Option[Int],
    override val broadcastPort: Option[Int],
    override val driverPort: Option[Int],
    override val fileserverPort: Option[Int],
    streamingBatchIntervalMs: Int,
    sparkCheckpointDir: String,
    override val name: String)
  extends SparkConfigModel(
    master,
    cleanerTtl,
    executorMemory,
    additionalJars,
    driverHostname,
    yarnJar,
    blockManagerPort,
    broadcastPort,
    driverPort,
    fileserverPort,
    name)

case class SparkBatchConfigModel(
    override val master: ConnectionConfig,
    override val cleanerTtl: Int,
    override val executorMemory: Option[String],
    override val additionalJars: Option[Seq[String]],
    override val driverHostname: String,
    override val yarnJar: Option[String],
    override val blockManagerPort: Option[Int],
    override val broadcastPort: Option[Int],
    override val driverPort: Option[Int],
    override val fileserverPort: Option[Int],
    override val name: String)
  extends SparkConfigModel(
    master,
    cleanerTtl,
    executorMemory,
    additionalJars,
    driverHostname,
    yarnJar,
    blockManagerPort,
    broadcastPort,
    driverPort,
    fileserverPort,
    name)
