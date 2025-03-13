package com.dimafeng.testcontainers.specs2

import org.specs2.Specification
import scala.io.Source
import java.net.URL
import com.dimafeng.testcontainers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import com.dimafeng.testcontainers.GenericContainer.Def

class GenericContainerSpec
    extends Specification
    with TestContainerForAll
    with org.specs2.specification.BeforeAll
    with org.specs2.specification.AfterAll {
  override val containerDef: Def[GenericContainer] = GenericContainer.Def(
    "nginx:latest",
    exposedPorts = Seq(80),
    waitStrategy = Wait.forHttp("/")
  )

  def is = s2"""
    GenericContainer should
      start nginx and expose 80 port $testNginxContainer
  """

  def testNginxContainer = {
    withContainers { case container =>
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
