package com.dimafeng.testcontainers.specs2

import java.util.Optional
import com.dimafeng.testcontainers.ContainerDef
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{times, verify, never}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}

class TestContainerForEachSpec extends BaseSpec[TestContainerForEach] {

  "TestContainerForEach" should {
    "provide container definition access" in {
      val container = mock[SampleJavaContainer]
      val containerDef = SampleContainer.Def(container)

      containerDef must not(beNull)
      containerDef.start() must not(beNull)
    }

    "be compatible with specs2 BeforeAfterEach trait" in {
      // This test verifies that the trait compiles with the required self-types
      class TestSuite
          extends org.specs2.Specification
          with TestContainerForEach {
        override val containerDef =
          SampleContainer.Def(mock[SampleJavaContainer])
        def is = s2"TestContainerForEach compilation test"
      }

      val suite = new TestSuite()
      suite must not(beNull)
      suite.containerDef must not(beNull)
    }

    "call lifecycle methods correctly for each test" in {
      val container = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainerForEach {
        override val containerDef = SampleContainer.Def(container)
        def is = s2"test"
      }

      val suite = new TestSuite()

      // Manually invoke ForEach lifecycle methods multiple times
      suite.before
      suite.after
      suite.before
      suite.after

      // Verify container lifecycle was called twice (ForEach pattern)
      verify(container, times(2)).start()
      verify(container, times(2)).stop()
      ok
    }

    "handle container startup errors gracefully" in {
      val container = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainerForEach {
        override val containerDef = SampleContainer.Def(container)
        override def afterContainersStart(
            containers: containerDef.Container
        ): Unit = {
          throw new RuntimeException("Startup error")
        }
        def is = s2"test"
      }

      val suite = new TestSuite()

      // Should handle errors gracefully
      suite.before must throwA[RuntimeException]

      // Cleanup should still work
      verify(container).start()
      verify(container).stop()
      ok
    }

    "provide withContainers method functionality" in {
      val container = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainerForEach {
        override val containerDef = SampleContainer.Def(container)
        def is = s2"test"

        def testWithContainers(): String = {
          // Start containers first
          before
          try {
            withContainers { _ => "success" }
          } finally {
            after
          }
        }
      }

      val suite = new TestSuite()
      val result = suite.testWithContainers()

      result === "success"
      verify(container).start()
      verify(container).stop()
      ok
    }

    "support custom lifecycle hooks for each test" in {
      val container = mock[SampleJavaContainer]
      @volatile var afterStartCallCount = 0
      @volatile var beforeStopCallCount = 0

      class TestSuite
          extends org.specs2.Specification
          with TestContainerForEach {
        override val containerDef = SampleContainer.Def(container)

        override def afterContainersStart(
            containers: containerDef.Container
        ): Unit = {
          afterStartCallCount += 1
        }

        override def beforeContainersStop(
            containers: containerDef.Container
        ): Unit = {
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
      verify(container, times(2)).start()
      verify(container, times(2)).stop()
      ok
    }

    "handle TestLifecycleAware containers for each test" in {
      val container = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainerForEach {
        override val containerDef = SampleContainer.Def(container)
        def is = s2"test"

        def simulateTest(): Unit = {
          before
          // Simulate test execution by calling beforeTest/afterTest manually
          startedContainers.foreach { c =>
            beforeTest(c)
            afterTest(c, None)
          }
          after
        }
      }

      val suite = new TestSuite()
      // Simulate two test executions
      suite.simulateTest()
      suite.simulateTest()

      verify(container, times(2)).start()
      verify(container, times(2)).beforeTest(any())
      verify(container, times(2))
        .afterTest(any(), ArgumentMatchers.eq(Optional.empty[Throwable]()))
      verify(container, times(2)).stop()
      ok
    }

    "demonstrate container isolation between tests" in {
      val container = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainerForEach {
        override val containerDef = SampleContainer.Def(container)
        def is = s2"test"

        @volatile var testExecutions = 0

        def simulateTest(): Unit = {
          before
          try {
            withContainers { _ =>
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
      verify(container, times(2)).start()
      verify(container, times(2)).stop()
      suite.testExecutions === 2
    }
  }
}
