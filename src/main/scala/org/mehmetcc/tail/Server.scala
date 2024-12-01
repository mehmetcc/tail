package org.mehmetcc.tail

import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir._
import sttp.ws.WebSocketFrame
import zio._
import zio.http.{Server => ZioHttpServer}
import zio.json.EncoderOps
import zio.stream._

trait Server {
  val serve: Task[ExitCode]
}

object Server {
  val serve: RIO[Server, ExitCode] = ZIO.serviceWithZIO[Server](_.serve)
}

final case class ServerImpl(port: Int, endpoints: Endpoints) extends Server {
  override val serve: Task[ExitCode] =
    ZioHttpServer
      .serve(ZioHttpInterpreter().toHttp(endpoints.all))
      .provide(ZioHttpServer.defaultWithPort(port))
      .exitCode
}

object ServerImpl {
  val live: URLayer[TailConfig with Broadcast, Server] =
    ZLayer {
      for {
        config    <- ZIO.service[TailConfig]
        broadcast <- ZIO.service[Broadcast]
        endpoints  = Endpoints(broadcast)
      } yield ServerImpl(config.port, endpoints)
    }
}

final case class Endpoints(broadcast: Broadcast) {
  private val health: ZServerEndpoint[Any, Any] = endpoint.get.in("k-tail" / "health").zServerLogic(_ => ZIO.unit)

  private val socket: ZServerEndpoint[Any, ZioStreams & WebSockets] =
    endpoint.get
      .in("tail" / path[String]("topic"))
      .out(webSocketBodyRaw(ZioStreams))
      .zServerLogic(topic => logic(topic))

  private def logic(topic: String): UIO[Stream[Throwable, WebSocketFrame] => ZStream[Any, Throwable, WebSocketFrame]] =
    ZIO.succeed((in: Stream[Throwable, WebSocketFrame]) => ZStream.unwrap(frames(in, topic)))

  private def frames(
    in: Stream[Throwable, WebSocketFrame],
    topic: String
  ): Task[ZStream[Any, Throwable, WebSocketFrame]] = for {
    isClosed <- Promise.make[Throwable, Unit]
    dequeue  <- broadcast.subscribe(topic)
    control = in.collectZIO {
                case WebSocketFrame.Ping(bytes) =>
                  ZIO.succeed(WebSocketFrame.Pong(bytes))
                case close @ WebSocketFrame.Close(_, _) =>
                  isClosed
                    .succeed(())
                    .zipLeft(broadcast.unsubscribe(topic))
                    .as(close)
              }
    messages = ZStream.fromTQueue(dequeue).map(msg => WebSocketFrame.text(msg.toJson))
    frames   = messages.merge(control).interruptWhen(isClosed)
  } yield frames

  val all: List[ZServerEndpoint[Any, ZioStreams & WebSockets]] = List(health, socket)
}
