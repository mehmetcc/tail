package org.mehmetcc.tail

import zio.{Queue, UIO, ULayer, URIO, ZIO, ZLayer}

trait Buffer {
  def offer(message: Message): UIO[Unit]
  def poll(): UIO[Option[Message]]
}

object Buffer {
  def offer(message: Message): URIO[Buffer, Unit] = ZIO.serviceWithZIO[Buffer](_.offer(message))

  def poll(): URIO[Buffer, Option[Message]] = ZIO.serviceWithZIO[Buffer](_.poll())
}

final case class BufferImpl(queue: Queue[Message]) extends Buffer {
  override def offer(message: Message): UIO[Unit] =
    queue
      .offer(message)
      .tap {
        case true  => ZIO.logInfo(s"Successfully buffered message ${message.id}")
        case false => ZIO.logError(s"Failed to buffer message ${message.id}")
      }
      .unit

  override def poll(): UIO[Option[Message]] = queue.poll
}

object BufferImpl {
  val live: ULayer[Buffer] = ZLayer {
    Queue.unbounded[Message].map(BufferImpl(_))
  }
}
