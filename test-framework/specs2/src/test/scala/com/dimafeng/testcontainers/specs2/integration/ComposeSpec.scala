package com.dimafeng.testcontainers.specs2.integration

import java.io.File
import java.time.Duration.ofSeconds
import com.dimafeng.testcontainers.{DockerComposeContainer, WaitingForService}
import com.dimafeng.testcontainers.specs2.TestContainerForAll
import org.specs2.Specification
import org.testcontainers.containers.wait.strategy.Wait

class ComposeWaitingForSpec
    extends Specification
    with TestContainerForAll
    with org.specs2.specification.BeforeAll
    with org.specs2.specification.AfterAll {
  override val containerDef = DockerComposeContainer.Def(
    composeFiles = Seq(
      new File(
        getClass.getClassLoader.getResource("docker-compose.yml").getPath
      )
    ),
    waitingFor = Some(
      WaitingForService(
        "redis",
        Wait.forLogMessage(".*Ready to accept connections\\n", 1)
      )
    )
  )

  def is = s2"""
    DockerComposeContainer should
      wait for service $testWaitForService
  """

  def testWaitForService = {
    withContainers { case container =>
      // container.start() should blocks until successful or timeout
      container.getContainerByServiceName("redis-1").get.isRunning must beTrue
    }
  }
}

class ComposeWaitingForWithTimeoutSpec extends Specification {

  def is = s2"""
    DockerComposeContainer should
      throw exception when timeout occurs $testTimeoutException
  """

  def testTimeoutException = {
    val waitStrategy = Wait
      .forLogMessage("this is never happen", 1)
      .withStartupTimeout(ofSeconds(5L))

    val container = DockerComposeContainer(
      Seq(
        new File(
          getClass.getClassLoader.getResource("docker-compose.yml").getPath
        )
      ),
      waitingFor = Some(WaitingForService("redis", waitStrategy))
    )

    val caught =
      try {
        container.start()
        throw new AssertionError("Expected RuntimeException")
      } catch {
        case e: RuntimeException => e
      } finally {
        container.stop()
      }

    caught.getMessage must contain("Timed out waiting for log output matching")
  }
}
