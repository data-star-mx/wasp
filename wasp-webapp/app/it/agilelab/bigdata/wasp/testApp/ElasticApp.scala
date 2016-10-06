package it.agilelab.bigdata.wasp.testApp

import it.agilelab.bigdata.wasp.core.WaspSystem
import it.agilelab.bigdata.wasp.core.elastic.ElasticAdminActor
import it.agilelab.bigdata.wasp.core.logging.WaspLogger
import it.agilelab.bigdata.wasp.core.utils.ElasticConfiguration
import it.agilelab.bigdata.wasp.web.Global
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.FilterBuilders._
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders._
import play.api.libs.json.Json


object ElasticApp extends App with WaspSystem with ElasticConfiguration{

    def loggerActorProps = Global.loggerActorProps
    val logger = WaspLogger("ElasticApp")

    val settings = ImmutableSettings.settingsBuilder().put("cluster.name", elasticConfig.cluster_name).build();

    val transportClient = new TransportClient(settings)

    for (connection <- elasticConfig.connections) {
      val address = new InetSocketTransportAddress(connection.host, ElasticAdminActor.port)
      transportClient.addTransportAddress(address)
    }

  println("connected")



  val filter = termFilter(
    "SECURITYID",
    "DE0008404005"
  );

  var res = transportClient.prepareSearch("orderpipeline_index")
                  //.setSearchType(SearchType.)
                  .setQuery(QueryBuilders.boolQuery().must(matchQuery(
                                                            "SECURITYID",
                                                            "DE0008404005")
                  ).must(rangeQuery("LASTPX")))
                  //.setPostFilter(filter)
                  .setExplain(true)
                  .execute().actionGet()
  println(res.toString)
  for(hit <- res.getHits.hits()){
    val json = Json.parse(hit.source)
    //println((json \ "SECURITYID").as[String])
    println(json)
  }
  //println(res)

}