package org.mehmetcc

import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.ZServerEndpoint
import zio.Task

object Endpoints {
  val apiEndpoints: List[ZServerEndpoint[Any, Any]] = List()

  val docEndpoints: List[ZServerEndpoint[Any, Any]] =
    SwaggerInterpreter().fromServerEndpoints[Task](apiEndpoints, "tail", "1.0.0")

  val prometheusMetrics: PrometheusMetrics[Task] = PrometheusMetrics.default[Task]()
  val metricsEndpoint: ZServerEndpoint[Any, Any] = prometheusMetrics.metricsEndpoint

  val all: List[ZServerEndpoint[Any, Any]] = apiEndpoints ++ docEndpoints ++ List(metricsEndpoint)
}
