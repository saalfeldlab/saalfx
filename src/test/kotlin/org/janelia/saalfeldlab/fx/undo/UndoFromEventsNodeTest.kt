package org.janelia.saalfeldlab.fx.undo

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.TitledPane
import javafx.scene.layout.HBox
import javafx.stage.Stage
import javafx.util.Pair
import org.janelia.saalfeldlab.fx.util.InvokeOnJavaFXApplicationThread
import org.junit.Test
import org.testfx.framework.junit.ApplicationTest
import org.testfx.util.WaitForAsyncUtils
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Only what the UI adds on top of [EventHistory]: which buttons exist, what they are wired to, and when they are
 * disabled. The undo, redo and cursor logic is covered by [EventHistoryTest].
 */
class UndoFromEventsNodeTest : ApplicationTest() {

	private lateinit var root: HBox

	override fun start(stage: Stage) {
		root = HBox()
		stage.scene = Scene(root)
		stage.show()
	}

	private fun event(title: String, isApplied: Boolean = true): Pair<String, BooleanProperty> =
		Pair(title, SimpleBooleanProperty(isApplied))

	private fun events(vararg isApplied: Boolean): ObservableList<Pair<String, BooleanProperty>> = FXCollections
		.observableArrayList(isApplied.mapIndexed { index, applied -> event("event $index", applied) })

	private fun <T> onFx(block: () -> T): T {
		val result = AtomicReference<T>()
		InvokeOnJavaFXApplicationThread { result.set(block()) }
		WaitForAsyncUtils.waitForFxEvents()
		return result.get()
	}

	private fun showUndoRedoButtons(
		events: ObservableList<Pair<String, BooleanProperty>>,
		undoableEvents: EventHistory<String> = EventHistory(events),
	) = onFx {
		val display = EventDisplay.defaultDisplay<String>({ it }, { Label(it) })
		val node = UndoFromEventsNode(undoableEvents, display)
		root.children.setAll(node)
		node
	}

	/* the per event buttons live in a TitledPane graphic, which the skin only attaches once laid out; walk the
	 * nodes instead, so what is asserted does not depend on a layout pass */
	private fun Node.buttons(): List<Button> {
		val found = LinkedHashSet<Button>()

		fun visit(node: Node) {
			(node as? Button)?.let { found += it }
			(node as? ScrollPane)?.content?.let { visit(it) }
			(node as? TitledPane)?.graphic?.let { visit(it) }
			(node as? Parent)?.childrenUnmodifiable?.forEach { visit(it) }
		}
		visit(this)
		return found.toList()
	}

	private fun Node.button(text: String) = buttons().single { it.text == text }

	/* newest event first, matching the order they are shown in */
	private fun Node.deleteButtons() = buttons().filter { it.text == DELETE_INDICATOR }

	@Test
	fun `undo and redo buttons are wired to the history`() {
		val events = events(true, true)
		val node = showUndoRedoButtons(events)

		onFx { node.button(UNDO).fire() }
		assertEquals(listOf(true, false), events.map { it.value.get() })

		onFx { node.button(REDO).fire() }
		assertEquals(listOf(true, true), events.map { it.value.get() })
	}

	@Test
	fun `undo and redo buttons track what is available`() {
		val events = events(true, true)
		val node = showUndoRedoButtons(events)
		assertFalse(node.button(UNDO).isDisable)
		assertTrue(node.button(REDO).isDisable, "nothing undone yet")

		onFx { node.button(UNDO).fire() }
		assertFalse(node.button(UNDO).isDisable)
		assertFalse(node.button(REDO).isDisable)

		onFx { node.button(UNDO).fire() }
		assertTrue(node.button(UNDO).isDisable, "everything is undone")
		assertFalse(node.button(REDO).isDisable)
	}

	@Test
	fun `each delete button removes its own event`() {
		val events = events(true, true, true)
		val (oldest, middle, newest) = events.toList()
		val node = showUndoRedoButtons(events)

		/* the buttons are newest first, so the middle event is still the middle button */
		onFx { node.deleteButtons()[1].fire() }
		assertEquals(listOf(oldest, newest), events.toList(), "$middle should be the only one removed")

		onFx { node.deleteButtons().first().fire() }
		assertEquals(listOf(oldest), events.toList(), "$newest is the first button")

		onFx { node.deleteButtons().first().fire() }
		assertTrue(events.isEmpty())
	}

