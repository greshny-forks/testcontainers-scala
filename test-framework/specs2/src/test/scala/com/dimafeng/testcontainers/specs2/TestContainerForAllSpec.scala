package com.dimafeng.testcontainers.specs2

import java.util.Optional

import com.dimafeng.testcontainers.lifecycle.and
import com.dimafeng.testcontainers.{BaseSpec, SampleContainer, SampleJavaContainer}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{times, verify}
import org.mockito.{ArgumentCaptor, ArgumentMatchers, Mockito}
import org.specs2.mutable.Specification

class TestContainerForAllSpec extends BaseSpec[TestContainerForAll] {

  import TestContainerForAllSpec._

  "TestContainerForAll" should {
    "call all appropriate methods of the containers" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]

      new TestSpec({
        assert(1 == 1)
      }, container1, container2).run(None, Reporter())

      verify(container1).beforeTest(any())
      verify(container1).start()
      verify(container1).afterTest(any(), ArgumentMatchers.eq(Optional.empty()))
      verify(container1).stop()

      verify(container2).beforeTest(any())
      verify(container2).start()
      verify(container2).afterTest(any(), ArgumentMatchers.eq(Optional.empty()))
      verify(container2).stop()
    }

    "call all appropriate methods of the containers if assertion fails" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]

      var err: Throwable = null

      try {
        new TestSpec({
          assert(1 == 2)
        }, container1, container2).run(None, Reporter())
      } catch {
        case e: AssertionError => err = e
      }

      assert(err != null)

      val captor1 = ArgumentCaptor.forClass[Optional[Throwable], Optional[Throwable]](classOf[Optional[Throwable]])
      verify(container1).beforeTest(any())
      verify(container1).start()
      verify(container1).afterTest(any(), captor1.capture())
      assert(captor1.getValue.isPresent)
      verify(container1).stop()

      val captor2 = ArgumentCaptor.forClass[Optional[Throwable], Optional[Throwable]](classOf[Optional[Throwable]])
      verify(container2).beforeTest(any())
      verify(container2).start()
      verify(container2).afterTest(any(), captor2.capture())
      assert(captor2.getValue.isPresent)
      verify(container2).stop()
    }

    "start and stop containers before and after all test cases" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]

      new MultipleTestsSpec({
        assert(1 == 1)
      }, container1, container2).run(None, Reporter())

      verify(container1, times(1)).beforeTest(any())
      verify(container1, times(1)).start()
      verify(container1, times(1)).afterTest(any(), any())
      verify(container1, times(1)).stop()

      verify(container2, times(1)).beforeTest(any())
      verify(container2, times(1)).start()
      verify(container2, times(1)).afterTest(any(), any())
      verify(container2, times(1)).stop()
    }

    "call afterContainersStart() and beforeContainersStop()" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]

      // Mockito somehow messed up internal state, so we can't use `spy` here.
      @volatile var afterStartCalled = false
      @volatile var beforeStopCalled = false

      val spec = new MultipleTestsSpec({
        assert(1 == 1)
      }, container1, container2) {
        override def afterContainersStart(containers: Containers): Unit = {
          super.afterContainersStart(containers)
          afterStartCalled = true
        }

        override def beforeContainersStop(containers: Containers): Unit = {
          super.beforeContainersStop(containers)
          beforeStopCalled = true
        }
      }

      spec.run(None, Reporter())

      assert(afterStartCalled && beforeStopCalled)
    }

    "call beforeContainersStop() and stop container if error thrown in afterContainersStart()" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]

      @volatile var afterStartCalled = false
      @volatile var beforeStopCalled = false

      val spec = new MultipleTestsSpec({
        assert(1 == 1)
      }, container1, container2) {
        override def afterContainersStart(containers: Containers): Unit = {
          afterStartCalled = true
          throw new RuntimeException("Test")
        }

        override def beforeContainersStop(containers: Containers): Unit = {
          super.beforeContainersStop(containers)
          beforeStopCalled = true
        }
      }

      intercept[RuntimeException] {
        spec.run(None, Reporter())
      }

      verify(container1, times(0)).beforeTest(any())
      verify(container1).start()
      verify(container1, times(0)).afterTest(any(), any())
      verify(container1).stop()

      verify(container2, times(0)).beforeTest(any())
      verify(container2).start()
      verify(container2, times(0)).afterTest(any(), any())
      verify(container2).stop()

      assert(afterStartCalled && beforeStopCalled)
    }

    "not start container if all tests are ignored" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]

      @volatile var called = false

      new TestSpecWithAllIgnored({
        called = true
      }, container1, container2) {}.run(None, Reporter())

      verify(container1, Mockito.never()).start()
      verify(container2, Mockito.never()).start()
      assert(called === false)
    }
  }
}

object TestContainerForAllSpec {

  protected abstract class AbstractTestSpec(
    testBody: => Unit,
    container1: SampleJavaContainer,
    container2: SampleJavaContainer
  ) extends Specification with TestContainerForAll {
    override type Containers = SampleContainer and SampleContainer

    override def startContainers(): Containers = {
      val c1 = SampleContainer.Def(container1).start()
      val c2 = SampleContainer.Def(container2).start()
      c1 and c2
    }
  }

  protected class TestSpec(testBody: => Unit, container1: SampleJavaContainer, container2: SampleJavaContainer)
    extends AbstractTestSpec(testBody, container1, container2) {

    "test" should {
      withContainers { case c1 and c2 =>
        assert(
          c1.underlyingUnsafeContainer === container1 &&
            c2.underlyingUnsafeContainer === container2
        )
        testBody
      }
    }
  }

  protected class MultipleTestsSpec(testBody: => Unit, container1: SampleJavaContainer, container2: SampleJavaContainer)
    extends AbstractTestSpec(testBody, container1, container2) {

    "test1" should {
      withContainers { case c1 and c2 =>
        assert(
          c1.underlyingUnsafeContainer === container1 &&
            c2.underlyingUnsafeContainer === container2
        )
        testBody
      }
    }

    "test2" should {
      withContainers { case c1 and c2 =>
        assert(
          c1.underlyingUnsafeContainer === container1 &&
            c2.underlyingUnsafeContainer === container2
        )
        testBody
      }
    }
  }

  protected class TestSpecWithAllIgnored(testBody: => Unit, container1: SampleJavaContainer, container2: SampleJavaContainer)
    extends AbstractTestSpec(testBody, container1, container2) {

    "test" ignore {
      testBody
    }
  }
}
