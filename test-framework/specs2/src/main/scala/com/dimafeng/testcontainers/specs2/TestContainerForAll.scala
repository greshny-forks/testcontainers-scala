package com.dimafeng.testcontainers.specs2

import com.dimafeng.testcontainers.ContainerDef
import org.specs2.mutable.Specification

/**
  * Starts a single container before all tests and stop it after all tests
  *
  * Example:
  * {{{
  * class MysqlSpec extends Specification with TestContainerForAll {
  *
  *   // You need to override `containerDef` with needed container definition
  *   override val containerDef = MySQLContainer.Def()
  *
  *   // To use containers in tests you need to use `withContainers` function
  *   "test" should {
  *     withContainers { mysqlContainer =>
  *       // Inside your test body you can do with your container whatever you want to
  *       mysqlContainer.jdbcUrl must not be empty
  *     }
  *   }
  * }
  * }}}
  */
trait TestContainerForAll extends TestContainersForAll { self: Specification =>

  val containerDef: ContainerDef

  final override type Containers = containerDef.Container

  override def startContainers(): containerDef.Container = {
    containerDef.start()
  }
}
