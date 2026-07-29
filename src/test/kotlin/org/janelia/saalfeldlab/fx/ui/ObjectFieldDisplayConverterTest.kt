package org.janelia.saalfeldlab.fx.ui

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Scene
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.stage.Stage
import javafx.util.StringConverter
import org.junit.Test
import org.testfx.framework.junit.ApplicationTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mimics the way paintera renders selected label ids: a comma separated list far too long to lay out, shown
 * shortened while the value keeps every id.
 */
class ObjectFieldDisplayConverterTest : ApplicationTest() {

	override fun start(stage: Stage) {
		stage.scene = Scene(HBox())
		stage.show()
	}

	private val displayLimit = 100

	private val idsConverter = object : StringConverter<List<Long>>() {
		override fun toString(ids: List<Long>?) = ids.orEmpty().joinToString(",")
		override fun fromString(string: String?) = string.orEmpty()
			.split(Regex("\\D+"))
			.filter { it.isNotBlank() }
			.map { it.toLong() }
	}

	private fun abridge(ids: List<Long>) = when {
		ids.size <= displayLimit -> ids.joinToString(",")
		else -> ids.take(displayLimit).joinToString(",") + ", … +${ids.size - displayLimit} more"
	}

	/** records what would go to the clipboard, so the test does not depend on a real one */
	private class RecordingCopyTextField(val wholeText: () -> String) : TextField() {

		var copied: String? = null

		override fun copy() {
			val whole = wholeText()
			copied = if (selectedText == text && text != whole) whole else selectedText
		}
	}

	private fun abridgedField(ids: List<Long>): Pair<ObjectField<List<Long>, SimpleObjectProperty<List<Long>>>, RecordingCopyTextField> {
		val property = SimpleObjectProperty(ids)
		val textField = RecordingCopyTextField { idsConverter.toString(property.value) }
		val objectField = ObjectField(
			property,
			idsConverter,
			textField,
			ObjectField.SubmitOn.ENTER_PRESSED,
			ObjectField.SubmitOn.FOCUS_LOST,
		)
		objectField.displayConverter = { abridge(it) }
		return objectField to textField
	}

	@Test
	fun `renders an abridged form but keeps the whole value`() {
		val ids = (1L..500L).toList()
		val (objectField, textField) = abridgedField(ids)

		assertTrue(textField.text.endsWith("+400 more"), "expected an abridged rendering, got `${textField.text.takeLast(24)}'")
		assertTrue(textField.text.length < idsConverter.toString(ids).length, "the rendering should be shorter than the value")
		assertEquals(ids, objectField.value, "the value keeps every id")
	}

	@Test
	fun `an untouched field is never parsed back`() {
		val ids = (1L..500L).toList()
		val (objectField, _) = abridgedField(ids)

		/* what losing focus does; the abridged text cannot round trip, so it must not be submitted */
		objectField.submit()

		assertEquals(ids, objectField.value, "submitting an unedited abridged field must not shrink the value")
	}

	@Test
	fun `editing the field submits what was typed`() {
		val (objectField, textField) = abridgedField(listOf(1L, 2L, 3L))

		textField.text = "7,8,9"
		objectField.submit()

		assertEquals(listOf(7L, 8L, 9L), objectField.value)
	}

	@Test
	fun `pasting more than the display limit keeps them all`() {
		val (objectField, textField) = abridgedField(listOf(1L))
		val pasted = (1L..500L).toList()

		textField.text = idsConverter.toString(pasted)
		objectField.submit()

		assertEquals(pasted, objectField.value, "every pasted id is kept in the value")
		assertTrue(textField.text.endsWith("+400 more"), "the field re-renders abridged after the paste")
	}

	@Test
	fun `copying the whole displayed text yields the whole value`() {
		val ids = (1L..500L).toList()
		val (_, textField) = abridgedField(ids)

		textField.selectAll()
		textField.copy()

		assertEquals(idsConverter.toString(ids), textField.copied, "selecting all and copying yields every id")
	}

	@Test
	fun `copying part of the displayed text yields only that part`() {
		val (_, textField) = abridgedField((1L..500L).toList())

		textField.selectRange(0, 3)
		textField.copy()

		assertEquals("1,2", textField.copied, "a partial selection copies literally what is highlighted")
	}

	@Test
	fun `invalid input reverts to the abridged rendering, not the whole value`() {
		val ids = (1L..500L).toList()
		val strict = object : StringConverter<List<Long>>() {
			override fun toString(value: List<Long>?) = idsConverter.toString(value)
			override fun fromString(string: String?) = throw ObjectField.InvalidUserInput("nope")
		}
		val property = SimpleObjectProperty(ids)
		val textField = RecordingCopyTextField { strict.toString(property.value) }
		val objectField = ObjectField(property, strict, textField, ObjectField.SubmitOn.ENTER_PRESSED)
		objectField.displayConverter = { abridge(it) }

		textField.text = "not a list"
		objectField.submit()

		assertEquals(ids, objectField.value, "a rejected edit leaves the value alone")
		assertTrue(textField.text.endsWith("+400 more"), "and restores the abridged rendering, got `${textField.text.takeLast(24)}'")
	}

	@Test
	fun `an untouched field with a null value is not parsed back`() {
		val objectField = ObjectField.stringField(null, ObjectField.SubmitOn.ENTER_PRESSED, ObjectField.SubmitOn.FOCUS_LOST)

		objectField.submit()

		assertEquals(null, objectField.value, "an unedited empty field should not invent a value")
	}

	@Test
	fun `a null file renders as empty rather than throwing`() {
		val objectField = ObjectField.fileField(null, { true }, ObjectField.SubmitOn.ENTER_PRESSED)

		assertEquals("", objectField.textField.text.orEmpty())

		objectField.submit()
		assertEquals(null, objectField.value, "an unedited empty field should not invent a value")
	}

	@Test
	fun `clearing the display converter restores the whole rendering`() {
		val ids = (1L..500L).toList()
		val (objectField, textField) = abridgedField(ids)

		objectField.displayConverter = null

		assertEquals(idsConverter.toString(ids), textField.text, "without a display converter the whole value is rendered")
	}

	@Test
	fun `two fields sharing a property each render with their own converter`() {
		val property = SimpleObjectProperty((1L..500L).toList())
		val abridged = ObjectField(property, idsConverter, TextField(), ObjectField.SubmitOn.ENTER_PRESSED)
			.also { it.displayConverter = { ids -> abridge(ids) } }
		val counting = ObjectField(property, idsConverter, TextField(), ObjectField.SubmitOn.ENTER_PRESSED)
			.also { it.displayConverter = { ids -> "" + ids.size + " ids" } }

		property.value = (1L..7L).toList()

		assertEquals("1,2,3,4,5,6,7", abridged.textField.text)
		assertEquals("7 ids", counting.textField.text)
	}

	@Test
	fun `a value within the limit is rendered in full`() {
		val ids = listOf(4L, 8L, 15L)
		val (objectField, textField) = abridgedField(ids)

		assertEquals("4,8,15", textField.text)

		/* and an unedited submit still round trips cleanly */
		objectField.submit()
		assertEquals(ids, objectField.value)
	}
}
