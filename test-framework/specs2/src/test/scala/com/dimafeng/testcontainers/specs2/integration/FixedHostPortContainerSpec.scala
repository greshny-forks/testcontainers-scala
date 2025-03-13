package com.dimafeng.testcontainers.specs2.integration

import java.net.URL
import com.dimafeng.testcontainers.FixedHostPortGenericContainer
import com.dimafeng.testcontainers.specs2.TestContainerForAll
import org.specs2.Specification
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import scala.io.Source

class FixedHostPortContainerSpec
    extends Specification
    with TestContainerForAll
    with org.specs2.specification.BeforeAll
    with org.specs2.specification.AfterAll {
  override val containerDef = FixedHostPortGenericContainer.Def(
    "nginx:latest",
    waitStrategy = Some(new HttpWaitStrategy().forPath("/")),
    portBindings = Seq((8090, 80))
  )

  def is = s2"""
    FixedHostPortGenericContainer should
      start nginx and expose 8090 port on host $testFixedPortBinding
  """

  def testFixedPortBinding = {
    withContainers { container =>
      container.mappedPort(80) === 8090

      val response = Source
        .fromInputStream(
          new URL(
            s"http://${container.containerIpAddress}:${container.mappedPort(80)}/"
          ).openConnection().getInputStream
        )
        .mkString

      response must contain(
        "If you see this page, the nginx web server is successfully installed"
      )
    }
  }
}
