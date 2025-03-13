package com.dimafeng.testcontainers.specs2

import java.util.Optional
import com.dimafeng.testcontainers.{
  Container,
  MultipleContainers,
  SingleContainer
}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.verify
import org.specs2.Specification
import org.specs2.specification.core.SpecStructure
import org.scalatestplus.mockito.MockitoSugar

class MultipleContainersSpec extends BaseSpec[TestContainerForEach] {

  "MultipleContainers" should {
    "call all expected methods of the multiple containers" in {
      val container1 = mock[SampleJavaContainer]
      val container2 = mock[SampleJavaContainer]

      val containers = MultipleContainers(
        new SampleContainer(container1),
        new SampleContainer(container2)
      )

      // Directly test the container lifecycle without specs2 integration
      containers.start()
      containers.stop()

      verify(container1).start()
      verify(container1).stop()
      verify(container2).start()
      verify(container2).stop()
      ok
    }

    "initialize containers lazily in MultipleContainers to let second container be depended on start data of the first one" in {
      lazy val container1 = new InitializableContainer("after start value")
      lazy val container2 = new InitializableContainer(container1.value)

      val containers = MultipleContainers(container1, container2)

      containers.start()

      container1.value === "after start value"
      container2.value === "after start value"

      containers.stop()
      ok
    }
  }

  class InitializableContainer(valueToBeSetAfterStart: String)
      extends SingleContainer[SampleJavaContainer]
      with MockitoSugar {
    override implicit val container: SampleJavaContainer =
      mock[SampleJavaContainer]
    var value: String = _

    override def start(): Unit = {
      value = valueToBeSetAfterStart
    }
  }

  class ExampleContainerWithVariable(val variable: String)
      extends SingleContainer[SampleJavaContainer]
      with MockitoSugar {
    override implicit val container: SampleJavaContainer =
      mock[SampleJavaContainer]
  }
}
