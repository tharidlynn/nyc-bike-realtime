package example

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import scalaj.http._
import org.json4s._
import org.json4s.native.JsonMethods._

import java.util.Properties

object CitiBikeStationInfoAPI extends App {

  val kafkaTopic = "info"

  val configs = new Properties()
  configs.put("bootstrap.servers", "localhost:9092")
  configs.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  configs.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  val producer = new KafkaProducer[String, String](configs)

  val json = Http("https://gbfs.citibikenyc.com/gbfs/en/station_information.json").asString.body
  val stationInfo = parse(json)

  val JInt(lastUpdated) = stationInfo \ "last_updated"
  val JInt(ttl) = stationInfo \ "ttl"
  val JArray(data) = stationInfo \ "data" \ "stations"

  for (station <- data) {
    // println(station \ "station_id" + "\n\n")
    val stationId: String = compact(render(station \ "station_id"))
//    println(compact(render(station)))
    val stationData: String = compact(render(station))

    //send kafka produce
    val record = new ProducerRecord(kafkaTopic, stationId, stationData)
    producer.send(record)
    
  } 
  
  producer.flush()
  producer.close()

  
  
}
