package com.dimafeng.testcontainers.specs2

import org.mockito.{Mockito, MockitoAnnotations}
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterEach

import scala.reflect.ClassTag

abstract class BaseSpec[T: ClassTag] extends Specification with BeforeAfterEach {

  def mock[T <: AnyRef](implicit manifest: Manifest[T]): T = Mockito.mock(manifest.runtimeClass.asInstanceOf[Class[T]])

  override def before: Any = MockitoAnnotations.initMocks(this)
  override def after: Any = ()
}