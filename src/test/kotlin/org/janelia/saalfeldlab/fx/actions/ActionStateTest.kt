package org.janelia.saalfeldlab.fx.actions

import javafx.event.Event
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import org.janelia.saalfeldlab.fx.actions.ActionStateTest.IncrementActionState.Companion.actionCount
import org.janelia.saalfeldlab.fx.actions.ActionStateTest.IncrementActionState.Companion.actionIsValid
import org.janelia.saalfeldlab.fx.actions.ActionStateTest.IncrementActionState.Companion.stateIsValid
import org.janelia.saalfeldlab.fx.actions.ActionStateTest.IncrementActionState.Companion.verifiedCount
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
			onActionWithState<TestState> {
				actionRunCount++
				assertEquals("Name!", name)
				assertEquals("Description!", description)
				assertEquals("Extra!", extra)
			}
		}.invoke()
		assertEquals(1, actionRunCount)

		Action<Event>(Event.ANY).apply {
			onActionWithState<InvalidTestState> {
				/* Never run, since InvalidTestState is never valid */
				actionRunCount++
			}
		}.invoke()
		assertEquals(1, actionRunCount)

		Action<Event>(Event.ANY).apply {
			val manuallyMakeValid = {
				InvalidTestState().apply {
					assertFailsWith<IllegalStateException> { invalid }
					invalid = "Actually valid?"
					assertEquals("Actually valid?", invalid)
				}
			}
			onActionWithState(manuallyMakeValid) {
				/* Never run, since ActionState is never valid */
				actionRunCount++
				assertEquals("Name!", name)
				assertEquals("Description!", description)
				assertEquals("Extra!", extra)
				assertEquals("Actually valid?", invalid)
			}
		}.invoke()
		assertEquals(2, actionRunCount)

	}

	internal class IncrementActionState : VerifiablePropertyActionState() {

		private var counter by verifiable("Increment verifiedCount") {
			propertyIsValid.takeIf { it }?.let { verifiedCount++ }
		}

		override fun <E : Event> verifyState(action: Action<E>) {
			super.verifyState(action)
			action.verify("State is not valid") { stateIsValid }
		}

		companion object {
			var actionIsValid = true
			var stateIsValid = true
			var propertyIsValid = true
			var verifiedCount = 0
			var actionCount = 0
		}
	}


	@Test
	fun `before and after verify action should be called exactly once every valid action`() {

		var expectedVerifiedCount = 0
		var expectedActionCount = 0

		val action = Action(MOUSE_CLICKED).apply {
			keysDown = null
			verify("action is not valid ") { actionIsValid }
			onActionWithState<IncrementActionState> {
				actionCount++
			}
		}

		val mouseClickedEvent = {
			MouseEvent(
				MOUSE_CLICKED,
				0.0, 0.0, 0.0, 0.0,
				null, 1,
				false, false, false, false,
				false, false, false, false, false,
				false, null
			)
		}

		/* should be 0 prior to first action call*/
		assertEquals(expectedVerifiedCount, verifiedCount)
		assertEquals(expectedActionCount, actionCount)

		/* Should be incremented for both after first successful action call*/
		action(mouseClickedEvent())
		assertEquals(++expectedVerifiedCount, verifiedCount)
		assertEquals(++expectedActionCount, actionCount)

		/* incremented again for both after repeated successful action call */
		action(mouseClickedEvent())
		assertEquals(++expectedVerifiedCount, verifiedCount)
		assertEquals(++expectedActionCount, actionCount)

		/* when state is invalid, verify should be called, but fail, so action is never triggered.
		* So increment for verified but not for action*/
		stateIsValid = false
		action(mouseClickedEvent())
		assertEquals(++expectedVerifiedCount, verifiedCount)
		assertEquals(expectedActionCount, actionCount)

		/* Ensure another success test works */
		stateIsValid = true
		actionIsValid = true
		action(mouseClickedEvent())
		assertEquals(++expectedVerifiedCount, verifiedCount)
		assertEquals(++expectedActionCount, actionCount)

		/* If action is invalid, but state is valid, then (depending on the order) the action verification
		* should fail before it even gets to the state verification. So neither should increment */
		stateIsValid = true
		actionIsValid = false
		action(mouseClickedEvent())
		assertEquals(expectedVerifiedCount, verifiedCount)
		assertEquals(expectedActionCount, actionCount)


		/* Ensure another success test works */
		stateIsValid = true
		actionIsValid = true
		action(mouseClickedEvent())
		assertEquals(++expectedVerifiedCount, verifiedCount)
		assertEquals(++expectedActionCount, actionCount)

		/* incorrect event type should not trigger checks */
		val mouseMovedEvent = {
			MouseEvent(
				MouseEvent.MOUSE_MOVED,
				0.0, 0.0, 0.0, 0.0,
				null, 1,
				false, false, false, false,
				false, false, false, false, false,
				false, null
			)
		}

		action(mouseMovedEvent())
		assertEquals(expectedVerifiedCount, verifiedCount)
		assertEquals(expectedActionCount, actionCount)

		/* Ensure another success test works */
		stateIsValid = true
		actionIsValid = true
		action(mouseClickedEvent())
		assertEquals(++expectedVerifiedCount, verifiedCount)
		assertEquals(++expectedActionCount, actionCount)
	}
}