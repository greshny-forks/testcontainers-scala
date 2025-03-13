package com.dimafeng.testcontainers.specs2

import java.util.Optional
import com.dimafeng.testcontainers.{Container, BaseSpec => CoreBaseSpec}
import com.dimafeng.testcontainers.lifecycle.TestLifecycleAware
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{times, verify, never}
import org.mockito.{ArgumentCaptor, ArgumentMatchers, Mockito}
import org.specs2.Specification
import org.specs2.specification.core.SpecStructure
import org.testcontainers.lifecycle.{
  TestDescription,
  TestLifecycleAware => JavaTestLifecycleAware
}

class ContainerSpec extends BaseSpec[TestContainerForEach] {

  "Container" should {
    "call all appropriate methods of the container" in {
      val container = mock[SampleJavaContainer]

      val spec = new TestSpec(true, new SampleContainer(container))
      runSpec(spec)

      verify(container).beforeTest(any())
      verify(container).start()
      verify(container).afterTest(any(), ArgumentMatchers.eq(Optional.empty()))
      verify(container).stop()
      ok
    }

    "call all appropriate methods of the container if assertion fails" in {
      val container = mock[SampleJavaContainer]

      val spec = new TestSpec(false, new SampleContainer(container))
      runSpec(spec)

      val captor =
        ArgumentCaptor.forClass[Optional[Throwable], Optional[Throwable]](
          classOf[Optional[Throwable]]
        )
      verify(container).beforeTest(any())
      verify(container).start()
      verify(container).afterTest(any(), captor.capture())
      captor.getValue.isPresent must beTrue
      verify(container).stop()
      ok
    }

    "start and stop container only once for ForAll" in {
      val container = mock[SampleJavaContainer]

      val spec = new MultipleTestsSpec(true, new SampleContainer(container))
      runSpec(spec)

      verify(container, times(2)).beforeTest(any())
      verify(container).start()
      verify(container, times(2)).afterTest(any(), any())
      verify(container).stop()
      ok
    }

    "call afterContainersStart() and beforeContainersStop()" in {
      val container = mock[SampleJavaContainer]

      // ForEach
      val specForEach =
        Mockito.spy(new TestSpec(true, new SampleContainer(container)))
      runSpec(specForEach)

      verify(specForEach).afterContainersStart(any())
      verify(specForEach).beforeContainersStop(any())

      // ForAll
      val specForAll =
        Mockito.spy(new MultipleTestsSpec(true, new SampleContainer(container)))
      runSpec(specForAll)

      verify(specForAll).afterContainersStart(any())
      verify(specForAll).beforeContainersStop(any())
      ok
    }

    "call beforeContainersStop() and stop container if error thrown in afterContainersStart()" in {
      val container = mock[SampleJavaContainer]

      // ForEach
      val specForEach = Mockito.spy(
        new TestSpecWithFailedAfterStart(true, new SampleContainer(container))
      )
      runSpec(specForEach) must throwA[RuntimeException]

      verify(container, times(0)).beforeTest(any())
      verify(container).start()
      verify(specForEach).afterContainersStart(any())
      verify(container, times(0)).afterTest(any(), any())
      verify(specForEach).beforeContainersStop(any())
      verify(container).stop()
      ok
    }

    "not start container if all tests are ignored" in {
      val container = mock[SampleJavaContainer]
      val specForAll = Mockito.spy(
        new TestSpecWithAllIgnored(true, new SampleContainer(container))
      )
      runSpec(specForAll)

      verify(container, never()).start()
      ok
    }

    "work with `configure` method" in {
      val innerContainer = new SampleJavaContainer
      val container = new SampleContainer(innerContainer)
        .configure { c => c.withWorkingDirectory("123"); () }

      container.workingDirectory === "123"
    }
  }

  private def runSpec(spec: Specification): Unit = {
    // Execute the specification
    try {
      val result = spec.is
      // This will trigger the lifecycle methods
    } catch {
      case e: Exception => throw e
    }
  }

  // Test spec classes

  protected class TestSpec(assertion: Boolean, _container: Container)
      extends Specification
      with TestContainerForEach {
    override val containerDef =
      SampleContainer.Def(_container.asInstanceOf[SampleContainer].container)

    def is = s2"""
      Test should
        pass assertion $testAssertion
    """

    def testAssertion = {
      withContainers { _ =>
        assertion must beTrue
      }
    }
  }

  protected class TestSpecWithFailedAfterStart(
      assertion: Boolean,
      _container: Container
  ) extends Specification
      with TestContainerForEach {
    override val containerDef =
      SampleContainer.Def(_container.asInstanceOf[SampleContainer].container)

    override def afterContainersStart(
        containers: containerDef.Container
    ): Unit = {
      throw new RuntimeException("something wrong in afterContainersStart()")
    }

    def is = s2"""
      Test should
        pass assertion $testAssertion
    """

    def testAssertion = {
      withContainers { _ =>
        assertion must beTrue
      }
    }
  }

  protected class MultipleTestsSpec(assertion: Boolean, _container: Container)
      extends Specification
      with TestContainerForAll
      with org.specs2.specification.BeforeAll
      with org.specs2.specification.AfterAll {
    override val containerDef =
      SampleContainer.Def(_container.asInstanceOf[SampleContainer].container)

    def is = s2"""
      Multiple tests should
        pass test1 $test1
        pass test2 $test2
    """

    def test1 = {
      withContainers { _ =>
        assertion must beTrue
      }
    }

    def test2 = {
      withContainers { _ =>
        assertion must beTrue
      }
    }
  }

  protected class TestSpecWithAllIgnored(
      assertion: Boolean,
      _container: Container
  ) extends Specification
      with TestContainerForAll
      with org.specs2.specification.BeforeAll
      with org.specs2.specification.AfterAll {
    override val containerDef =
      SampleContainer.Def(_container.asInstanceOf[SampleContainer].container)

    def is = s2"""
      Ignored tests should
        be ignored $test1
    """

    def test1 = skipped("test ignored")
  }
}
