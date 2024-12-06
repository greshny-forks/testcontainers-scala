package com.dimafeng.testcontainers

import org.testcontainers.utility.DockerImageName
import org.opensearch.testcontainers.{
  OpensearchContainer => JavaOpensearchContainer
}
class OpensearchContainer(dockerImageName: DockerImageName)
    extends SingleContainer[JavaOpensearchContainer[_]] {
  override val container: JavaOpensearchContainer[_] =
    new JavaOpensearchContainer[JavaOpensearchContainer[_]](
      dockerImageName
    )
}

object OpensearchContainer {
  val defaultImage: String = "opensearchproject/opensearch"
  val defaultTag: String = "2.11.0"
  val defaultDockerImageName: String = s"$defaultImage:$defaultTag"

  case class Def(
      dockerImageName: DockerImageName =
        DockerImageName.parse(defaultDockerImageName)
  ) extends ContainerDef {
    override type Container = OpensearchContainer

    override protected def createContainer(): OpensearchContainer =
      new OpensearchContainer(dockerImageName)
  }

  def apply(
      dockerImageNameOverride: DockerImageName = null
  ): OpensearchContainer =
    new OpensearchContainer(
      Option(dockerImageNameOverride).getOrElse(
        DockerImageName.parse(defaultDockerImageName)
      )
    )
}
