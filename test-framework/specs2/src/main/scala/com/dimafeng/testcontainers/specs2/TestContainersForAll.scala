package com.dimafeng.testcontainers.specs2

import com.dimafeng.testcontainers.ContainerDef
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments

/**
  * Base trait for starting and stopping containers before and after all tests.
  */
trait TestContainersForAll { self: Specification =>

  type Containers

  def startContainers(): Containers

  // Start containers before all tests
  override def is = {
    val containers = startContainers()
    try {
      s2"""
        ${executeTests(containers)}
      """
    } finally {
      stopContainers(containers)
    }
  }

  protected def executeTests(containers: Containers): Fragments

  protected def stopContainers(containers: Containers): Unit
}
