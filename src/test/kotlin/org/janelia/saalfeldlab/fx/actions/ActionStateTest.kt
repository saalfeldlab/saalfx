package org.janelia.saalfeldlab.fx.actions

import javafx.event.Event
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ActionStateTest {

	interface NameState<A : ActionState<A>> : ActionState<A> {
		var name: String

		override fun <E : Event> verifyState(action: Action<E>) {
			action.verifyProperty(::name, "Name should be initialized") { "Name!" }
		}
	}

	interface DescriptionState<A : ActionState<A>> : ActionState<A> {
		var description: String

		override fun <E : Event> verifyState(action: Action<E>) {
			action.verifyProperty(::description, "Description should be initialized") { "Description!" }
		}
	}


	interface NameDescriptionState<A : ActionState<A>> : NameState<A>, DescriptionState<A> {

		override fun <E : Event> verifyState(action: Action<E>) {
			super<NameState>.verifyState(action)
			super<DescriptionState>.verifyState(action)
		}
	}

	class TestState() : NameDescriptionState<TestState> {

		override lateinit var name: String
		override lateinit var description: String
		lateinit var extra: String

		private constructor(name: String, description: String, extra: String) : this() {
			this.name = name
			this.description = description
			this.extra = extra
		}

		override fun <E : Event> verifyState(action: Action<E>) {
			super.verifyState(action)
			action.verifyProperty(::extra, "Extra should be initialized") { "Extra!" }
		}

		override fun verifiedCopy(): TestState {
			return TestState(name, description, extra)
		}
	}

	class InvalidTestState() : NameDescriptionState<InvalidTestState> {

		override lateinit var name: String
		override lateinit var description: String
		lateinit var extra: String
		lateinit var invalid: String

		private constructor(name: String, description: String, extra: String, invalid: String) : this() {
			this.name = name
			this.description = description
			this.extra = extra
			this.invalid = invalid
		}

		override fun <E : Event> verifyState(action: Action<E>) {
			super.verifyState(action)
			action.verifyProperty(::extra, "Extra should be initialized") { "Extra!" }
			action.verifyProperty(::invalid, "Invalid intentionally not set valid for test") {
				if (::invalid.isInitialized) invalid else null
			}
		}

		override fun verifiedCopy(): InvalidTestState {
			return InvalidTestState(name, description, extra, invalid)
		}
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
				/* Never run, since ActionState is never valid */
				actionRunCount++
			}
		}.invoke(null)
		assertEquals(1, actionRunCount)

		Action<Event>(Event.ANY).apply {
			val manuallyMakeValid = {
				InvalidTestState().apply {
					assertFailsWith<UninitializedPropertyAccessException> { invalid }
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