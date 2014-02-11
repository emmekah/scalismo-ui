package org.statismo.stk.ui.swing

import scala.swing.ToggleButton
import org.statismo.stk.ui.Workspace
import scala.swing.event.ButtonClicked

class ToggleLandmarkPickingButton(val workspace: Workspace) extends ToggleButton("LM") {
	reactions += {
	  case ButtonClicked(s) => {
	    workspace.landmarkClickMode = peer.isSelected
	  }
	}
}