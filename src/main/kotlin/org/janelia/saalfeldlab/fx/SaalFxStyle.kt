package org.janelia.saalfeldlab.fx

import javafx.scene.Parent
import javafx.scene.Scene
import org.janelia.saalfeldlab.fx.ui.MatchSelection
import org.janelia.saalfeldlab.fx.ui.NumberField

object SaalFxStyle {
	fun registerStylesheets(styleable : Scene) {
		NumberField.registerStyleSheet(styleable)
		MatchSelection.registerStyleSheet(styleable)
	}
	fun registerStylesheets(styleable : Parent) {
		NumberField.registerStyleSheet(styleable)
		MatchSelection.registerStyleSheet(styleable)
	}
}