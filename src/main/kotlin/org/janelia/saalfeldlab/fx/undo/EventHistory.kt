package org.janelia.saalfeldlab.fx.undo

import javafx.beans.InvalidationListener
import javafx.beans.binding.BooleanBinding
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.value.ObservableIntegerValue
import javafx.collections.ObservableList
import javafx.util.Pair
import org.janelia.saalfeldlab.fx.extensions.nonnull

/**
 * A linear history of toggleable events: applied up to a cursor, undone after it.
 * The cursor is derived from the events to keep apply/undo/redo/delete/add events in sync.
 *
 * @param events in order, and an associated booleanProperty mapping whether that are currently applied
 */
class EventHistory<T>(val events: ObservableList<Pair<T, BooleanProperty>>) {

	private val _currentIndexProperty = ReadOnlyIntegerWrapper(-1)
    private var currentIndex by _currentIndexProperty.nonnull()

	private val sizeProperty = SimpleIntegerProperty(0)

	private val appliedEventListener = InvalidationListener { updateCurrentIndex() }

	private var appliedEventProperties = listOf<BooleanProperty>()

	/**
	 * Index of the most recently applied event, or -1 if none are applied.
	 */
	val currentIndexProperty: ObservableIntegerValue = _currentIndexProperty.readOnlyProperty

	val canUndo: BooleanBinding = _currentIndexProperty.greaterThanOrEqualTo(0)

	val canRedo: BooleanBinding = _currentIndexProperty.add(1).lessThan(sizeProperty)

	init {
		events.addListener(InvalidationListener { observeEvents() })
		observeEvents()
	}

	/**
	 * Undo the most recent event.
	 */
	fun undo() {
		if (canUndo.get())
			events[currentIndex].value.set(false)
	}

	/**
	 * Reapply the event after the cursor.
	 */
	fun redo() {
		if (canRedo.get())
			events[currentIndex + 1].value.set(true)
	}

	private fun observeEvents() {
		appliedEventProperties.forEach { it.removeListener(appliedEventListener) }
		appliedEventProperties = events.map { it.value }
		appliedEventProperties.forEach { it.addListener(appliedEventListener) }
		sizeProperty.set(events.size)
		updateCurrentIndex()
	}

	private fun updateCurrentIndex() {
        currentIndex = events.indexOfLast { it.value.get() }
    }
}
