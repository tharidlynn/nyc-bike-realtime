package example

import java.util.Properties
import java.util.concurrent.TimeUnit

import org.apache.kafka.streams.kstream.TimeWindows
import org.json4s._
import org.json4s.native.JsonMethods._
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream._
import org.apache.kafka.streams.kstream.Windowed
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.streams.{KafkaStreams, KeyValue, StreamsConfig}

import Serdes._

object TurnoverRatio {

  def main(args: Array[String]): Unit = {


    val configs: Properties = {
      val p = new Properties()
      p.put(StreamsConfig.APPLICATION_ID_CONFIG, "turn-over")
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


    val netDeltaTable: KTable[Windowed[String], String] = statusStream
      .groupByKey

      .windowedBy(TimeWindows.of(TimeUnit.MINUTES.toMillis(60)))

      .aggregate("0,0")((stationId, record, accum) => {
          val newValue = record.toInt
          val netDelta = accum.split(",")(0).toInt
          val lastValue = accum.split(",")(1).toInt

          val delta = Math.abs(newValue - lastValue)
          
          (netDelta + delta) + "," + newValue

    })

    val ratio: KStream[String, String] = netDeltaTable
      .toStream
      .map((windowedKey, v) => (windowedKey.key(), v))
      .join(stationInfoTable)((delta, info) => {
          val infoParse = parse(info)
          val JInt(capacity) = infoParse \ "capacity"

          delta + "," + capacity
      })

      .map((k, v) => {
        val delta: Int = v.split(",")(0).toInt
        val capacity: Int = v.split(",")(2).toInt

        if (capacity == 0) {
           (k, "NA")
        }
        val turnOver: Double = (delta / capacity.toDouble) * 100.0

        (k, f"$turnOver%.2f")
      })

    ratio.map((k, v) => {
      (k,s"station_id: $k, turnover: $v %")
    }).to("turn-over")


    val streams: KafkaStreams = new KafkaStreams(builder.build(), configs)
    streams.start()

    sys.ShutdownHookThread {
      streams.close(10, TimeUnit.SECONDS)
    }

  }

}
