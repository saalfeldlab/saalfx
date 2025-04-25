package org.janelia.saalfeldlab.fx.actions

import javafx.event.Event
import org.checkerframework.checker.units.qual.A
import org.janelia.saalfeldlab.fx.actions.Action.Companion.onAction
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty


/**
 * Represents the state of an action, useful for ensuring some state is valid prior to [Action.onActionWithState].
 *
 * Any fields of [ActionState] implementations that use [verifyProperty] to set a field after verification
 * prior to [Action.onActionWithState] should be `lateinit`. This allows for a constructor without valid field values
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
	 * @param action The action which this state will be verified for prior to triggering.
	 */
	fun <E : Event> verifyState(action: Action<E>)

	companion object {
		/**
		 * Provides a factory function to create a new instance of [ActionState] using its primary constructor.
		 *
		 * @param A The specific type of [ActionState] being created.
		 * @return A factory function that produces instances of type [A].
		 */
		inline fun <reified A : ActionState> newByReflection(): () -> A {
			val constructor = A::class.constructors.firstOrNull<KFunction<A>> { it.parameters.isEmpty() || it.parameters.all { it.isOptional } }
				?: throw NoSuchMethodException("No constructor found for ${A::class.simpleName} with either no parameters, or all optional parameters")
			return { constructor.callBy(emptyMap()) }
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
		this@VerifiablePropertyActionState.verifiableProperties.forEach { (_, verifiable) ->
			verifiable.registerVerify(action)
		}
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
		return value ?: throw IllegalStateException("Verifiable Property ${property.name} referenced before verify")
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