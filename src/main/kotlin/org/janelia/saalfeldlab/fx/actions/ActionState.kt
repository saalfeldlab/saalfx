package org.janelia.saalfeldlab.fx.actions

import javafx.event.Event
import org.checkerframework.checker.units.qual.A
import org.janelia.saalfeldlab.fx.actions.Action.Companion.onAction
import org.janelia.saalfeldlab.fx.actions.ActionState.Companion.newInstance
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty


/**
 * Represents the state of an action, useful for ensuring some state is valid prior to [Action.onAction].
 *
 * Any fields of [ActionState] implementations that use [verifyProperty] to set a field after verification
 * prior to [Action.onAction] should be `lateinit`. This allows for a constructor without valid field values
 * that are then verified and set in [Action.verify], and guaranteed by [onAction]
 *
 * Particularly useful to abstract and reuse logic for shared state needs. For Example, the following:
 * ```kotlin
 *	interface NameState<A : ActionState<A>> : ActionState<A> {
 * 		var name: String
 * 		override fun <E : Event> verifyState(action: Action<E>) {
 * 			action.verifyProperty(::name, "Name should be initialized") { "Name!" }
 * 		}
 * 	}
 *
 *	interface DescriptionState<A : ActionState<A>> : ActionState<A> {
 * 		var description: String
 * 		override fun <E : Event> verifyState(action: Action<E>) {
 * 			action.verifyProperty(::description, "Description should be initialized") { "Description!" }
 * 		}
 * 	}
 *
 *	interface NameDescriptionState<A : ActionState<A>> : NameState<A>, DescriptionState<A> {
 *
 * 		override fun <E : Event> verifyState(action: Action<E>) {
 * 			super<NameState>.verifyState(action)
 * 			super<DescriptionState>.verifyState(action)
 * 		}
 * 	}
 *
 * class TestState() : NameDescriptionState<TestState> {
 *
 *      override lateinit var name: String
 *      override lateinit var description: String
 *      lateinit var extra: String
 *
 * 		private constructor(name: String, description: String, extra: String) : this() {
 * 			this.name = name
 * 			this.description = description
 * 			this.extra = extra
 * 		}
 *
 * 		override fun <E : Event> verifyState(action: Action<E>) {
 * 			super.verifyState(action)
 * 			action.verifyProperty(::extra, "Extra should be initialized") { "Extra!" }
 * 		}
 *
 * 		override fun verifiedCopy(): TestState {
 * 			return TestState(name, description, extra)
 * 		}
 * 	}
 *
 * ```
 */
interface ActionState {

	/**
	 * Register this [verifyState] for the given [this@verifyState]  .
	 *
	 * @param E The type of [Event] this action is valid for .
	 * @param The action which this state will be verified prior to triggering.
	 */
	fun <E : Event> verifyState(action: Action<E>)

	/**
	 * Creates a copy of this ActionState, AFTER it has been verified.
	 * That is to say, the resulting copy should be assumed to be valid.
	 *
	 * @return A new instance of this state that has been verified and is safe for use.
	 */
	fun copyFromVerified(verified: ActionState)

	companion object {
		/**
		 * Provides a factory function to create a new instance of [ActionState] using its primary constructor.
		 *
		 * @param A The specific type of [ActionState] being created.
		 * @return A factory function that produces instances of type [A].
		 */
		inline fun <reified A : ActionState> newInstance(): () -> A {
			return {
				A::class.constructors
					.firstOrNull { it.parameters.isEmpty() || it.parameters.all { it.isOptional } }
					?.callBy(emptyMap())!!
			}
		}
	}
}

open class VerifiablePropertyActionState(vararg delegates: Any) : ActionState {

	internal val verifiableProperties = mutableMapOf<String, Verifiable<*>>()

	init {
		delegates
			.filterIsInstance<VerifiablePropertyActionState>()
			.forEach {
				verifiableProperties += it.verifiableProperties
			}
	}

	override fun <E : Event> verifyState(action: Action<E>) {
		verifiableProperties.forEach { (_, verifiable) ->
			verifiable.registerVerify(action)
		}
	}

	private fun copyVerifiableProperties(verified: VerifiablePropertyActionState) {
		verifiableProperties.forEach { (id, property) ->
			verified.verifiableProperties[id]?.getValue()?.let {
				property.setFromAny(it)
			}
		}
	}

