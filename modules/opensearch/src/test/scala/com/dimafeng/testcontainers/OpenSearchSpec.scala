package com.dimafeng.testcontainers

import org.scalatest.flatspec.AnyFlatSpec
import com.dimafeng.testcontainers.scalatest.TestContainersForAll

class OpensearchSpec extends AnyFlatSpec with TestContainersForAll {
  override type Containers = OpensearchContainer
  override def startContainers(): Containers = OpensearchContainer.Def().start()

  "Opensearch container" should "be started" in withContainers { container =>
    val restClient = ???
  }
}
