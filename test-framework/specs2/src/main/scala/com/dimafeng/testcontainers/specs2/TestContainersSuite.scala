package com.dimafeng.testcontainers.specs2

import com.dimafeng.testcontainers.implicits.DockerImageNameConverters
import com.dimafeng.testcontainers.lifecycle.Andable
import com.dimafeng.testcontainers.lifecycle.TestLifecycleAware
import org.specs2.Specification
import org.testcontainers.lifecycle.TestDescription

private[specs2] trait TestContainersSuite extends DockerImageNameConverters { self: Specification =>

  /** To use testcontainers specs2 suites you need to declare,
    * which containers you want to use inside your tests.
    *
    * For example:
    * {{{
    *   override type Containers = MySQLContainer
    * }}}
    *
    * If you want to use multiple containers inside your tests, use `and` syntax:
    * {{{
    *   override type Containers = MySQLContainer and PostgreSQLContainer
    * }}}
    */
  type Containers <: Andable

  /** Contains containers startup logic.
    * In this method you can use any intermediate logic.
    * You can pass parameters between containers, for example:
    * {{{
    * override def startContainers(): Containers = {
    *   val container1 = Container1.Def().start()
    *   val container2 = Container2.Def(container1.someParam).start()
    *   container1 and container2
    * }
    * }}}
    *
    * @return Started containers
    */
  def startContainers(): Containers

  /** To use containers inside your test bodies you need to use `withContainers` function:
    * {{{
    * "mysql container" >> {
    *   "should have jdbcUrl" >> {
    *     withContainers { mysqlContainer =>
    *       mysqlContainer.jdbcUrl must not(beEmpty)
    *     }
    *   }
    * }}}
    * `withContainers` also supports multiple containers:
    * {{{
    * "mysql and postgres containers" >> {
    *   "should test something" >> {
    *     withContainers { mysqlContainer and postgresContainer=>
    *       // tests
    *     }
    *   }
    * }
    * }}}
    *
    * @param runTest Test body
    */
  def withContainers[A](runTest: Containers => A): A = {
    val c = startedContainers.getOrElse(throw IllegalWithContainersCall())
    runTest(c)
  }

  /** Override, if you want to do something after containers start.
    */
  def afterContainersStart(containers: Containers): Unit = {}

  /** Override, if you want to do something before containers stop.
    */
  def beforeContainersStop(containers: Containers): Unit = {}

  @volatile private[testcontainers] var startedContainers: Option[Containers] =
    None

  private[specs2] def createDescription(spec: Specification): TestDescription =
    new TestDescription {
      override def getTestId(): String = spec.hashCode().toString()
      override def getFilesystemFriendlyName(): String =
        spec.getClass().getSimpleName().replaceAll("[^a-zA-Z0-9.-]", "_")
    }

  private val suiteDescription: TestDescription = createDescription(self)

  private[testcontainers] def beforeTest(containers: Containers): Unit = {
    containers.foreach {
      case container: TestLifecycleAware =>
        container.beforeTest(suiteDescription)
      case _ => // do nothing
    }
  }

  private[testcontainers] def afterTest(
      containers: Containers,
      throwable: Option[Throwable]
  ): Unit = {
    containers.foreach {
      case container: TestLifecycleAware =>
        container.afterTest(suiteDescription, throwable)
      case _ => // do nothing
    }
  }

  private[testcontainers] def stopContainers(containers: Containers): Unit = {
    try {
      beforeContainersStop(containers)
    }
    finally {
      try {
        startedContainers.foreach(_.stop())
      }
      finally {
        startedContainers = None
      }
    }
  }

}
case class IllegalWithContainersCall()
    extends IllegalStateException(
      "'withContainers' method can't be used before all containers are started. " +
        "'withContainers' method should be used only in test cases to prevent this."
    )
