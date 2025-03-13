package com.dimafeng.testcontainers.specs2

import java.util.Optional
import com.dimafeng.testcontainers.lifecycle.and
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{times, verify, never}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}

class TestContainersForEachSpec extends BaseSpec[TestContainersForEach] {

  "TestContainersForEach" should {
    "provide multiple container access" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainersForEach {
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

    "call lifecycle methods for each test with all containers" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainersForEach {
        override type Containers = SampleContainer and SampleContainer

        override def startContainers(): Containers = {
          val c1 = SampleContainer.Def(container1).start()
          val c2 = SampleContainer.Def(container2).start()
          c1 and c2
        }

        def is = s2"test"
      }

      val suite = new TestSuite()

      // Simulate two test runs (ForEach pattern)
      suite.before
      suite.after

      suite.before
      suite.after

      // Verify all containers get lifecycle calls for each test
      verify(container1, times(2)).start()
      verify(container1, times(2)).stop()
      verify(container2, times(2)).start()
      verify(container2, times(2)).stop()

      ok
    }

    "provide withContainers functionality for multiple containers" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainersForEach {
        override type Containers = SampleContainer and SampleContainer

        override def startContainers(): Containers = {
          val c1 = SampleContainer.Def(container1).start()
          val c2 = SampleContainer.Def(container2).start()
          c1 and c2
        }

        def is = s2"test"

        def testWithContainers(): String = {
          before
          try {
            withContainers { case c1 and c2 =>
              c1 must not(beNull)
              c2 must not(beNull)
              "success"
            }
          } finally {
            after
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
          with TestContainersForEach {
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
      suite.before must throwA[RuntimeException]

      // Cleanup should still work for all containers
      verify(container1).start()
      verify(container1).stop()
      verify(container2).start()
      verify(container2).stop()

      ok
    }

    "support custom lifecycle hooks for each test with multiple containers" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]
      @volatile var afterStartCallCount = 0
      @volatile var beforeStopCallCount = 0
      @volatile var receivedContainers
          : Option[SampleContainer and SampleContainer] = None

      class TestSuite
          extends org.specs2.Specification
          with TestContainersForEach {
        override type Containers = SampleContainer and SampleContainer

        override def startContainers(): Containers = {
          val c1 = SampleContainer.Def(container1).start()
          val c2 = SampleContainer.Def(container2).start()
          c1 and c2
        }

        override def afterContainersStart(containers: Containers): Unit = {
          afterStartCallCount += 1
          receivedContainers = Some(containers)
        }

        override def beforeContainersStop(containers: Containers): Unit = {
          beforeStopCallCount += 1
        }

        def is = s2"test"
      }

      val suite = new TestSuite()
      // Simulate two test runs
      suite.before
      suite.after
      suite.before
      suite.after

      afterStartCallCount === 2 // Called for each test
      beforeStopCallCount === 2 // Called for each test
      receivedContainers must beSome
      verify(container1, times(2)).start()
      verify(container1, times(2)).stop()
      verify(container2, times(2)).start()
      verify(container2, times(2)).stop()
      ok
    }

    "handle TestLifecycleAware for multiple containers in ForEach pattern" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainersForEach {
        override type Containers = SampleContainer and SampleContainer

        override def startContainers(): Containers = {
          val c1 = SampleContainer.Def(container1).start()
          val c2 = SampleContainer.Def(container2).start()
          c1 and c2
        }

        def is = s2"test"

        def simulateTest(): Unit = {
          before
          // Simulate test execution by calling beforeTest/afterTest manually
          startedContainers.foreach { containers =>
            beforeTest(containers)
            afterTest(containers, None)
          }
          after
        }
      }

      val suite = new TestSuite()
      // Simulate two test executions
      suite.simulateTest()
      suite.simulateTest()

      // Verify both containers get lifecycle calls for each test
      verify(container1, times(2)).start()
      verify(container1, times(2)).beforeTest(any())
      verify(container1, times(2))
        .afterTest(any(), ArgumentMatchers.eq(Optional.empty[Throwable]()))
      verify(container1, times(2)).stop()

      verify(container2, times(2)).start()
      verify(container2, times(2)).beforeTest(any())
      verify(container2, times(2))
        .afterTest(any(), ArgumentMatchers.eq(Optional.empty[Throwable]()))
      verify(container2, times(2)).stop()
      ok
    }

    "demonstrate container isolation between tests" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]
      @volatile var testExecutions = 0

      class TestSuite
          extends org.specs2.Specification
          with TestContainersForEach {
        override type Containers = SampleContainer and SampleContainer

        override def startContainers(): Containers = {
          val c1 = SampleContainer.Def(container1).start()
          val c2 = SampleContainer.Def(container2).start()
          c1 and c2
        }

        def is = s2"test"

        def simulateTest(): Unit = {
          before
          try {
            withContainers { case c1 and c2 =>
              testExecutions += 1
              "test"
            }
          } finally {
            after
          }
        }
      }

      val suite = new TestSuite()
      suite.simulateTest()
      suite.simulateTest()

      // Each test gets fresh container instances (ForEach pattern)
      verify(container1, times(2)).start()
      verify(container1, times(2)).stop()
      verify(container2, times(2)).start()
      verify(container2, times(2)).stop()

      testExecutions === 2
    }

    "handle container dependency scenarios for each test" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]
      @volatile var dependencyExecutions = 0

      class TestSuite
          extends org.specs2.Specification
          with TestContainersForEach {
        override type Containers = SampleContainer and SampleContainer

        override def startContainers(): Containers = {
          val c1 = SampleContainer.Def(container1).start()
          // Simulate container dependency (c2 depends on c1)
          val c2 = {
            dependencyExecutions += 1
            SampleContainer.Def(container2).start()
          }
          c1 and c2
        }

        def is = s2"test"
      }

      val suite = new TestSuite()
      // Simulate two test runs
      suite.before
      suite.after
      suite.before
      suite.after

      // Both containers should be started for each test
      verify(container1, times(2)).start()
      verify(container2, times(2)).start()

      // Dependency logic should be executed for each test
      dependencyExecutions === 2
    }
  }
}