	@Test
	fun `the row count follows the events`() {
		val events = events(true, true)
		val node = showUndoRedoButtons(events)
		assertEquals(2, node.deleteButtons().size, "one delete button per event")

		onFx { events.add(event("added")) }
		assertEquals(3, node.deleteButtons().size)

		onFx { events.removeAt(0) }
		assertEquals(2, node.deleteButtons().size)

		onFx { events.clear() }
		assertTrue(node.deleteButtons().isEmpty())
	}

	@Test
	fun `delete all empties the history, and is disabled without events`() {
		val events = events(true, true)
		val node = showUndoRedoButtons(events)
		assertFalse(node.button(DELETE_ALL).isDisable)

		onFx { node.button(DELETE_ALL).fire() }
		assertTrue(events.isEmpty())
		assertTrue(node.button(DELETE_ALL).isDisable, "nothing to delete")
	}

	@Test
	fun `confirmDeleteAll can decline the delete all button`() {
		val events = events(true, true)
		val history = EventHistory(events)
		val node = onFx {
			val declining = object : UndoFromEventsNode<String>(history, EventDisplay.defaultDisplay({ it }, { Label(it) })) {
				override fun confirmDeleteAll() = false
			}
			root.children.setAll(declining)
			declining
		}

		onFx { node.button(DELETE_ALL).fire() }
		assertEquals(2, events.size, "delete all was declined, so nothing should be removed")
	}


	@Test
	fun `delete disabled removes only the undone events`() {
		val events = events(true, true, true)
		val (oldest, middle, newest) = events.toList()
		val node = showUndoRedoButtons(events)

		onFx { middle.value.set(false) }
		onFx { node.button(DELETE_DISABLED).fire() }

		assertEquals(listOf(oldest, newest), events.toList(), "only the undone event should be removed")
	}

	@Test
	fun `delete disabled is only enabled while something is undone`() {
		val events = events(true, true)
		val node = showUndoRedoButtons(events)

		assertTrue(node.button(DELETE_DISABLED).isDisable, "nothing is undone yet")

		onFx { node.button(UNDO).fire() }
		assertFalse(node.button(DELETE_DISABLED).isDisable)

		onFx { node.button(DELETE_DISABLED).fire() }
		assertEquals(1, events.size)
		assertTrue(node.button(DELETE_DISABLED).isDisable, "the undone event is gone")
	}

	@Test
	fun `delete disabled goes through the bulk removal`() {
		val events = events(true, false)
		val refused = object : EventHistory<String>(events) {
			override fun deleteAll(entries: Collection<Pair<String, BooleanProperty>>) = Unit
		}
		val node = showUndoRedoButtons(events, refused)

		onFx { node.button(DELETE_DISABLED).fire() }
		assertEquals(2, events.size, "one override should govern both bulk buttons")
	}

	@Test
	fun `overriding delete affectts deleteAll`() {
		val events = events(true, true, true)
		val refused = object : EventHistory<String>(events) {
			override fun delete(index: Int) = Unit
		}
		val node = showUndoRedoButtons(events, refused)

		onFx { node.deleteButtons().first().fire() }
		assertEquals(3, events.size)

		onFx { node.button(DELETE_ALL).fire() }
		assertEquals(3, events.size, "delete all should go through delete")
	}

	@Test
	fun `deleting through the buttons leaves a usable pane`() {
		val events = events(true, true, true)
		val node = showUndoRedoButtons(events)

		onFx { node.deleteButtons().forEach { it.fire() } }

		assertTrue(events.isEmpty())
		assertTrue(node.button(UNDO).isDisable)
		assertTrue(node.button(REDO).isDisable)

		val added = event("after everything was deleted")
		onFx { events.add(added) }
		assertFalse(node.button(UNDO).isDisable)
		onFx { node.button(UNDO).fire() }
		assertFalse(added.value.get())
	}

	companion object {
		private const val UNDO = "Undo"
		private const val REDO = "Redo"
		private const val DELETE_ALL = "Delete All"
		private const val DELETE_DISABLED = "Delete Disabled"
		private const val DELETE_INDICATOR = "✕"
	}
}
