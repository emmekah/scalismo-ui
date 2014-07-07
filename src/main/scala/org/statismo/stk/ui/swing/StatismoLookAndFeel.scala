package org.statismo.stk.ui.swing

import scala.swing.{SimpleSwingApplication, Swing}
import javax.swing.{ToolTipManager, SwingUtilities, UIManager}
import org.statismo.stk.ui.UiFramework

object StatismoLookAndFeel {
  def initializeWith(lookAndFeelClassName: String): Unit = {
    def doit() = {
      UiFramework.instance = new SwingUiFramework
      UIManager.setLookAndFeel(lookAndFeelClassName)
      val laf = UIManager.getLookAndFeel
      if (laf.getClass.getSimpleName.startsWith("Nimbus")) {
        val defaults = laf.getDefaults
        defaults.put("Tree.drawHorizontalLines", true)
        defaults.put("Tree.drawVerticalLines", true)
      }
      UIManager.put("FileChooser.readOnly", true)
      ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false)
    }
    if (SwingUtilities.isEventDispatchThread) {
      doit()
    } else {
      Swing.onEDTWait(doit())
    }
  }
}

trait StatismoLookAndFeel extends SimpleSwingApplication {
  override def main(args: Array[String]) = {
    StatismoLookAndFeel.initializeWith(defaultLookAndFeelClassName)
    super.main(args)
  }

  def defaultLookAndFeelClassName: String = {
    val nimbus = UIManager.getInstalledLookAndFeels.filter(_.getName.equalsIgnoreCase("nimbus")).map(i => i.getClassName)
    if (!nimbus.isEmpty) nimbus.head else UIManager.getSystemLookAndFeelClassName
  }

  override def startup(args: Array[String]) {
    super.startup(args)
    SwingUtilities.updateComponentTreeUI(top.peer)
  }
}
