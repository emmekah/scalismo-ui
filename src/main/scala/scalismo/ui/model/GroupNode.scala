package scalismo.ui.model

import scalismo.statisticalmodel.StatisticalMeshModel
import scalismo.ui.model.capabilities.{ Removeable, Renameable }

class GroupsNode(override val parent: Scene) extends SceneNodeCollection[GroupNode] {
  override val name = "Groups"

  def add(name: String, ghost: Boolean = false): GroupNode = {
    val node = new GroupNode(this, name, ghost)
    add(node)
    node
  }

  // the groups node is always collapsed in the view.
  override def isViewCollapsed: Boolean = true
}

class GroupNode(override val parent: GroupsNode, initialName: String, val isGhost: Boolean) extends SceneNode with Renameable with Removeable {
  name = initialName

  val transformations = new TransformationsNode(this)
  val landmarks = new LandmarksNode(this)
  val triangleMeshes = new TriangleMeshesNode(this)
  val scalarMeshFields = new ScalarMeshFieldsNode(this)
  val vectorFields = new VectorFieldsNode(this)
  val pointClouds = new PointCloudsNode(this)
  val images = new ImagesNode(this)

  override val children: List[SceneNode] = List(transformations, landmarks, triangleMeshes, scalarMeshFields, pointClouds, images, vectorFields)

  // this is a convenience method to add a statistical model as a (gp, mesh) combination.
  def addStatisticalMeshModel(model: StatisticalMeshModel, initialName: String): Unit = {
    triangleMeshes.add(model.referenceMesh, initialName)
    transformations.add(DiscreteLowRankGpPointTransformation(model.gp), initialName)

  }

  override def remove(): Unit = parent.remove(this)
}

