package scalismo.ui.vtk

import scalismo.ui.MeshView.TriangleMeshRenderable
import scalismo.ui.PointCloudView.PointCloudRenderable3D
import scalismo.ui.ScalarFieldView.ScalarFieldRenderable
import scalismo.ui.ScalarMeshFieldView.ScalarMeshFieldRenderable
import scalismo.ui.VectorFieldView.VectorFieldRenderable3D
import scalismo.ui.visualization.{ EllipsoidLike, Renderable }
import scalismo.ui.{ BoundingBox, Image3DView, Scene }
import vtk.vtkActor

import scala.util.Try

object RenderableActor {
  type RenderableToActor = (Renderable, VtkViewport) => Option[RenderableActor]

  def apply(renderable: Renderable)(implicit vtkViewport: VtkViewport): Option[RenderableActor] = {
    // first, use the function in case the user overwrote something
    val raOption = renderableToActorFunction(renderable, vtkViewport)
    if (raOption.isDefined) raOption
    else {
      renderable match {
        case r: VtkRenderable => Some(r.getVtkActor)
        case _ =>
          System.err.println("RenderableActor: Don't know what to do with " + renderable.getClass)
          None
      }
    }
  }

  val DefaultRenderableToActorFunction: RenderableToActor = {
    case (renderable, vtkViewport) =>
      implicit val _vtkViewport = vtkViewport
      renderable match {
        case bb3d: Scene.SlicingPosition.BoundingBoxRenderable3D => Some(new BoundingBoxActor3D(bb3d))
        case sp3d: Scene.SlicingPosition.SlicingPlaneRenderable3D => Some(new SlicingPlaneActor3D(sp3d))
        case sp2d: Scene.SlicingPosition.SlicingPlaneRenderable2D => Some(new SlicingPlaneActor2D(sp2d))

        case img3d: Image3DView.Renderable3D[_] => img3d.imageOrNone.map { source => new ImageActor3D(source) }
        case img2d: Image3DView.Renderable2D[_] => img2d.imageOrNone.map { source => ImageActor2D(source) }

        case ell: EllipsoidLike => Some(EllipsoidActor(vtkViewport, ell))
        case mesh: TriangleMeshRenderable => Some(MeshActor(vtkViewport, mesh))
        case smesh: ScalarMeshFieldRenderable => Some(MeshActor(vtkViewport, smesh))
        case sf: ScalarFieldRenderable => Some(ScalarFieldActor(vtkViewport, sf))

        // these are not completely finished yet (2D implementations are missing)
        case pc3d: PointCloudRenderable3D => Some(new PointCloudActor3D(pc3d))
        case vf3d: VectorFieldRenderable3D => Some(new VectorFieldActor3D(vf3d))

        case _ => None
      }
  }

  private var _renderableToActorFunction = DefaultRenderableToActorFunction

  def renderableToActorFunction = this.synchronized(_renderableToActorFunction)

  def renderableToActorFunction(nf: RenderableToActor) = this.synchronized {
    _renderableToActorFunction = nf
  }
}

trait RenderableActor extends VtkContext {
  def vtkActors: Seq[vtkActor]

  def currentBoundingBox: BoundingBox = {
    vtkActors.map { a => VtkUtils.bounds2BoundingBox(a.GetBounds()) }.fold(BoundingBox.None)((bb1, bb2) => bb1.union(bb2))
  }

  def onDestroy(): Unit = {
  }
}