	override fun copyFromVerified(verified: ActionState) {
		copyVerifiableProperties(verified as VerifiablePropertyActionState)
	}
}

fun <T> VerifiablePropertyActionState.verifiable(condition: String? = null, generator: () -> T?): VerifiedProvider<T> {
	return VerifiedProvider<T>(this, condition, generator)
}

class VerifiedProvider<T>(val state: VerifiablePropertyActionState, val condition: String? = null, val generator: () -> T?) : PropertyDelegateProvider<Any?, Verifiable<T>> {
	override fun provideDelegate(thisRef: Any?, property: KProperty<*>): Verifiable<T> {
		val text = condition ?: "Verified Property '${property.name}' was not valid"
		return Verifiable(property.name, text, generator).also {
			state.verifiableProperties[it.id] = it
		}
	}
}

class Verifiable<T>(
	internal val id: String,
	private val expected: String,
	private val provider: () -> T?,
) : ReadWriteProperty<Any?, T> {

	private var value: T? = null

	internal fun setFromAny(value: Any?) {
		@Suppress("UNCHECKED_CAST")
		this.value = value as T?
	}

	internal fun getValue(): T? {
		return value
	}

	fun <E : Event> registerVerify(action: Action<E>) {
		action.verify(expected) { generateVerifiedProperty() }
	}

	private fun generateVerifiedProperty(): Boolean {
		value = value ?: provider()
		return value != null
	}


	override fun getValue(thisRef: Any?, property: KProperty<*>): T {
		return value ?: throw IllegalStateException("${property.name} not initialized.")
	}

	override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
		this.value = value
	}
}


/**
 * Verifies and sets a property of an [ActionState] through the provided [stateProvider].
 *
 * @param E The type of [Event] associated with the [Action].
 * @param A The type of the [ActionState] being verified.
 * @param T The type of the property being set.
 * @param property A mutable reference to the property that will be set during verification.
 * @param description A text description for the verification logic.
 * @param stateProvider A supplier providing the new state value, or `null` if not valid.
 */
fun <E : Event, T> Action<E>.verifyProperty(
	property: KMutableProperty0<T>,
	description: String,
	stateProvider: () -> T?,
) {
	verify(description) {
		stateProvider()?.let { property.set(it) } != null
	}
}

/**
 *
 * Register the [ActionState] provided by [createState] to the [verifyProperty] of this [Action],
 * and register the provided [withActionState] as the [Action.onAction], while providing
 * a new copy of the guaranteed valid [ActionState] to the [Action.onAction] call.
 *
 * @param E The type of [Event] triggering the action.
 * @param A The type of [ActionState] being verified and provided during [Action.onAction].
 * @param createState supplier to create a new [ActionState] instance.
 * @param withActionState executed during [Action.onAction] after being provided with a verified [ActionState].
 */
fun <E : Event, A : ActionState> Action<E>.onAction(
	createState: () -> A,
	withActionState: A.(E?) -> Unit,
) {
	val verifiedState = createState().apply { verifyState(this@onAction) }
	onAction {
		val verifiedCopy = createState().apply { copyFromVerified(verifiedState) }
		withActionState(verifiedCopy, it)
	}
}

/**
 * A convenience method to handle actions with a specific [ActionState], without requiring an explicit factory function.
 *
 * @param A The type of [ActionState].
 * @param withActionState executed during [Action.onAction] after being provided with a verified [ActionState].
 */
@JvmSynthetic
@JvmName("onActionState")
inline fun <reified A : ActionState> Action<Event>.onAction(
	noinline withActionState: A.(Event?) -> Unit,
) {
	onAction<Event, A>(withActionState)
}

/**
 * A convenience method for handling actions with a specific [Event] and [ActionState].
 *
 * @param E The type of [Event].
 * @param A The type of [ActionState].
 * @param withActionState executed during [Action.onAction] after being provided with a verified [ActionState].
 */
@JvmSynthetic
@JvmName("onActionStateWithEvent")
inline fun <E : Event, reified A : ActionState> Action<E>.onAction(
	noinline withActionState: A.(E?) -> Unit,
) {
	onAction<E, A>(newInstance<A>(), withActionState)
}