package com.dimafeng.testcontainers.specs2

import java.util.Optional
import com.dimafeng.testcontainers.ContainerDef
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{times, verify, never}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}

class TestContainerForAllSpec extends BaseSpec[TestContainerForAll] {

  "TestContainerForAll" should {
    "provide container definition access" in {
      val container = mock[SampleJavaContainer]
      val containerDef = SampleContainer.Def(container)

      containerDef must not(beNull)
      containerDef.start() must not(beNull)
    }

    "be compatible with specs2 BeforeAll and AfterAll traits" in {
      // This test verifies that the trait compiles with the required self-types
      class TestSuite
          extends org.specs2.Specification
          with TestContainerForAll
          with org.specs2.specification.BeforeAll
          with org.specs2.specification.AfterAll {

        override val containerDef =
          SampleContainer.Def(mock[SampleJavaContainer])

        def is = s2"TestContainerForAll compilation test"
      }

      val suite = new TestSuite()
      suite must not(beNull)
      suite.containerDef must not(beNull)
    }

    "call lifecycle methods correctly via manual invocation" in {
      val container = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainerForAll
          with org.specs2.specification.BeforeAll
          with org.specs2.specification.AfterAll {

        override val containerDef = SampleContainer.Def(container)
        def is = s2"test"
      }

      val suite = new TestSuite()

      // Manually invoke lifecycle methods to verify behavior
      suite.beforeAll
      suite.afterAll

      // Verify container lifecycle was called
      verify(container).start()
      verify(container).stop()
      ok
    }

    "handle container startup errors gracefully" in {
      val container = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainerForAll
          with org.specs2.specification.BeforeAll
          with org.specs2.specification.AfterAll {

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
      suite.beforeAll must throwA[RuntimeException]

      // Cleanup should still work
      verify(container).start()
      verify(container).stop()
      ok
    }

    "provide withContainers method functionality" in {
      val container = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainerForAll
          with org.specs2.specification.BeforeAll
          with org.specs2.specification.AfterAll {

        override val containerDef = SampleContainer.Def(container)
        def is = s2"test"

        def testWithContainers(): String = {
          // Start containers first
          beforeAll
          try {
            withContainers { _ => "success" }
          } finally {
            afterAll
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

    "support custom lifecycle hooks" in {
      val container = mock[SampleJavaContainer]
      @volatile var afterStartCalled = false
      @volatile var beforeStopCalled = false

      class TestSuite
          extends org.specs2.Specification
          with TestContainerForAll
          with org.specs2.specification.BeforeAll
          with org.specs2.specification.AfterAll {

        override val containerDef = SampleContainer.Def(container)

        override def afterContainersStart(
            containers: containerDef.Container
        ): Unit = {
          afterStartCalled = true
        }

        override def beforeContainersStop(
            containers: containerDef.Container
        ): Unit = {
          beforeStopCalled = true
        }

        def is = s2"test"
      }

      val suite = new TestSuite()
      suite.beforeAll
      suite.afterAll

      afterStartCalled must beTrue
      beforeStopCalled must beTrue
      verify(container).start()
      verify(container).stop()
      ok
    }

    "handle TestLifecycleAware containers" in {
      val container = mock[SampleJavaContainer]

      class TestSuite
          extends org.specs2.Specification
          with TestContainerForAll
          with org.specs2.specification.BeforeAll
          with org.specs2.specification.AfterAll {

        override val containerDef = SampleContainer.Def(container)
        def is = s2"test"

        def simulateTest(): Unit = {
          beforeAll
          // Simulate test execution by calling beforeTest/afterTest manually
          startedContainers.foreach { c =>
            beforeTest(c)
            afterTest(c, None)
          }
          afterAll
        }
      }

      val suite = new TestSuite()
      suite.simulateTest()

      verify(container).start()
      verify(container).beforeTest(any())
      verify(container).afterTest(
        any(),
        ArgumentMatchers.eq(Optional.empty[Throwable]())
      )
      verify(container).stop()
      ok
    }
  }
}
