package com.dimafeng.testcontainers.specs2

import org.specs2.Specification
import org.specs2.specification.BeforeAfterEach

/**
 * Starts containers before each test and stop them after each test
 *
 * Example:
 * {{{
 * class ExampleSpec extends Specification with TestContainersForEach {
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
 *   "test" >> {
 *     withContainers { case mysqlContainer and pgContainer =>
 *       // Inside your test body you can do with your containers whatever you want to
 *       mysqlContainer.jdbcUrl must not(beEmpty)
 *       pgContainer.jdbcUrl must not(beEmpty)
 *     }
 *   }
 * }
 * }}}
 *
 * Notes:
 * - If you override before() without calling super.before() your containers won't start
 * - If you override after() without calling super.after() your containers won't stop
 */
trait TestContainersForEach extends TestContainersSuite with BeforeAfterEach {
  self: Specification =>

  def before = {
    val containers = startContainers()
    startedContainers = Some(containers)
    try {
      afterContainersStart(containers)
      beforeTest(containers)
    } catch {
      case e: Throwable =>
        stopContainers(containers)
        throw e
    }
  }

  def after = {
    startedContainers.foreach { containers =>
      afterTest(containers, None) // specs2 doesn't provide test failure info in after
      stopContainers(containers)
    }
  }
}
