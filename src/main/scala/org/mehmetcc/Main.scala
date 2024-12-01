package org.mehmetcc

import org.mehmetcc.tail._
import zio._
import zio.logging.LogFormat
import zio.logging.backend.SLF4J
import zio.stream.ZSink

object Main extends ZIOAppDefault {
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = SLF4J.slf4j(LogFormat.default)

  private val program = for {
    consume  <- Consumer.consume
    _        <- consume.mapZIO(Buffer.offer).run(ZSink.drain).forkDaemon
    exitCode <- Server.serve
  } yield exitCode

  override def run: Task[ExitCode] = program.provide(
    TailConfig.live,
    BufferImpl.live,
    ConsumerImpl.live,
    BroadcastImpl.live,
    ServerImpl.live
  )

}
