# NYC-BIKE Real Time
This project was an attempt to rewrite [Tracking NYC Citi Bike real time utilization using Kafka Streams](https://towardsdatascience.com/tracking-nyc-citi-bike-real-time-utilization-using-kafka-streams-1c0ea9e24e79) in Scala used for educational purposes only.

The original post has only 2 metrics: "Low availability" and "Turn over ratio". So I decide to add another metric called "Closed station" that filters the closed station and display to the consumers.

_Note: the project was bootstraped with `sbt new scala/scala-seed.g8`_


## Getting Started

1.Start Zookeeper and Kafka server with command

```
$ bin/zookeeper-server-start.sh config/zookeeper.properties

$ bin/kafka-server-start.sh config/server.properties
```
2.Create 5 Kafka topics:
```
$ bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic info

$ bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic status

$ bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic low-ava

$ bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic closed-station

$ bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic turn-over
```

3.Verify all topics were created

`bin/kafka-topics.sh --list --bootstrap-server localhost:9092`

4.Build fat Jar File with `sbt assembly`

[Optional] `sbt run`  is sufficient in the local development.

5.Start Both Producers

```
$ java -cp target/scala-2.12/nyc-bike-assembly-0.1.0-SNAPSHOT.jar example.CitiBikeStationInfoAPI

$ java -cp target/scala-2.12/nyc-bike-assembly-0.1.0-SNAPSHOT.jar example.CitiBikeStationStatusAPI
```

6.Start All Kafka stream

```
$ java -cp target/scala-2.12/nyc-bike-assembly-0.1.0-SNAPSHOT.jar example.ClosedStation

$ java -cp target/scala-2.12/nyc-bike-assembly-0.1.0-SNAPSHOT.jar example.LowAvailability

$ java -cp target/scala-2.12/nyc-bike-assembly-0.1.0-SNAPSHOT.jar example.TurnoverRatio
```
7.Start consumer to see the results
```
$ bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --from-beginning --topic low-ava

$ bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --from-beginning --topic closed-station

$ bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --from-beginning --topic turn-over
```

Outputs should look like this:

```
# low availability topic

station_id: "3721", longitude: -73.9209334552288, latitude: 40.7675487799971, bikes: 0, capacity: 35,ratio: 0.00 %
station_id: "3725", longitude: -73.95840793848038, latitude: 40.7687620293096, bikes: 4, capacity: 41,ratio: 9.76 %
station_id: "3745", longitude: -73.92957486212252, latitude: 40.75651272984955, bikes: 0, capacity: 27,ratio: 0.00 %
station_id: "3757", longitude: -73.93372, latitude: 40.710681, bikes: 2, capacity: 23,ratio: 8.70 %
station_id: "3787", longitude: -73.97141, latitude: 40.69985, bikes: 0, capacity: 22,ratio: 0.00 %
station_id: "3790", longitude: -73.991581, latitude: 40.7003, bikes: 3, capacity: 37,ratio: 8.11 %
station_id: "3791", longitude: -74.04696375131607, latitude: 40.73520838045357, bikes: 3, capacity: 32,ratio: 9.38 %
```

```
# closed station topic

 StationId "2010" is temporarily closed (at Unknown time)
 StationId "3136" is temporarily closed (at 08/06/2019 14:28:55)
 StationId "3137" is temporarily closed (at 08/06/2019 16:03:34)
 StationId "3143" is temporarily closed (at 08/06/2019 21:43:33)
 StationId "3183" is temporarily closed (at 20/05/2019 08:46:01)
 StationId "3223" is temporarily closed (at Unknown time)
 StationId "3233" is temporarily closed (at 08/06/2019 12:01:07)
```

```
# turn over ratio topic

station_id: "3776", turnover: 60.87 %
station_id: "3777", turnover: 100.00 %
station_id: "3778", turnover: 96.30 %
station_id: "3779", turnover: 66.67 %
station_id: "3781", turnover: 63.33 %
station_id: "3782", turnover: 12.90 %
station_id: "3787", turnover: 0.00 %
station_id: "3788", turnover: 59.26 %
station_id: "3790", turnover: 8.11 %
station_id: "3791", turnover: 9.38 %
```

## Some useful commands

Describe current offsets and end offsets

```
$ bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group turn-over --describe
```

Reset current offsets to the earliest offset

```
$ bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group turn-over --topic test --reset-offsets --to-earliest --execute
```

Append delete.topic.enable=true to config/server.properties before executing delete command:

```
$ bin/kafka-topics.sh --zookeeper localhost:2181 --delete --topic info
```

Terminate Zookeeper and Kafka:

```
$ ps -ef | grep kafka | grep -v grep | awk '{print $2}'| xargs kill -9

$ ps -ef | grep zookeeper | grep -v grep | awk '{print $2}'| xargs kill -9
```
