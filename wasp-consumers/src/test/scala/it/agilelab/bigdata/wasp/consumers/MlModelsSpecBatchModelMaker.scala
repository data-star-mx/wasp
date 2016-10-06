package it.agilelab.bigdata.wasp.consumers

import it.agilelab.bigdata.wasp.consumers.MlModels.TransformerWithInfo
import it.agilelab.bigdata.wasp.consumers.strategies.{ReaderKey, Strategy}
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.param.Params
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.sql.{SQLContext, DataFrame}

/**
 * Created by Mattia Bertorello on 05/10/15.
 */
case class MlModelsSpecBatchModelMaker() extends Strategy {
  /**
   *
   * @param dataFrames
   * @return
   */
  override def transform(dataFrames: Map[ReaderKey, DataFrame]): DataFrame = {
    assert(sparkContext.isDefined)
    val sqlContext = new SQLContext(sparkContext = sparkContext.get)
    val model1 = MlModelsTransformerCreation.createTransfomer(sqlContext)


    mlModelsBroadcast.addModelToSave(TransformerWithInfo(name = "exampleModel", version = "1.0", transformer = model1))

    sqlContext.emptyDataFrame
  }
}

object MlModelsSpecBatchModelMaker {
  val nameDF: String = "mlDF1"
  val nameModel: String = "mlModelName1"
  val versionModel: String = "mlModelVersion1"
}


object MlModelsTransformerCreation {
  def createTransfomer(sqlContext: SQLContext): Transformer with Params = {
    // Prepare training data from a list of (label, features) tuples.
    val training = sqlContext.createDataFrame(Seq(
      (1.0, Vectors.dense(0.0, 1.1, 0.1)),
      (0.0, Vectors.dense(2.0, 1.0, -1.0)),
      (0.0, Vectors.dense(2.0, 1.3, 1.0)),
      (1.0, Vectors.dense(0.0, 1.2, -0.5))
    )).toDF("label", "features")

    // Create a LogisticRegression instance.  This instance is an Estimator.
    val lr = new LogisticRegression()
    // Print out the parameters, documentation, and any default values.
    //println("LogisticRegression parameters:\n" + lr.explainParams() + "\n")

    // We may set parameters using setter methods.
    lr.setMaxIter(10)
      .setRegParam(0.01)

    // Learn a LogisticRegression model.  This uses the parameters stored in lr.
    lr.fit(training)
  }
}

case class MlModelsSpecConsumerModelMaker() extends Strategy {

  /**
   *
   * @param dataFrames
   * @return
   */
  override def transform(dataFrames: Map[ReaderKey, DataFrame]): DataFrame = {
    assert(sparkContext.isEmpty)
    import org.apache.spark.sql.functions._
    import org.apache.spark.mllib.linalg.{Vector, Vectors}

    val key = ReaderKey("topic", MlModelsSpecBatchModelMaker.nameDF)
    val dataFrameToTransform = dataFrames.get(key).get

    val transform = mlModelsBroadcast.get(MlModelsSpecBatchModelMaker.nameModel, MlModelsSpecBatchModelMaker.versionModel)
    assert(transform.isDefined)

    val toVector = udf[Vector, Seq[Double]](a => Vectors.dense(a.toArray))
    val t = dataFrameToTransform.col("featuresArray")
    val f = toVector(t)


    val result = transform.get.transformer.transform(dataFrameToTransform.withColumn("features", f))
    result
  }
}
