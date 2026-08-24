package org.janelia.saalfeldlab.fx.undo

import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableIntegerValue
import javafx.collections.ObservableList
import javafx.util.Pair
import javafx.util.Subscription
import org.janelia.saalfeldlab.fx.extensions.nonnull

/**
 * [UndoableEvents] backed by an observable list.
 *
 * undo/redo only update the boolean property on the given event.
 * The event list itself is only modified directly by [delete] or [deleteAll].
 *
 * Changes to the [events] list and boolean property should be observed to trigger desired behavior.
 *
 * @param events backing observable list.
 */
open class EventHistory<T>(override val events: ObservableList<Pair<T, BooleanProperty>>) : UndoableEvents<T> {

	private val _currentIndexProperty = ReadOnlyIntegerWrapper(-1)
	private var currentIndex by _currentIndexProperty.nonnull()

	private var appliedEventSubscription = Subscription.EMPTY

	override val currentIndexProperty: ObservableIntegerValue = _currentIndexProperty.readOnlyProperty

	override val canUndo: BooleanBinding = _currentIndexProperty.greaterThanOrEqualTo(0)

	override val canRedo: BooleanBinding = _currentIndexProperty.add(1).lessThan(Bindings.size(events))

	private val _hasDisabledEventsProperty = ReadOnlyBooleanWrapper(false)

	val hasDisabledEvents: ObservableBooleanValue = _hasDisabledEventsProperty.readOnlyProperty

	init {
		events.subscribe { update() }
		update()
	}

	/**
	 * Undo the event at [index] and remove it.
	 */
	open fun delete(index: Int) {
		events[index].value.set(false)
		events.removeAt(index)
	}

	/**
	 * Undo [entry] and remove it.
	 */
	open fun delete(entry: Pair<T, BooleanProperty>) {
		val index = events.indexOfFirst { it === entry }
		if (index >= 0)
			delete(index)
	}

	/**
	 * Undo the first occurrence of [event] and remove it.
	 */
	open fun delete(event: T) {
		val index = events.indexOfFirst { it.key == event }
		if (index >= 0)
			delete(index)
	}

	/**
	 * Bulk undo and remove operation.
	 */
	open fun deleteAll(entries: Collection<Pair<T, BooleanProperty>>) {
		val allEntries = entries.toSet()
		for (index in events.indices.reversed())
			if (events[index] in allEntries)
				delete(index)
	}

	/**
	 * Undo and remove all events.
	 */
	open fun deleteAll() = deleteAll(events.toList())

	/**
	 * Undo and remove every event that is currently disabled.
	 */
	open fun deleteDisabled() = deleteAll(events.filterNot { it.value.get() })

	private fun update() {
		appliedEventSubscription.unsubscribe()
		appliedEventSubscription = events
			.map { it.value.subscribe { _, _ -> updateFromEvents() } }
			.fold(Subscription.EMPTY) { combined, subscription -> combined.and(subscription) }
		updateFromEvents()
	}

	private fun updateFromEvents() {
		currentIndex = events.indexOfLast { it.value.get() }
		_hasDisabledEventsProperty.set(events.any { !it.value.get() })
	}
}
