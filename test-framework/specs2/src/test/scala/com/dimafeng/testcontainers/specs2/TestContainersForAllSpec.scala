package com.dimafeng.testcontainers.specs2

import java.util.Optional
import com.dimafeng.testcontainers.lifecycle.and
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{times, verify, never}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}

class TestContainersForAllSpec extends BaseSpec[TestContainersForAll] {

  "TestContainersForAll" should {
    "provide multiple container access" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainersForAll
          with org.specs2.specification.BeforeAll
          with org.specs2.specification.AfterAll {

        override type Containers = SampleContainer and SampleContainer

        override def startContainers(): Containers = {
          val c1 = SampleContainer.Def(container1).start()
          val c2 = SampleContainer.Def(container2).start()
          c1 and c2
        }

        def is = s2"test"
      }

      val suite = new TestSuite()
      val containers = suite.startContainers()
      containers must not(beNull)
    }

    "call lifecycle methods for all containers" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainersForAll
          with org.specs2.specification.BeforeAll
          with org.specs2.specification.AfterAll {

        override type Containers = SampleContainer and SampleContainer

        override def startContainers(): Containers = {
          val c1 = SampleContainer.Def(container1).start()
          val c2 = SampleContainer.Def(container2).start()
          c1 and c2
        }

        def is = s2"test"
      }

      val suite = new TestSuite()

      // Test the lifecycle manually
      suite.beforeAll
      suite.afterAll

      // Verify both containers are started and stopped
      verify(container1).start()
      verify(container1).stop()
      verify(container2).start()
      verify(container2).stop()

      ok
    }

    "provide withContainers functionality for multiple containers" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainersForAll
          with org.specs2.specification.BeforeAll
          with org.specs2.specification.AfterAll {

        override type Containers = SampleContainer and SampleContainer

        override def startContainers(): Containers = {
          val c1 = SampleContainer.Def(container1).start()
          val c2 = SampleContainer.Def(container2).start()
          c1 and c2
        }

        def is = s2"test"

        def testWithContainers(): String = {
          beforeAll
          try {
            withContainers { case c1 and c2 =>
              c1 must not(beNull)
              c2 must not(beNull)
              "success"
            }
          } finally {
            afterAll
          }
        }
      }

      val suite = new TestSuite()
      val result = suite.testWithContainers()

      result === "success"
      verify(container1).start()
      verify(container1).stop()
      verify(container2).start()
      verify(container2).stop()
      ok
    }

    "handle container startup errors gracefully" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainersForAll
          with org.specs2.specification.BeforeAll
          with org.specs2.specification.AfterAll {

        override type Containers = SampleContainer and SampleContainer

        override def startContainers(): Containers = {
          val c1 = SampleContainer.Def(container1).start()
          val c2 = SampleContainer.Def(container2).start()
          c1 and c2
        }

        override def afterContainersStart(containers: Containers): Unit = {
          throw new RuntimeException("Startup error")
        }

        def is = s2"test"
      }

      val suite = new TestSuite()

      // Should handle startup errors
      suite.beforeAll must throwA[RuntimeException]

      // Cleanup should still work
      verify(container1).start()
      verify(container1).stop()
      verify(container2).start()
      verify(container2).stop()

      ok
    }

    "support custom lifecycle hooks with multiple containers" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]
      @volatile var afterStartCalled = false
      @volatile var beforeStopCalled = false
      @volatile var receivedContainers
          : Option[SampleContainer and SampleContainer] = None

      class TestSuite
          extends org.specs2.Specification
          with TestContainersForAll
          with org.specs2.specification.BeforeAll
          with org.specs2.specification.AfterAll {

        override type Containers = SampleContainer and SampleContainer

        override def startContainers(): Containers = {
          val c1 = SampleContainer.Def(container1).start()
          val c2 = SampleContainer.Def(container2).start()
          c1 and c2
        }

        override def afterContainersStart(containers: Containers): Unit = {
          afterStartCalled = true
          receivedContainers = Some(containers)
        }

        override def beforeContainersStop(containers: Containers): Unit = {
          beforeStopCalled = true
        }

        def is = s2"test"
      }

      val suite = new TestSuite()
      suite.beforeAll
      suite.afterAll

      afterStartCalled must beTrue
      beforeStopCalled must beTrue
      receivedContainers must beSome
      verify(container1).start()
      verify(container1).stop()
      verify(container2).start()
      verify(container2).stop()
      ok
    }

    "handle TestLifecycleAware for multiple containers" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainersForAll
          with org.specs2.specification.BeforeAll
          with org.specs2.specification.AfterAll {

        override type Containers = SampleContainer and SampleContainer

        override def startContainers(): Containers = {
          val c1 = SampleContainer.Def(container1).start()
          val c2 = SampleContainer.Def(container2).start()
          c1 and c2
        }

        def is = s2"test"

        def simulateTest(): Unit = {
          beforeAll
          // Simulate test execution by calling beforeTest/afterTest manually
          startedContainers.foreach { containers =>
            beforeTest(containers)
            afterTest(containers, None)
          }
          afterAll
        }
      }

      val suite = new TestSuite()
      suite.simulateTest()

      // Verify both containers get lifecycle calls
      verify(container1).start()
      verify(container1).beforeTest(any())
      verify(container1).afterTest(
        any(),
        ArgumentMatchers.eq(Optional.empty[Throwable]())
      )
      verify(container1).stop()

      verify(container2).start()
      verify(container2).beforeTest(any())
      verify(container2).afterTest(
        any(),
        ArgumentMatchers.eq(Optional.empty[Throwable]())
      )
      verify(container2).stop()
      ok
    }

    "demonstrate container dependency scenarios" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]
      @volatile var dependencyExecuted = false

      class TestSuite
          extends org.specs2.Specification
          with TestContainersForAll
          with org.specs2.specification.BeforeAll
          with org.specs2.specification.AfterAll {

        override type Containers = SampleContainer and SampleContainer

        override def startContainers(): Containers = {
          val c1 = SampleContainer.Def(container1).start()
          // Simulate container dependency (c2 depends on c1)
          val c2 = {
            dependencyExecuted = true
            SampleContainer.Def(container2).start()
          }
          c1 and c2
        }

        def is = s2"test"
      }

      val suite = new TestSuite()
      suite.beforeAll
      suite.afterAll

      // Both containers should be started
      verify(container1).start()
      verify(container2).start()

      // Dependency logic should be executed
      dependencyExecuted must beTrue
    }
  }
}
