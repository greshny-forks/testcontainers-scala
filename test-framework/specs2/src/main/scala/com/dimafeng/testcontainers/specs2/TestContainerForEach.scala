package com.dimafeng.testcontainers.specs2

import com.dimafeng.testcontainers.ContainerDef
import org.specs2.Specification

/**
 * Starts a single container before each test and stop it after each test
 *
 * Example:
 * {{{
 * class MysqlSpec extends Specification with TestContainerForEach {
 *
 *   // You need to override `containerDef` with needed container definition
 *   override val containerDef = MySQLContainer.Def()
 *
 *   // To use containers in tests you need to use `withContainers` function
 *   "mysql container" should {
 *     "have a valid JDBC URL" in {
 *       withContainers { mysqlContainer =>
 *         // Inside your test body you can do with your container whatever you want to
 *         mysqlContainer.jdbcUrl must not(beEmpty)
 *       }
 *     }
 *     
 *     "accept connections" in {
 *       withContainers { mysqlContainer =>
 *         // Each test gets a fresh container instance
 *         mysqlContainer.jdbcUrl must not(beEmpty)
 *       }
 *     }
 *   }
 * }
 * }}}
 *
 * Notes:
 * - If you override before() without calling super.before() your container won't start
 * - If you override after() without calling super.after() your container won't stop
 * - Each test gets a fresh container instance
 */
trait TestContainerForEach extends TestContainersForEach { self: Specification =>

  val containerDef: ContainerDef

  final override type Containers = containerDef.Container

  override def startContainers(): containerDef.Container = {
    containerDef.start()
  }
}
