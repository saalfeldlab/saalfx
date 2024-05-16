package org.janelia.saalfeldlab.fx.extensions

import javafx.beans.binding.ObjectBinding
import javafx.scene.control.ListCell
import javafx.scene.control.ListView

fun ListView<*>.bindHeightToItemSize() {

	/* This is a magic number, representing the default max row number, so we don't
	* expand larger than the current default */
	val defaultMaxRows = 18
	val cellHeightFallBack = 24.0
	val prefHeightBinding: ObjectBinding<Double> = items.createObservableBinding {
		val cellHeight = (lookup(".list-cell") as? ListCell<*>)?.height?.let { h -> if (h == 0.0) cellHeightFallBack else h } ?: cellHeightFallBack
		val borderPadding = padding.top + padding.bottom
		borderPadding + cellHeight * it.size.coerceAtMost(defaultMaxRows)
	}
	prefHeightProperty().bind(prefHeightBinding)
}