package org.janelia.saalfeldlab.fx.undo

import javafx.beans.property.BooleanProperty
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableIntegerValue
import javafx.collections.ObservableList
import javafx.util.Pair

/**
 * A history of toggleable events: applied up to [currentIndexProperty], undone after it.
 */
interface UndoableEvents<T> {

	/**
	 * The events in order, and whether each is currently applied.
	 */
	val events: ObservableList<Pair<T, BooleanProperty>>

	/**
	 * Index of the most recently applied event, or -1 if none are applied.
	 */
	val currentIndexProperty: ObservableIntegerValue

	val canUndo: ObservableBooleanValue

	val canRedo: ObservableBooleanValue

	/**
	 * Deactivate the most recent applied event.
	 */
	fun undo() {
		if (canUndo.get())
			events[currentIndexProperty.get()].value.set(false)
	}

	/**
	 * Reapply the event after [currentIndexProperty].
	 */
	fun redo() {
		if (canRedo.get())
			events[currentIndexProperty.get() + 1].value.set(true)
	}

}
