package example

import java.util.Properties
import java.util.concurrent.TimeUnit
import java.time._
import org.json4s._
import org.json4s.native.JsonMethods._

import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream._
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}

import Serdes._


case class StatusStats(isRent: Int, lastReported: Int)

object ClosedStation extends App {

    val configs: Properties = {
        val p = new Properties()
        p.put(StreamsConfig.APPLICATION_ID_CONFIG, "closed-station")
        p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
  
        p
      }


    val builder: StreamsBuilder = new StreamsBuilder

    val statusStream: KStream[String, StatusStats] = builder.stream[String, String]("status")
      .mapValues((stationId, v) => {
        val status = parse(v)
        val JInt(isRent) = status \ "is_renting"
        val JInt(lastReported) = status \ "last_reported"
        
        StatusStats(isRent.toInt, lastReported.toInt)
      })

    val closedStream: KStream[String, String] = statusStream
      .filter((station, status) => status.isRent == 0 )
      .map((stationId, status) => {
        val formatter = format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        val humanDate = if (status.lastReported == 1) "Unknown time" else formatter.format(LocalDateTime.ofInstant(Instant.ofEpochSecond(status.lastReported), ZoneId.of("America/New_York")))

        (stationId, s" StationId $stationId is temporarily closed (at $humanDate)")
      })

    closedStream.to("closed-station")

    val streams: KafkaStreams = new KafkaStreams(builder.build(), configs)

    streams.start()

    sys.ShutdownHookThread {
      streams.close(10, TimeUnit.SECONDS)
   }
}


