package org.mehmetcc.tail

import zio.kafka.consumer.Subscription.Topics
import zio.kafka.consumer.{ConsumerSettings, Consumer => KafkaConsumer}
import zio.kafka.serde.Serde
import zio.stream._
import zio.{RLayer, URIO, ZIO, ZLayer}

trait Consumer {
  val consume: Stream[Throwable, Message]
}

object Consumer {
  val consume: URIO[Consumer, Stream[Throwable, Message]] = ZIO.serviceWith[Consumer](_.consume)
}

final case class ConsumerImpl(topics: Set[String], kafka: KafkaConsumer) extends Consumer {
  override val consume: Stream[Throwable, Message] = kafka
    .plainStream[Any, Array[Byte], Array[Byte]](Topics(topics), Serde.byteArray, Serde.byteArray)
    .map(record =>
      Message(
        record.offset.topicPartition.topic(),
        record.partition,
        record.offset.offset,
        record.key,
        record.value
      )
    )
}

object ConsumerImpl {
  val live: RLayer[TailConfig, Consumer] = ZLayer.scoped {
    for {
      config  <- ZIO.service[TailConfig]
      topics   = config.topics.toSet
      settings = ConsumerSettings(config.bootstrapServers).withGroupId(config.groupId)
      kafka   <- KafkaConsumer.make(settings)
    } yield ConsumerImpl(topics, kafka)
  }
}
