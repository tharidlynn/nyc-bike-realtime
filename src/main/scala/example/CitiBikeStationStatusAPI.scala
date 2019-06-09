 /* 
  station_id: String, num_bikes_available: Int, num_ebikes_available: Int,
  num_bikes_disabled: Int, num_docks_available: Int, num_docks_disabled: Int, is_installed: Int,
  is_renting: Int, is_returning: Int, last_reported: Int, eightd_has_available_keys: Boolean
  */

package example

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import scalaj.http._
import org.json4s._
import org.json4s.native.JsonMethods._
import java.util.Properties

object CitiBikeStationStatusAPI extends App {

  val kafkaTopic = "status"
  val configs = new Properties()
  configs.put("bootstrap.servers", "localhost:9092")
  configs.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  configs.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  val producer = new KafkaProducer[String, String](configs)

  while(true) {

    try {
      val json = Http("https://gbfs.citibikenyc.com/gbfs/en/station_status.json").asString.body
      val stationStatus = parse(json)
  
      val JInt(lastUpdated) = stationStatus \ "last_updated"
      val JInt(ttl) = stationStatus \ "ttl"
      val JArray(data) = stationStatus \ "data" \ "stations"

      for (station <- data) {

        val stationId: String = compact(render(station \ "station_id"))
        val stationData: String = compact(render(station))
  
        val record = new ProducerRecord(kafkaTopic, stationId, stationData)
        producer.send(record)
        println(record)
      }
  
      Thread.sleep(12000)
  
    }

    catch {
      case _: Throwable => 

      Thread.sleep(12000)
    }
   
  }
  
  producer.flush()
  producer.close()

}
