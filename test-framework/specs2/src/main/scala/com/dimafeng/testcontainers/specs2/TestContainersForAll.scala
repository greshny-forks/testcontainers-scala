package com.dimafeng.testcontainers.specs2

import org.specs2.specification.AfterAll
import org.specs2.specification.BeforeAll

trait TestContainersForAll extends BeforeAll with AfterAll {
  override def beforeAll(): Unit = ???

  override def afterAll(): Unit = ???
}
