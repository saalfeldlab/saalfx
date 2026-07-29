package org.janelia.saalfeldlab.fx.undo

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.util.Pair
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The undo/redo/delete logic, without any of the UI that triggers it.
 */
class EventHistoryTest {

	private fun event(title: String, isApplied: Boolean = true): Pair<String, BooleanProperty> =
		Pair(title, SimpleBooleanProperty(isApplied))

	private fun events(vararg isApplied: Boolean): ObservableList<Pair<String, BooleanProperty>> = FXCollections
		.observableArrayList(isApplied.mapIndexed { index, applied -> event("event $index", applied) })

	private val ObservableList<Pair<String, BooleanProperty>>.appliedStates
		get() = map { it.value.get() }

	@Test
	fun `undo unapplies the most recently applied event`() {
		val events = events(true, true, true)
		val history = EventHistory(events)

		history.undo()
		assertEquals(listOf(true, true, false), events.appliedStates)

		history.undo()
		assertEquals(listOf(true, false, false), events.appliedStates)
	}

	@Test
	fun `redo reapplies the event after the cursor`() {
		val events = events(true, false, false)
		val history = EventHistory(events)

		history.redo()
		assertEquals(listOf(true, true, false), events.appliedStates)

		history.redo()
		assertEquals(listOf(true, true, true), events.appliedStates)
	}

	@Test
	fun `undo and redo stop at the ends`() {
		val allUndone = events(false, false)
		EventHistory(allUndone).undo()
		assertEquals(listOf(false, false), allUndone.appliedStates)

		val allApplied = events(true, true)
		EventHistory(allApplied).redo()
		assertEquals(listOf(true, true), allApplied.appliedStates)
	}

	@Test
	fun `an empty history can neither undo nor redo`() {
		val history = EventHistory(events())

		assertFalse(history.canUndo.get())
		assertFalse(history.canRedo.get())
		assertEquals(-1, history.currentIndexProperty.get())
	}

	@Test
	fun `undo targets the last applied event, not the last event`() {
		/* what a project loaded with an undone tail looks like */
		val events = events(true, true, false)
		val history = EventHistory(events)

		assertEquals(1, history.currentIndexProperty.get(), "the cursor sits on the last applied event")
		history.undo()

		assertEquals(listOf(true, false, false), events.appliedStates, "the undone tail is not the undo target")
	}

	@Test
	fun `redo is available when the history ends undone`() {
		val events = events(true, true, false)
		val history = EventHistory(events)

		assertTrue(history.canRedo.get())
		history.redo()

		assertEquals(listOf(true, true, true), events.appliedStates)
	}

	@Test
	fun `the cursor follows events applied from elsewhere`() {
		val events = events(true, true, true)
		val history = EventHistory(events)

		/* something other than undo unapplies the last two */
		events[2].value.set(false)
		events[1].value.set(false)

		assertEquals(0, history.currentIndexProperty.get())
		history.undo()
		assertEquals(listOf(false, false, false), events.appliedStates)
	}

	@Test
	fun `reapplying an event out of order moves the cursor to it`() {
		val events = events(false, false, false)
		val history = EventHistory(events)

		/* the last event is reapplied directly, skipping the ones before it */
		events[2].value.set(true)

		assertEquals(2, history.currentIndexProperty.get())
		assertFalse(history.canRedo.get(), "nothing after it to redo")
		history.undo()
		assertEquals(listOf(false, false, false), events.appliedStates)
	}

	@Test
	fun `deleting the event at the cursor moves it to the previous applied one`() {
		val events = events(true, true, false)
		val history = EventHistory(events)

		events.remove(events[1])

		assertEquals(0, history.currentIndexProperty.get())
		history.undo()
		assertEquals(listOf(false, false), events.appliedStates)
	}

	@Test
	fun `deleting every event leaves a usable history`() {
		val events = events(true, true, true)
		val history = EventHistory(events)

		events.clear()
		assertFalse(history.canUndo.get())
		assertFalse(history.canRedo.get())
		history.undo()
		history.redo()

		val added = event("added after the history was emptied")
		events.add(added)
		assertTrue(history.canUndo.get(), "the new event is applied, so it can be undone")
		history.undo()
		assertFalse(added.value.get())
		assertSame(added, events.single())
	}

	@Test
	fun `events added to a fully undone history become the cursor`() {
		val events = events(false, false)
		val history = EventHistory(events)
		assertFalse(history.canUndo.get())

		events.add(event("newest"))

		assertEquals(2, history.currentIndexProperty.get())
		history.undo()
		assertEquals(listOf(false, false, false), events.appliedStates)
	}

	@Test
	fun `stress - undo and redo the whole history, past both ends`() {
		val count = 200
		val events = events(*BooleanArray(count) { true })
		val history = EventHistory(events)

		repeat(count + 10) { history.undo() }
		assertTrue(events.appliedStates.none { it }, "everything should be undone")
		assertFalse(history.canUndo.get())

		repeat(count + 10) { history.redo() }
		assertTrue(events.appliedStates.all { it }, "everything should be reapplied")
		assertFalse(history.canRedo.get())
	}

	@Test
	fun `stress - interleaved deletes, toggles and additions keep the cursor correct`() {
		val events = events(true, true, true, true)
		val history = EventHistory(events)

		history.undo()                          /* [T, T, T, F] */
		events.remove(events[0])                /* [T, T, F] */
		events.add(event("added"))              /* [T, T, F, T] */
		assertEquals(3, history.currentIndexProperty.get(), "the added event is the newest applied")

		history.undo()                          /* [T, T, F, F] */
		assertEquals(listOf(true, true, false, false), events.appliedStates)

		events.removeAt(3)                      /* drop the undone tail: [T, T, F] */
		assertEquals(1, history.currentIndexProperty.get())

		history.undo()                          /* [T, F, F] */
		history.undo()                          /* [F, F, F] */
		history.undo()                          /* nothing left */
		assertEquals(listOf(false, false, false), events.appliedStates)

		repeat(3) { history.redo() }
		assertEquals(listOf(true, true, true), events.appliedStates)
	}

	@Test
	fun `stress - replacing the whole list rewires the listeners`() {
		val events = events(true, true)
		val history = EventHistory(events)
		val replaced = events.toList()

		events.setAll(event("a"), event("b", isApplied = false))

		/* the old events are no longer observed */
		replaced.forEach { it.value.set(false) }
		assertEquals(0, history.currentIndexProperty.get(), "only the new events count")

		/* and the new ones are */
		events[1].value.set(true)
		assertEquals(1, history.currentIndexProperty.get())
	}

	@Test
	fun `stress - many toggles in a row leave the cursor consistent`() {
		val count = 50
		val events = events(*BooleanArray(count) { true })
		val history = EventHistory(events)

		repeat(20) { round ->
			events.forEach { it.value.set(round % 2 == 0) }
			val expected = if (round % 2 == 0) count - 1 else -1
			assertEquals(expected, history.currentIndexProperty.get(), "round $round")
		}
	}
}
