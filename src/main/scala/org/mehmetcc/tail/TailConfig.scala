package org.mehmetcc.tail

import zio._
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.TypesafeConfigProvider

case class TailConfig(port: Int, bootstrapServers: List[String], groupId: String, topics: List[String])

object TailConfig {
  private val config: Config[TailConfig] = deriveConfig[TailConfig].nested("tail")

  val live: Layer[Config.Error, TailConfig] =
    ZLayer.fromZIO(
      ZIO
        .config[TailConfig](config)
        .withConfigProvider(TypesafeConfigProvider.fromResourcePath())
        .tap { config =>
          ZIO.logInfo(s"""
                         |k-tail server configuration:
                         |port: ${config.port}
                         |bootstrap-servers: ${config.bootstrapServers.mkString(",")}
                         |group-id: ${config.groupId}
                         |topics: ${config.topics.mkString(",")}
                         |""".stripMargin)
        }
    )
}
