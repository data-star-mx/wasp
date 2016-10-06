package it.agilelab.bigdata.wasp.consumers

import it.agilelab.bigdata.wasp.core.WaspSystem
import it.agilelab.bigdata.wasp.core.models.IndexModel
import it.agilelab.bigdata.wasp.core.utils.BSONFormats
import reactivemongo.bson.BSONObjectID

/**
	* Created by m1lt0n on 28/07/16.
	*/
//TODO: Index di prova per batch, rimuovere
private[consumers] object BatchOutputIndex {

  val index_name = "BatchOutput"

  def apply() = IndexModel(
    name = IndexModel.normalizeName(index_name),
    creationTime = WaspSystem.now,
    schema = BSONFormats.fromString(indexSchema),
    _id = Some(BSONObjectID.generate)
  )

  private val indexSchema = s"""
      {"yahoofinance":
          {"properties":{
            ${IndexModel.schema_base_elastic},
        "stock_name":{"type":"string","index":"not_analyzed","store":"true","enabled":"true"},
        "bid":{"type":"double","index":"not_analyzed","store":"true","enabled":"true"},
        "ask":{"type":"double","index":"not_analyzed","store":"true","enabled":"true"},
        "percent_change":{"type":"double","index":"not_analyzed","store":"true","enabled":"true"},
        "volume":{"type":"double","index":"not_analyzed","store":"true","enabled":"true"},
        "currency":{"type":"string","index":"not_analyzed","store":"true","enabled":"true"}
          }}}"""
}
