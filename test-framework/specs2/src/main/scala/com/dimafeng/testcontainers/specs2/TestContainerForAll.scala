package com.dimafeng.testcontainers.specs2

import com.dimafeng.testcontainers.ContainerDef
import org.specs2.Specification

/**
 * Starts a single container before all tests and stop it after all tests
 *
 * Example:
 * {{{
 * class MysqlSpec extends Specification with TestContainerForAll with BeforeAll with AfterAll {
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
 *   }
 * }
 * }}}
 *
 * Notes:
 * - You must extend both BeforeAll and AfterAll traits for proper lifecycle management
 * - If you override beforeAll() without calling super.beforeAll() your container won't start
 * - If you override afterAll() without calling super.afterAll() your container won't stop
 */
trait TestContainerForAll extends TestContainersForAll { 
  self: Specification with org.specs2.specification.BeforeAll with org.specs2.specification.AfterAll =>

  val containerDef: ContainerDef

  final override type Containers = containerDef.Container

  override def startContainers(): containerDef.Container = {
    containerDef.start()
  }
}
