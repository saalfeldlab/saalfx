package org.janelia.saalfeldlab.fx.actions

import javafx.event.Event
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ActionStateTest {

	interface NameState {
		var name: String
	}

	interface DescriptionState {
		var description: String
	}

	open class TestNameState : VerifiablePropertyActionState(), NameState {
		override var name by verifiable { "Name!" }
	}

	open class TestDescriptionState : VerifiablePropertyActionState(), DescriptionState {
		override var description by verifiable("Expected Condition!") { "Description!" }
	}

	open class DescriptionNameState(
		private val nameState: NameState = TestNameState(),
		private val descriptionState: DescriptionState = TestDescriptionState(),
	) :
		VerifiablePropertyActionState(nameState, descriptionState),
		NameState by nameState,
		DescriptionState by descriptionState

	open class TestState : DescriptionNameState() {
		var extra by verifiable { "Extra!" }
	}

	class InvalidTestState : TestState() {

		var invalid: String by verifiable { null }
	}

	@Test
	fun testActionState() {

		var actionRunCount = 0

		Action<Event>(Event.ANY).apply {
			onAction<TestState> {
				actionRunCount++
				assertEquals("Name!", name)
				assertEquals("Description!", description)
				assertEquals("Extra!", extra)
			}
		}.invoke(null)
		assertEquals(1, actionRunCount)

		Action<Event>(Event.ANY).apply {
			onAction<InvalidTestState> {
				/* Never run, since InvalidTestState is never valid */
				actionRunCount++
			}
		}.invoke(null)
		assertEquals(1, actionRunCount)

		Action<Event>(Event.ANY).apply {
			val manuallyMakeValid = {
				InvalidTestState().apply {
					assertFailsWith<IllegalStateException> { invalid }
					invalid = "Actually valid?"
					assertEquals("Actually valid?", invalid)
				}
			}
			onAction(manuallyMakeValid) {
				/* Never run, since ActionState is never valid */
				actionRunCount++
				assertEquals("Name!", name)
				assertEquals("Description!", description)
				assertEquals("Extra!", extra)
				assertEquals("Actually valid?", invalid)
			}
		}.invoke(null)
		assertEquals(2, actionRunCount)

	}
}