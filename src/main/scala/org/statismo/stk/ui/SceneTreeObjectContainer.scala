package org.statismo.stk.ui

import scala.swing.Reactor
import scala.collection.immutable.IndexedSeq
import scala.collection.immutable

object MutableObjectContainer {

  import scala.language.implicitConversions

  implicit def containerToChildrenSeq[Child <: AnyRef](container: MutableObjectContainer[Child]): immutable.Seq[Child] = container.children.to[immutable.Seq]
}

trait MutableObjectContainer[Child <: AnyRef] extends Reactor {
  protected var _children: IndexedSeq[Child] = Nil.toIndexedSeq

  protected[ui] def children: Seq[Child] = _children

  def add(child: Child): Unit = this.synchronized {
    child match {
      case removeable: Removeable =>
        listenTo(removeable)
      case _ =>
    }
    _children = Seq(_children, Seq(child)).flatten.toIndexedSeq
  }

  reactions += {
    case Removeable.Removed(c) =>
      val child = c.asInstanceOf[Child]
      remove(child, silent = true)
  }

  def removeAll() = this.synchronized {
    val copy = _children.map({
      c => c
    })
    copy.foreach {
      c =>
      //println(s"removing $c")
        remove(c)
    }
  }

  protected def remove(child: Child, silent: Boolean): Boolean = this.synchronized {
    if (!silent && child.isInstanceOf[Removeable]) {
      // this will publish a message which is handled in the reactions
      child.asInstanceOf[Removeable].remove()
      true
    } else {
      val before = _children.length
      val toRemove = _children filter (_ eq child)
      toRemove foreach {
        case r: Removeable => deafTo(r)
      }
      _children = _children.diff(toRemove)
      val after = _children.length
      before != after
    }
  }

  protected final def remove(child: Child): Boolean = {
    remove(child, silent = false)
  }

}

trait SceneTreeObjectContainer[Child <: SceneTreeObject] extends MutableObjectContainer[Child] {
  //  override def children = super.children // required to prevent type conflict

  protected def publisher: SceneTreeObject

  override def add(child: Child): Unit = {
    super.add(child)
    publisher.publishEdt(SceneTreeObject.ChildrenChanged(publisher))
  }

  protected override def remove(child: Child, silent: Boolean): Boolean = {
    val changed = super.remove(child, silent)
    if (changed) {
      publisher.publishEdt(SceneTreeObject.ChildrenChanged(publisher))
    }
    changed
  }
}

trait StandaloneSceneTreeObjectContainer[Child <: SceneTreeObject] extends SceneTreeObject with SceneTreeObjectContainer[Child] {
  protected[ui] override def children = super.children

  // required to prevent type conflict
  protected override lazy val publisher = this
}

