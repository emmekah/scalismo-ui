package scalismo.ui.visualization

import scalismo.ui.{ EdtPublisher, Scene, SceneTreeObject, Viewport }

import scala.collection.{ immutable, mutable }
import scala.language.existentials
import scala.ref.WeakReference
import scala.swing.event.Event
import scala.util.{ Failure, Success, Try }

class Visualizations {
  private type ViewportOrClassName = Either[Viewport, String]

  private val perviewport = new mutable.WeakHashMap[ViewportOrClassName, PerViewport]

  private class PerViewport(val context: ViewportOrClassName) {
    private val mappings = new mutable.WeakHashMap[VisualizationProvider[_], Try[Visualization[_]]]

    def tryGet(key: VisualizationProvider[_]): Try[Visualization[_]] = {
      //FIXME too: this method is invoked far too often.
      //FIXME
      //System.gc()
      //println(new Date()+ " " +key.getClass + " " +key+" " + " map size = " + mappings.size)
      //mappings.keys.foreach { p => println(s"\t$p") }

      val value = mappings.getOrElseUpdate(key, {
        val existing: Try[Visualization[_]] = key match {
          case fac: VisualizationFactory[_] =>
            context match {
              case Left(viewport) => Visualizations.this.tryGet(key, viewport.getClass.getCanonicalName)
              case Right(vpClass) => Try {
                fac.instantiate(vpClass)
              }
            }
          case _ => tryGet(key.visualizationProvider)
        }
        existing match {
          case Success(ok) => Try.apply[Visualization[_]] { ok.derive() }
          case f @ Failure(e) => f
        }
      })
      value.asInstanceOf[Try[Visualization[_]]]
    }
  }

  private def tryGet(key: VisualizationProvider[_], context: ViewportOrClassName): Try[Visualization[_]] = {
    val delegate = perviewport.getOrElseUpdate(context, new PerViewport(context))
    delegate.tryGet(key)
  }

  def tryGet(key: VisualizationProvider[_], context: Viewport): Try[Visualization[_]] = tryGet(key, Left(context))

  def tryGet(key: VisualizationProvider[_], context: String): Try[Visualization[_]] = tryGet(key, Right(context))

  def getUnsafe[R <: Visualization[_]](key: VisualizationProvider[_], context: Viewport): R = tryGet(key, context).get.asInstanceOf[R]

  def getUnsafe[R <: Visualization[_]](key: VisualizationProvider[_], context: String): R = tryGet(key, context).get.asInstanceOf[R]

}

trait VisualizationFactory[A <: Visualizable[A]] extends VisualizationProvider[A] {
  protected[ui] override final val visualizationProvider = null

  override protected[ui] def visualizationFactory: VisualizationFactory[A] = this

  protected[ui] final def visualizationsFor(viewport: Viewport): Seq[Visualization[A]] = visualizationsFor(viewport.getClass.getCanonicalName)

  protected[ui] def visualizationsFor(viewportClassName: String): Seq[Visualization[A]]

  protected[ui] final def instantiate(viewportClassName: String): Visualization[A] = {
    visualizationsFor(viewportClassName).headOption match {
      case Some(v) => v
      case None => throw new RuntimeException(getClass + " did not provide any Visualization options for viewport class " + viewportClassName)
    }
  }
}

trait SimpleVisualizationFactory[A <: Visualizable[A]] extends VisualizationFactory[A] {
  protected var visualizations = new immutable.HashMap[String, Seq[Visualization[A]]]

  final override def visualizationsFor(viewportClassName: String): Seq[Visualization[A]] = visualizations.getOrElse(viewportClassName, Nil)
}

trait VisualizationProvider[A <: Visualizable[A]] {
  protected[ui] def visualizationProvider: VisualizationProvider[A]

  protected[ui] def visualizationFactory: VisualizationFactory[A] = visualizationProvider.visualizationFactory

  def visualizations(implicit scene: Scene): immutable.Map[Viewport, Visualization[A]] = {
    var map = new immutable.HashMap[Viewport, Visualization[A]]
    val viss = scene.visualizations
    scene.perspective.viewports.foreach {
      viewport =>
        viss.tryGet(this, viewport) match {
          case Success(vis: Visualization[A]) => map += Tuple2(viewport, vis)
          case _ => // do nothing
        }
    }
    map
  }
}

trait Visualizable[X <: Visualizable[X]] extends VisualizationProvider[X] {
  protected[ui] def isVisibleIn(viewport: Viewport): Boolean
}

trait VisualizableSceneTreeObject[X <: VisualizableSceneTreeObject[X]] extends SceneTreeObject with Visualizable[X] {
  protected[ui] override def isVisibleIn(viewport: Viewport): Boolean = visible(viewport)

}

trait Derivable[A <: AnyRef] {
  protected val self: A = this.asInstanceOf[A]
  private var _derived: immutable.Seq[WeakReference[A]] = Nil

  protected[visualization] def derived: immutable.Seq[A] = derivedInUse.map(r => r.get).filter(o => o != None).map(o => o.get)

  private def derivedInUse: immutable.Seq[WeakReference[A]] = {
    _derived = _derived.filter(w => w.get != None)
    _derived
  }

  final def derive(): A = {
    val child: A = createDerived()
    _derived = Seq(derivedInUse, Seq(new WeakReference[A](child))).flatten.to[immutable.Seq]
    child
  }

  protected def createDerived(): A
}

trait Visualization[A <: Visualizable[_]] extends Derivable[Visualization[A]] {
  private val mappings = new mutable.WeakHashMap[A, Seq[Renderable]]

  def description: String

  override final def toString = description

  final def apply(target: Visualizable[_]) = {
    val typed: A = target.asInstanceOf[A]
    mappings.getOrElseUpdate(typed, instantiateRenderables(typed))
  }

  protected def instantiateRenderables(source: A): Seq[Renderable]
}

final class NullVisualization[A <: Visualizable[_]] extends Visualization[A] {
  override protected def createDerived() = new NullVisualization[A]

  override protected def instantiateRenderables(source: A) = Nil

  override val description = "(invisible)"
}

// this object can be subscribed to to receive events when /any/ VisualizationProperty has changed,
// without having to subscribe to each property individually
object VisualizationProperty extends EdtPublisher {

  case class ValueChanged[V, C <: VisualizationProperty[V, C]](source: VisualizationProperty[V, C]) extends Event

  def publishValueChanged[V, C <: VisualizationProperty[V, C]](source: VisualizationProperty[V, C]) = {
    val event = ValueChanged(source)
    source.publishEdt(event)
    this.publishEdt(event)
  }
}

trait VisualizationProperty[V, C <: VisualizationProperty[V, C]] extends Derivable[C] with EdtPublisher {
  private var _value: Option[V] = None

  final def value: V = {
    _value.getOrElse(defaultValue)
  }

  final def value_=(newValue: V): Unit = {
    if (newValue != value) {
      _value = Some(newValue)
      derived.foreach(_.value = newValue)
      VisualizationProperty.publishValueChanged(this)
    }
  }

  def defaultValue: V

  final override protected def createDerived(): C = {
    val child = newInstance()
    child.value = this.value
    child
  }

  def newInstance(): C
}