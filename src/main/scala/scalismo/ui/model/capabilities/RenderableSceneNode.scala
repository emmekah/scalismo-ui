/*
 * Copyright (C) 2016  University of Basel, Graphics and Vision Research Group 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package scalismo.ui.model.capabilities

import scalismo.ui.model.{ Renderable, SceneNode }

/**
 * A renderable node is -- by design -- a leaf node,
 * i.e., it has no further children.
 *
 */
trait RenderableSceneNode extends SceneNode with Renderable {

  final override def renderables: List[RenderableSceneNode] = List(this)

  final override def children: List[SceneNode] = Nil
}

object RenderableSceneNode {

  import scala.language.implicitConversions

}