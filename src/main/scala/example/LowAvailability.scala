package example

import java.util.Properties
import java.util.concurrent.TimeUnit
import org.json4s._
import org.json4s.native.JsonMethods._

import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream._
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}

case class BikeStats(numBikesAvailable: Int, stationCapacity: Int,
                         availabilityRatio: Double, latitude: Double, longitude: Double)

object LowAvailability {

  def main(args: Array[String]): Unit = {
    import Serdes._

    val configs: Properties = {
      val p = new Properties()
      p.put(StreamsConfig.APPLICATION_ID_CONFIG, "low-availability")
      p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
      p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

      p
    }

    val builder: StreamsBuilder = new StreamsBuilder

    val statusStream: KStream[String, Int] = builder.stream[String, String]("status")
      .map((stationId, v) => {
        val status = parse(v)
        val JInt(numBikes) = status \ "num_bikes_available"

        (stationId, numBikes.toInt)
      })

    val stationInfoTable: KTable[String, String] = builder.table[String, String]("info")

  
    val outputStream: KStream[String, String] = statusStream
      .leftJoin(stationInfoTable)((numBikes, info) => {
      val infoParse = parse(info)
      val JInt(capacity) = infoParse \ "capacity"
      val JDouble(latitude) = infoParse \ "lat"
      val JDouble(longitude) = infoParse \ "lon"

      val availabilityRatio = if (capacity == null) 1 else numBikes / capacity.toDouble

      BikeStats(numBikes.toInt,
        capacity.toInt,
        availabilityRatio,
        latitude,
        longitude)
    })

      .filter((k, stats) => {
        stats.availabilityRatio < 0.1
      })

      .map((k, stats) => {
        (k, s"station_id: $k, " +
          s"longitude: ${stats.longitude}, " +
          s"latitude: ${stats.latitude}, " +
          s"bikes: ${stats.numBikesAvailable}, capacity: ${stats.stationCapacity}," +
          f"ratio: ${stats.availabilityRatio * 100}%.2f %%")
      })

    outputStream.to("low-ava")


    val streams: KafkaStreams = new KafkaStreams(builder.build(), configs)


    streams.start()

    sys.ShutdownHookThread {
      streams.close(10, TimeUnit.SECONDS)
    }
  }

}
