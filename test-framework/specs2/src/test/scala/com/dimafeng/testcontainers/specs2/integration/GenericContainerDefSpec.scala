package com.dimafeng.testcontainers.specs2.integration

import java.net.URL
import com.dimafeng.testcontainers.{GenericContainer, SingleContainer}
import com.dimafeng.testcontainers.lifecycle.and
import com.dimafeng.testcontainers.specs2.TestContainersForAll
import org.specs2.Specification
import org.testcontainers.containers.wait.strategy.Wait
import scala.io.Source

class GenericContainerDefSpec
    extends Specification
    with TestContainersForAll
    with org.specs2.specification.BeforeAll
    with org.specs2.specification.AfterAll {

  import GenericContainerDefSpec._

  override type Containers =
    CompatibleGenericContainer and NotCompatibleGenericContainer

  override def startContainers(): Containers = {
    val compatible = CompatibleGenericContainer.Def().start()
    val notCompatible = NotCompatibleGenericContainer.Def().start()
    compatible and notCompatible
  }

  def is = s2"""
    GenericContainer.Def should
      be able to work through compatible and not compatible constructors $testGenericContainerDef
  """

  def testGenericContainerDef = {
    withContainers { case compatible and notCompatible =>
      val expectedText =
        "If you see this page, the nginx web server is successfully installed"
      (compatible.rootPage must contain(expectedText)) and
        (notCompatible.rootPage must contain(expectedText))
    }
  }
}

object GenericContainerDefSpec {

  private val port = 80

  private def createUrl(container: SingleContainer[?]) = {
    new URL(
      s"http://${container.containerIpAddress}:${container.mappedPort(port)}/"
    )
  }

  private def urlToString(url: URL) = {
    Source.fromInputStream(url.openConnection().getInputStream).mkString
  }

  class CompatibleGenericContainer
      extends GenericContainer(
        dockerImage = "nginx:latest",
        exposedPorts = Seq(port),
        waitStrategy = Some(Wait.forHttp("/"))
      ) {
    def rootUrl: URL = createUrl(this)
    def rootPage: String = urlToString(rootUrl)
  }
  object CompatibleGenericContainer {
    case class Def()
        extends GenericContainer.Def[CompatibleGenericContainer](
          new CompatibleGenericContainer()
        )
  }

  class NotCompatibleGenericContainer(underlying: GenericContainer)
      extends GenericContainer(underlying) {
    def rootUrl: URL = createUrl(this)
    def rootPage: String = urlToString(rootUrl)
  }
  object NotCompatibleGenericContainer {
    case class Def()
        extends GenericContainer.Def[NotCompatibleGenericContainer](
          new NotCompatibleGenericContainer(
            GenericContainer(
              dockerImage = "nginx:latest",
              exposedPorts = Seq(port),
              waitStrategy = Wait.forHttp("/")
            )
          )
        )
  }
}
