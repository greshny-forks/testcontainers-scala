package com.dimafeng.testcontainers.specs2

import com.dimafeng.testcontainers.ContainerDef
import org.specs2.mutable.Specification
import org.specs2.execute.{AsResult, Result}
import org.specs2.specification.core.Fragments

/** Base trait for starting and stopping containers before and after each test.
  */
trait TestContainersForEach { self: Specification =>

  type Containers

  def startContainers(): Containers

  // Start containers before each test
  override def is = {
    s2"""
      ${executeTests}
    """
  }

  protected def executeTests: Fragments = {
    val containers = startContainers()
    try {
      s2"""
        ${withContainers(containers)}
      """
    } finally {
      stopContainers(containers)
    }
  }

  protected def withContainers(containers: Containers): Fragments

  protected def stopContainers(containers: Containers): Unit
}
