package it.agilelab.bigdata.wasp.core.utils

/**
  * Created by mattiabertorello on 27/01/17.
  */



import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.util
import java.util.HashMap

import org.apache.avro.Schema
import org.apache.avro.file.DataFileWriter
import org.apache.avro.generic.GenericData.Record
import org.apache.avro.generic.{GenericDatumWriter, GenericRecord}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._

import scala.collection.immutable.Map

class RowToAvro(schema: StructType,
                schemaAvro: Schema)   {

  private lazy val converter = createConverterToAvro(schema, schemaAvro)

  def write(row: Row): Array[Byte] = {
    val output = new ByteArrayOutputStream()
    val value = converter(row).asInstanceOf[GenericRecord]
    var writer = new DataFileWriter(new GenericDatumWriter[GenericRecord]())
    writer.append(value)
    writer.create(schemaAvro, output)
    writer.flush()
    output.toByteArray

  }


  /**
    * This function constructs converter function for a given sparkSQL datatype. This is used in
    * writing Avro records out to disk
    */
  private def createConverterToAvro(
                                     dataType: DataType,
                                     schema: Schema): (Any) => Any = {
    dataType match {
      case BinaryType => (item: Any) => item match {
        case null => null
        case bytes: Array[Byte] => ByteBuffer.wrap(bytes)
      }
      case ByteType | ShortType | IntegerType | LongType |
           FloatType | DoubleType | StringType | BooleanType => identity
      case _: DecimalType => (item: Any) => if (item == null) null else item.toString
      case TimestampType => (item: Any) =>
        if (item == null) null else item.asInstanceOf[Timestamp].getTime
      case ArrayType(elementType, _) =>
        val elementConverter = createConverterToAvro(elementType, schema)
        (item: Any) => {
          if (item == null) {
            null
          } else {
            val sourceArray = item.asInstanceOf[Seq[Any]]
            val sourceArraySize = sourceArray.size
            val targetArray = new Array[Any](sourceArraySize)
            var idx = 0
            while (idx < sourceArraySize) {
              targetArray(idx) = elementConverter(sourceArray(idx))
              idx += 1
            }
            targetArray
          }
        }
      case MapType(StringType, valueType, _) =>
        val valueConverter = createConverterToAvro(valueType, schema)
        (item: Any) => {
          if (item == null) {
            null
          } else {
            val javaMap = new util.HashMap[String, Any]()
            item.asInstanceOf[Map[String, Any]].foreach { case (key, value) =>
              javaMap.put(key, valueConverter(value))
            }
            javaMap
          }
        }
      case structType: StructType =>
        val fieldConverters = structType.fields.map(field =>
          createConverterToAvro(field.dataType, schema))
        (item: Any) => {
          if (item == null) {
            null
          } else {
            val record = new Record(schema)
            val convertersIterator = fieldConverters.iterator
            val fieldNamesIterator = dataType.asInstanceOf[StructType].fieldNames.iterator
            val rowIterator = item.asInstanceOf[Row].toSeq.iterator

            while (convertersIterator.hasNext) {
              val converter = convertersIterator.next()
              record.put(fieldNamesIterator.next(), converter(rowIterator.next()))
            }
            record
          }
        }
    }
  }
}