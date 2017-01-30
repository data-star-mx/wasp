package it.agilelab.bigdata.wasp.consumers

import it.agilelab.bigdata.wasp.consumers.writers._
import it.agilelab.bigdata.wasp.core.models.KeyValueModel
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import org.scalatest.Matchers._
import org.scalatest.{BeforeAndAfter, _}
import org.scalatest.concurrent.ScalaFutures

/**
  * Created by mattiabertorello on 27/01/17.
  */
case class DataTestClass(d1s: String, d2ts: Long)

class HbaseWritesSpec  extends FlatSpec  with ScalaFutures with BeforeAndAfter with  SparkFlatSpec  {

    behavior of "HBase writer"

  it should "convert the json schema" in {

    val r: HbaseTableModel = HBaseWriter.getHbaseConfDataConvert(
      """
        |{
        |  "table": {
        |    "namespace": "default",
        |    "name": "htable"
        |  },
        |  "rowkey": [
        |    {
        |      "col": "jsonCol2",
        |      "type": "string"
        |    }
        |  ],
        |  "columns": {
        |    "cf1": [
        |      {
        |        "HBcol1": {
        |          "col": "jsonCol3",
        |          "type": "string",
        |          "mappingType": "oneToOne"
        |        }
        |      }
        |     ]
        |  }
        |}
      """.stripMargin)

    r.rowKey should be(Seq(RowKeyInfo("jsonCol2", "string")))
    r.table should be(TableNameC("default", "htable"))
    r.columns should be(Map("cf1"-> List(Map("HBcol1" -> InfoCol(Some("jsonCol3"), Some("string"), "oneToOne", None, None)))))
  }

  it should "write on hbase" in {

  }

}
