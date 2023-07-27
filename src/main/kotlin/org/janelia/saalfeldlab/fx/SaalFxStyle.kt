package org.janelia.saalfeldlab.fx

import javafx.scene.Parent
import javafx.scene.Scene
import org.janelia.saalfeldlab.fx.ui.MatchSelection
import org.janelia.saalfeldlab.fx.ui.NumberField

object SaalFxStyle {
	fun registerStylesheets(styleable : Scene) {
		NumberField.registStyleSheet(styleable)
		MatchSelection.registStyleSheet(styleable)
	}
	fun registerStylesheets(styleable : Parent) {
		NumberField.registStyleSheet(styleable)
		MatchSelection.registStyleSheet(styleable)
	}
}