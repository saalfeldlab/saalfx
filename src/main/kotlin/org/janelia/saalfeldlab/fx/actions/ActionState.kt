package org.janelia.saalfeldlab.fx.actions

import javafx.event.Event
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.full.primaryConstructor


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
 *
 * @param self-referential type, used for creating new instances after verification.
 */
interface ActionState<A> where A : ActionState<A> {

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
    fun verifiedCopy(): A

    companion object {
        /**
         * Provides a factory function to create a new instance of [ActionState] using its primary constructor.
         *
         * @param A The specific type of [ActionState] being created.
         * @return A factory function that produces instances of type [A].
         */
        inline operator fun <reified A : ActionState<A>> invoke(): () -> A {
            return { A::class.primaryConstructor!!.call() }
        }
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
fun <E : Event, A : ActionState<A>, T> Action<E>.verifyProperty(
    property: KMutableProperty0<T>,
    description: String,
    stateProvider: () -> T?
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
fun <E : Event, A : ActionState<A>> Action<E>.onAction(
    createState: () -> A,
    withActionState: A.(E?) -> Unit
) {
    val verifiedState = createState().apply { verifyState(this@onAction) }
    onAction {
        withActionState(verifiedState.verifiedCopy(), it)
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
inline fun <reified A : ActionState<A>> Action<Event>.onAction(
    noinline withActionState: A.(Event?) -> Unit
) {
    onAction<Event, A>(ActionState<A>(), withActionState)
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
inline fun <E : Event, reified A : ActionState<A>> Action<E>.onAction(
    noinline withActionState: A.(E?) -> Unit
) {
    onAction<E, A>(ActionState<A>(), withActionState)
}