package com.dimafeng.testcontainers.specs2

import com.dimafeng.testcontainers.ContainerDef
import org.specs2.Specification
import org.specs2.main.Arguments
import org.specs2.specification.AfterAll
import org.specs2.specification.BeforeAll
import org.specs2.specification.core.EnvDefault

trait TestContainersForAll extends TestContainersSuite {
  self: Specification with BeforeAll with AfterAll =>
  override def beforeAll(): Unit = {
    //val env = EnvDefault.create(Arguments())
    // self.is.examples
    val containers = startContainers()
    startedContainers = Some(containers)

    afterContainersStart(containers = containers)
  }

  override def afterAll(): Unit = ???
}
