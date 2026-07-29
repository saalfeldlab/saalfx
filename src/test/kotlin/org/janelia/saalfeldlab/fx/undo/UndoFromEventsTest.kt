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
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Only what the UI adds on top of [EventHistory]: which buttons exist, what they are wired to, and when they are
 * disabled. The undo, redo and cursor logic is covered by [EventHistoryTest].
 */
class UndoFromEventsTest : ApplicationTest() {

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
		onDelete: ((Pair<String, BooleanProperty>) -> Unit)? = null,
		onDeleteAll: (() -> Unit)? = null,
	) = onFx {
		val node = UndoFromEvents.withUndoRedoButtons(events, { it }, { Label(it) }, onDelete, onDeleteAll)
		root.children.setAll(node)
		node
	}

	/* the per event buttons live in a TitledPane graphic, which the skin only attaches once laid out; walk the
	 * nodes instead, so what is asserted does not depend on a layout pass */
	private fun Node.buttons(): List<Button> {
		val found = LinkedHashSet<Button>()

		fun visit(node: Node) {
			(node as? Button)?.let { found += it }
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
	fun `delete buttons are only added when a delete callback is given`() {
		val events = events(true, true)

		assertTrue(showUndoRedoButtons(events).deleteButtons().isEmpty(), "no delete button without a callback")
		assertEquals(2, showUndoRedoButtons(events, onDelete = {}).deleteButtons().size, "one delete button per event")
	}

	@Test
	fun `each delete button reports its own event`() {
		val events = events(true, true, true)
		val deleted = mutableListOf<Pair<String, BooleanProperty>>()
		val node = showUndoRedoButtons(events, onDelete = { deleted += it })

		/* the newest event is shown first */
		onFx { node.deleteButtons().first().fire() }
		assertSame(events.last(), deleted.single())

		deleted.clear()
		onFx { node.deleteButtons().forEach { it.fire() } }
		assertEquals(events.size, deleted.size)
		events.forEach { event -> assertEquals(1, deleted.count { it === event }, "$event should be reported once") }
	}

	@Test
	fun `the row count follows the events`() {
		val events = events(true, true)
		val node = showUndoRedoButtons(events, onDelete = {})

		onFx { events.add(event("added")) }
		assertEquals(3, node.deleteButtons().size)

		onFx { events.removeAt(0) }
		assertEquals(2, node.deleteButtons().size)

		onFx { events.clear() }
		assertTrue(node.deleteButtons().isEmpty())
	}

	@Test
	fun `delete all is only added when a callback is given, and is disabled without events`() {
		val events = events(true)
		assertTrue(showUndoRedoButtons(events).buttons().none { it.text == DELETE_ALL })

		var deleteAllCalls = 0
		val node = showUndoRedoButtons(events, onDeleteAll = { deleteAllCalls++ })
		assertFalse(node.button(DELETE_ALL).isDisable)

		onFx { node.button(DELETE_ALL).fire() }
		assertEquals(1, deleteAllCalls)

		onFx { events.clear() }
		assertTrue(node.button(DELETE_ALL).isDisable, "nothing to delete")
	}

	@Test
	fun `deleting through the buttons leaves a usable pane`() {
		val events = events(true, true, true)
		val node = showUndoRedoButtons(events, onDelete = { events.remove(it) })

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
		private const val DELETE_INDICATOR = "✕"
	}
}
