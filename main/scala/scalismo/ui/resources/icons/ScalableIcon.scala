package scalismo.ui.resources.icons

import javax.swing.Icon

import scalismo.ui.view.{ Constants, ScalableUI }

trait ScalableIcon extends Icon {
  def standardSized(): Icon = {
    val scaledSize = ScalableUI.scale(Constants.StandardUnscaledIconSize)
    resize(scaledSize, scaledSize)
  }

  def resize(width: Int, height: Int) = {
    ScalableUI.resizeIcon(this, width, height)
  }
}
