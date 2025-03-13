package com.dimafeng.testcontainers.specs2

import com.dimafeng.testcontainers.ContainerDef
import org.specs2.Specification
import org.specs2.main.Arguments
import org.specs2.specification.AfterAll
import org.specs2.specification.BeforeAll
import org.specs2.specification.core.EnvDefault

/**
 * Starts containers before all tests and stop them after all tests
 *
 * Example:
 * {{{
 * class ExampleSpec extends Specification with TestContainersForAll with BeforeAll with AfterAll {
 *
 *   // First of all, you need to declare, which containers you want to use
 *   override type Containers = MySQLContainer and PostgreSQLContainer
 *
 *   // After that, you need to describe, how you want to start them,
 *   // In this method you can use any intermediate logic.
 *   // You can pass parameters between containers, for example.
 *   override def startContainers(): Containers = {
 *     val container1 = MySQLContainer.Def().start()
 *     val container2 = PostgreSQLContainer.Def().start()
 *     container1 and container2
 *   }
 *
 *   // `withContainers` function supports multiple containers:
 *   "mysql and postgres containers" should {
 *     "test something" in {
 *       withContainers { case mysqlContainer and pgContainer =>
 *         // Inside your test body you can do with your containers whatever you want to
 *         mysqlContainer.jdbcUrl must not(beEmpty)
 *         pgContainer.jdbcUrl must not(beEmpty)
 *       }
 *     }
 *   }
 * }
 * }}}
 *
 * Notes:
 * - If you override beforeAll() without calling super.beforeAll() your containers won't start
 * - If you override afterAll() without calling super.afterAll() your containers won't stop
 */
trait TestContainersForAll extends TestContainersSuite {
  self: Specification with BeforeAll with AfterAll =>
  override def beforeAll(): Unit = {
    //val env = EnvDefault.create(Arguments())
    // self.is.examples
    val containers = startContainers()
    startedContainers = Some(containers)

    afterContainersStart(containers = containers)
  }

  override def afterAll(): Unit = {
    startedContainers.foreach(stopContainers)
  }
}
