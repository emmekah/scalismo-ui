package org.statismo.stk.ui.swing

import org.statismo.stk.ui.SceneTreeObject
import org.statismo.stk.ui.Colorable
import scala.swing.Slider
import scala.swing.BorderPanel
import scala.swing.event.ValueChanged
import scala.swing.Component
import javax.swing.JColorChooser
import scala.swing.Swing
import java.awt.Color
import scala.swing.event.Event
import javax.swing.colorchooser.DefaultSwatchChooserPanel
import org.statismo.stk.ui.swing.util.ColorPickerPanel
import javax.swing.border.TitledBorder
import scala.swing.Label
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel
import java.awt.Graphics

class ColorablePanel extends BorderPanel with SceneObjectPropertyPanel {
  val description = "Appearance"
  private var target: Option[Colorable] = None

  private val opacitySlider = new Slider() {
    min = 0
    max = 100
    value = 100
  }

  case class ColorChosen(color: Color) extends Event

  val colorDisplayer = new Component {
    override lazy val peer = new JPanel {
      override def paintComponent(g: Graphics) {
        g.setColor(Color.GRAY); // this approximates what you'll see when rendering in a black panel
        g.fillRect(0, 0, getSize().width, getSize().height);
        g.setColor(getBackground());
        g.fillRect(0, 0, getSize().width, getSize().height);
      }
    }
    def setColor(color: Color, opacity: Double) = {
      val comp = color.getColorComponents(null)
      val c = new Color(comp(0), comp(1), comp(2), opacity.toFloat)
      peer.setBackground(c);
      peer.setForeground(c);
      revalidate
      repaint
    }
    peer.setOpaque(false)
    peer.setPreferredSize(new Dimension(20, 20))
  }

  val colorChooser = new Component with ColorPickerPanel.ColorListener {
    override lazy val peer = new ColorPickerPanel(this)

    def colorChanged(c: Color) = {
      publish(ColorChosen(c))
    }
    border = new javax.swing.border.EmptyBorder(10, 0, 0, 0)

  }

  {
    val northedPanel = new BorderPanel {
      val opacityPanel = new BorderPanel {
        layout(opacitySlider) = BorderPanel.Position.Center
        layout(new Label("0%")) = BorderPanel.Position.West
        layout(new Label("100%")) = BorderPanel.Position.East
        border = new TitledBorder(null, "Opacity", TitledBorder.LEADING, 0, null, null)
      }
      val colorPanel = new BorderPanel {
        border = new TitledBorder(null, "Color", TitledBorder.LEADING, 0, null, null)
        layout(colorChooser) = BorderPanel.Position.Center
        layout(colorDisplayer) = BorderPanel.Position.North
        //layoutManager.setHgap(10);
      }
      layout(colorPanel) = BorderPanel.Position.Center
      layout(opacityPanel) = BorderPanel.Position.South
    }
    layout(northedPanel) = BorderPanel.Position.North
  }
  listenToOwnEvents()

  reactions += {
    case Colorable.AppearanceChanged(t) => updateUi()
    case ColorChosen(c) => {
      if (target.isDefined) {
        target.get.color = c
        updateColorDisplayer
      }
    }
    case ValueChanged(s) => {
      if (target.isDefined) {
        target.get.opacity = (s.asInstanceOf[Slider].value.toDouble / 100.0)
        updateColorDisplayer
      }
    }
  }
  def listenToOwnEvents() = {
    listenTo(opacitySlider, colorChooser)
  }

  def deafToOwnEvents() = {
    deafTo(opacitySlider, colorChooser)
  }

  def cleanup() = {
    if (target.isDefined) {
      deafTo(target.get)
      target = None
    }
  }

  def setObject(obj: Option[SceneTreeObject]): Boolean = {
      cleanup()
    if (obj.isDefined && obj.get.isInstanceOf[Colorable]) {
      target = Some(obj.get.asInstanceOf[Colorable])
      updateUi()
      listenTo(target.get)
      true
    } else {
      false
    }
  }

  def updateUi() = {
    if (target.isDefined) {
      deafToOwnEvents()
      opacitySlider.value = (target.get.opacity * 100).toInt
      updateColorDisplayer
      listenToOwnEvents()
    }
  }

  def updateColorDisplayer() {
    colorDisplayer.setColor(target.get.color, target.get.opacity)
  }
}