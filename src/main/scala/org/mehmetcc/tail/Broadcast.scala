package org.mehmetcc.tail

import zio.stm._
import zio.stream.{ZSink, ZStream}
import zio.{RIO, Task, UIO, URIO, URLayer, ZIO, ZLayer}

trait Broadcast {
  def subscribe(topic: String): Task[TDequeue[Message]]

  def unsubscribe(topic: String): UIO[Unit]
}

object Broadcast {
  def subscribe(topic: String): RIO[Broadcast, TDequeue[Message]] = ZIO.serviceWithZIO[Broadcast](_.subscribe(topic))

  def unsubscribe(topic: String): RIO[Broadcast, Unit] = ZIO.serviceWithZIO[Broadcast](_.unsubscribe(topic))
}

final case class BroadcastImpl(subscribers: TMap[String, Int], hubs: TMap[String, THub[Message]]) extends Broadcast {
  override def subscribe(topic: String): Task[TDequeue[Message]] = checkin(topic).commit.flatMap {
    case (subscribers, dequeue) => ZIO.logInfo(s"Topic $topic has $subscribers subscriber(s)").as(dequeue)
  }

  private def checkin(topic: String): USTM[(Int, TDequeue[Message])] = for {
    subscribers <- incrementSubscribers(topic)
    hub         <- resolveHub(topic)
    dequeue     <- hub.subscribe
  } yield (subscribers, dequeue)

  private def resolveHub(topic: String): USTM[THub[Message]] = hubs.get(topic).flatMap {
    case Some(hub) => STM.succeed(hub)
    case _         => THub.unbounded[Message].tap(hubs.put(topic, _))
  }

  private def incrementSubscribers(topic: String): USTM[Int] = subscribers
    .updateWith(topic) {
      case Some(count) => Some(count + 1)
      case _           => Some(1)
    }
    .map(_.get) // since we always map to Some, this is a safe operation

  override def unsubscribe(topic: String): UIO[Unit] = checkout(topic).commit.flatMap { subscribers =>
    ZIO.logInfo(s"topic $topic has ${subscribers.getOrElse(0)} subscriber(s)")
  }

  private def checkout(topic: String): USTM[Option[Int]] = for {
    subscribers <- decrementSubscribers(topic)
    _           <- if (subscribers.isEmpty) hubs.delete(topic) else STM.unit
  } yield subscribers

  private def decrementSubscribers(topic: String): USTM[Option[Int]] = subscribers.updateWith(topic) {
    case Some(1)     => None
    case Some(count) => Some(count - 1)
    case None        => None
  }
}

object BroadcastImpl {
  val live: URLayer[Buffer, Broadcast] = ZLayer {
    for {
      subscribers <- TMap.empty[String, Int].commit
      hubs        <- TMap.empty[String, THub[Message]].commit
      _ <- ZStream
             .repeatZIO((Buffer.poll()))
             .mapZIO {
               case Some(message) =>
                 val publish = for {
                   hub <- hubs.get(message.topic)
                   _   <- hub.map(_.publish(message).unit).getOrElse(STM.unit)
                 } yield ()

                 publish.commit
               case _ => ZIO.unit
             }
             .run(ZSink.drain)
             .forkDaemon
    } yield BroadcastImpl(subscribers, hubs)
  }
}
