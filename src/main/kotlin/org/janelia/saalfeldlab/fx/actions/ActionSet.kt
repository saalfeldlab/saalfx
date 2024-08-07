package org.janelia.saalfeldlab.fx.actions

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.EventType
import javafx.scene.Node
import javafx.scene.input.*
import javafx.stage.Window
import org.janelia.saalfeldlab.fx.event.KeyTracker
import java.lang.invoke.MethodHandles
import java.util.function.Consumer


/**
 * [ActionSet]s allowed for grouping [Action]s.
 *
 * It is often preferable to group [Action]s based on their similarity, either of use, or implementation.
 * [ActionSet] provides a convenient way to create and manage a group of [Action]
 *
 * For example, you may want your application to change state based on a key press, and to revert on key relase:
 * ```kotlin
 * val node : Node = StackPane()
 * val keyTracker : KeyTracker = KeyTracker()
 * val changeStateActionSet = ActionSet("Change Application State", { keyTracker }) {
 *  KeyEvent.KEY_PRESSED(KeyCode.SPACE) {
 *      filter = true
 *      onAction { App.changeState() }
 *  }
 *  KeyEvent.KEY_RELEASED(KeyCode.SPACE) {
 *      filter = true
 *      onAction { App.changeStateBack() }
 *  }
 * }
 * /* To add all actions */
 * node.installActionSet(changeStateActionSet)
 * /* to remove all actions */
 * node.removeActionSet(changeStateActionSet)
 * ```
 * Above we created an action set with two actions, one for a key press, and one for a key release, which trigger some state change to the applications
 *
 * See [DragActionSet] for more examples.
 *
 * @see DragActionSet
 *
 * @property name of the action set. If an [Action] is part of this [ActionSet] and has no name, this will be used for the [Action]s name as well.
 * @property keyTracker to use to keep track of the key state
 * @constructor creates this [ActionSet]
 *
 * @param apply configuration callback to configure this [ActionSet], and to create [Action]s
 */
open class ActionSet(val name: String, var keyTracker: () -> KeyTracker? = { null }, apply: (ActionSet.() -> Unit)? = null) {

	@JvmOverloads
	constructor(name: String, keyTracker: () -> KeyTracker? = { null }, apply: Consumer<ActionSet>?) : this(name, keyTracker, { apply?.accept(this) })

	val actions = mutableListOf<Action<out Event>>()
	private val actionHandlerMap = mutableMapOf<EventType<Event>, MutableList<EventHandler<Event>>>()
	private val actionFilterMap = mutableMapOf<EventType<Event>, MutableList<EventHandler<Event>>>()
	private val checks = mutableMapOf<EventType<out Event>, MutableList<Pair<String, (Event) -> Boolean>>>()

	init {
		apply?.let { it(this) }
	}

	private fun Action<out Event>.testChecksForEventType(event: Event, eventType: EventType<out Event> = event.eventType): Boolean {
		val checks = checks[eventType] ?: return true

		for ((reason, check) in checks) {
			if (!check(event)) {
				logger.debug { "Verify All Failed: $reason" }
				return false
			}
		}
		return true
	}

	private tailrec fun Action<out Event>.testChecksForInheritedEventTypes(event: Event, eventType: EventType<out Event>? = event.eventType): Boolean {
		if (eventType == null) return true
		return if (!testChecksForEventType(event, eventType)) false else testChecksForInheritedEventTypes(event, eventType.superType)
	}

	/**
	 * Specify a check that will be evaluated for all [Action]s in this [ActionSet] that are triggered by [eventType]
	 *
	 * @param E type of the [Event] that [check] acts on
	 * @param eventType that we are checking against
	 * @param reason for which we are verifying
	 * @param check callback to verify if an [Action] in this set is valid for an event
	 * @receiver
	 */
	@Suppress("UNCHECKED_CAST")
	fun <E : Event> verifyAll(eventType: EventType<E>, reason: String = "", check: (E) -> Boolean) {
		checks[eventType]?.add(reason to check as (Event) -> Boolean) ?: let {
			checks[eventType] = mutableListOf(reason to check as (Event) -> Boolean)
		}
	}

	/**
	 * Creates an [Action] and adds it to this [ActionSet]
	 *
	 * @param E the [Event] that the created [Action] will handle
	 * @param eventType the [EventType] that the created [Action] will trigger on
	 * @param withAction configuration in the created [Action]s scope
	 * @return the created and configured [Action]
	 */
	fun <E : Event> action(eventType: EventType<E>, withAction: Action<E>.() -> Unit = {}): Action<E> {
		return Action(eventType).apply {
			keyTracker = this@ActionSet.keyTracker

			/* set the default name */
			name = this@ActionSet.name

			withAction()

		}.also { addAction(it) }
	}

	/**
	 * Add an existing [Action] to this [ActionSet]
	 *
	 * @param E the [Event] that [action] handles
	 * @param action the [Action] to add
	 */
	@Suppress("UNCHECKED_CAST")
	fun <E : Event> addAction(action: Action<E>) {
		actions += action

		val handler = ActionSetActionEventHandler(action)

		val eventType = action.eventType as EventType<Event>
		/* Add as filter or handler, depending on action flag */
		val actionMap = if (action.filter) actionFilterMap else actionHandlerMap
		actionMap[eventType]?.let { it += handler } ?: let {
			actionMap[eventType] = mutableListOf(handler)
		}
	}


	/**
	 * Add a [KeyAction] created and configured via [withAction]
	 *
	 * @param eventType to trigger
	 * @param withAction a consumer providing the created [KeyAction] for configuration
	 * @return the [KeyAction]
	 */
	@JvmOverloads
	fun addKeyAction(eventType: EventType<KeyEvent>, withAction: Consumer<KeyAction>? = null): KeyAction {
		return withAction?.let {
			keyAction(eventType) {
				withAction.accept(this)
			}
		} ?: keyAction(eventType)
	}

	/**
	 * Create and add a [KeyAction] to the [ActionSet].
	 *
	 * @param eventType that triggers the [KeyAction]
	 * @param withAction callback to configure the [KeyAction] within its scope
	 * @return the created [KeyAction]
	 */
	@JvmSynthetic
	fun keyAction(eventType: EventType<KeyEvent>, withAction: KeyAction.() -> Unit = {}): KeyAction {
		return KeyAction(eventType).apply {
			keyTracker = this@ActionSet.keyTracker
			/* set the default name */
			name = this@ActionSet.name
			withAction()
		}.also { addAction(it) }
	}

	/**
	 * Convenience operator to create a [KeyAction] from a [KeyEvent] [EventType] receiver, while specifying the required [KeyCodeCombination]
	 *
	 * @param withKeys [KeyCodeCombination]s required to match the [KeyEvent].
	 * @param withAction [KeyAction] configuration callback
	 * @return the [KeyAction]
	 */
	operator fun EventType<KeyEvent>.invoke(withKeys: KeyCombination, withAction: KeyAction.() -> Unit): KeyAction {
		/* create the Action*/
		return KeyAction(this).apply {
			keyTracker = this@ActionSet.keyTracker

			/* set the default name */
			name = this@ActionSet.name

			/* configure based on the withKeys paramters*/
			ignoreKeys()
			verify { withKeys.match(it) }

			/* configure via the callback*/
			withAction()
		}.also { addAction(it)}
	}

	/**
	 * Convenience operator to create a [KeyAction] from a [KeyEvent] [EventType] receiver, while specifying the required [KeyCode]s
	 *
	 * @param withKeys [KeyCode]s required to be down UNLESS [KeyEvent] is [KeyEvent.KEY_RELEASED], in which case [withKeys] are passed to [KeyAction.keysReleased].
	 * @param withAction [KeyAction] configuration callback
	 * @return the [KeyAction]
	 */
	operator fun EventType<KeyEvent>.invoke(vararg withKeys: KeyCode, withAction: KeyAction.() -> Unit): KeyAction {

		/* create the Action*/
		return KeyAction(this).apply {
			keyTracker = this@ActionSet.keyTracker

			/* set the default name */
			name = this@ActionSet.name

			/* configure based on the withKeys parameters*/
			if (eventType == KeyEvent.KEY_RELEASED) {
				ignoreKeys()
				keysReleased(*withKeys)
			} else if (withKeys.isNotEmpty()) {
				keysDown(*withKeys)
			}

			/* configure via the callback*/
			withAction()
		}.also { addAction(it) }
	}

	/**
	 * Convenience operator to create a [KeyAction] from a [KeyEvent] [EventType] receiver, while specifying the required keybindings
	 *
	 * @param keyBindings key binding map to lookup the [keyName] in
	 * @param keyName name of the valid key combination in the [keyBindings] map
	 * @param withAction [KeyAction] configuration callback
	 * @return the [KeyAction]
	 */
	operator fun EventType<KeyEvent>.invoke(keyBindings: NamedKeyCombination.CombinationMap, keyName: String, keysExclusive: Boolean = true, withAction: KeyAction.() -> Unit): KeyAction {

		return this(keyBindings[keyName]!!, keysExclusive, withAction)
	}

	/**
	 * Convenience operator to create a [KeyAction] from a [KeyEvent] [EventType] receiver, while specifying the required [NamedKeyBinding]
	 *
	 * @param keyBinding key combination to validate the action against
	 * @param withAction [KeyAction] configuration callback
	 * @return the [KeyAction]
	 */
	operator fun EventType<KeyEvent>.invoke(keyBinding: NamedKeyBinding, keysExclusive: Boolean = true, withAction: KeyAction.() -> Unit): KeyAction {

		/* create the Action*/
		return KeyAction(this).apply {
			keyTracker = this@ActionSet.keyTracker
			name = keyBinding.keyBindingName

			/* configure based on the keyBinding */
			keyMatchesBinding(keyBinding, keysExclusive)

			/* configure via the callback*/
			withAction()
		}.also { addAction(it) }
	}

	/**
	 *  Extension to create and add a [MouseAction] with various configuration options.
	 *
	 *  @param mouseButtonTrigger to check against the event. If none provided, any mouse button will be valid
	 *  @param onRelease only meaningful if [mouseButtonTrigger] is provided. Dictates whether to trigger on mouse press or release
	 *  @param withKeysDown optional keys to check against when the action is triggered
	 *  @param keysExclusive whether [withKeysDown] is strict, or allows other keys as well
	 *  @param withAction [MouseAction] configuration callback
	 *
	 */
	@JvmOverloads
	@Suppress("UNCHECKED_CAST")
	operator fun <E : MouseEvent> EventType<E>.invoke(
		mouseButtonTrigger: MouseButton? = null,
		onRelease: Boolean = false,
		withKeysDown: Array<KeyCode>? = null,
		keysExclusive: Boolean = false,
		withAction: MouseAction.() -> Unit
	): MouseAction {

		/* create the Action*/
		return MouseAction(this as EventType<MouseEvent>).apply {
			keyTracker = this@ActionSet.keyTracker

			/* set the default name */
			name = this@ActionSet.name

			/* configure based on the parameters */
			mouseButtonTrigger?.let {
				/* default to exclusive if pressed, and NOT exclusive if released*/
				verifyButtonTrigger(mouseButtonTrigger, released = onRelease, exclusive = !onRelease)
			}

			withKeysDown?.let {
				keysDown(*it, exclusive = keysExclusive)
			} ?: ignoreKeys()

			/* configure based on the callback */
			withAction()
		}.also { addAction(it)}
	}

	/**
	 * Create and return an [Action] of subtype [R], based on the reciever [E] [EventType].
	 *
	 * If [E] is a [KeyEvent], the returned action will be a [KeyAction]
	 * If [E] is a [MouseEvent], the returned action will be a [MouseAction]
	 * If [E] is neither, the returned action will be an [Action]
	 *
	 * in any case, the resulting action is configured to require [withKeysDown] to be pressed.
	 *
	 * @param E the target event type
	 * @param R the resultant Action type
	 * @param withKeysDown required for resultant action to be valid
	 * @param withAction configuration callback in the resultant Actions scope
	 * @receiver [EventType] the resultant action will trigger
	 * @return the created action of type [R]
	 */
	inline operator fun <reified E : Event, reified R : Action<E>> EventType<E>.invoke(vararg withKeysDown: KeyCode, noinline withAction: R.() -> Unit): R {
		return actionFromEventType(withAction).apply {
			if (withKeysDown.isNotEmpty()) keysDown(*withKeysDown, exclusive = this.keysExclusive)
		}
	}

	/**
	 * Create and return an [Action] of subtype [R], based on the reciever [E] [EventType].
	 *
	 * If [E] is a [KeyEvent], the returned action will be a [KeyAction]
	 * If [E] is a [MouseEvent], the returned action will be a [MouseAction]
	 * If [E] is neither, the returned action will be an [Action]
	 *
	 * @param E the target event type
	 * @param R the resultant Action type
	 * @param withAction configuration callback in the resultant Actions scope
	 * @receiver [EventType] the resultant action will trigger
	 * @return the created action of type [R]
	 */
	inline operator fun <reified E : Event, reified R : Action<E>> EventType<E>.invoke(noinline withAction: R.() -> Unit): R {
		return actionFromEventType(withAction)
	}


	@Suppress("UNCHECKED_CAST")
	inline fun <reified E : Event, reified R : Action<E>> EventType<E>.actionFromEventType(noinline withAction: R.() -> Unit) = when (E::class.java) {
		KeyEvent::class.java -> keyAction(this as EventType<KeyEvent>, withAction as KeyAction.() -> Unit) as R
		MouseEvent::class.java -> mouseAction(this as EventType<MouseEvent>, withAction as MouseAction.() -> Unit) as R
		else -> action(this, withAction as Action<E>.() -> Unit) as R
	}

	/**
	 * Add a [MouseAction] created and configured via [withAction]
	 *
	 * @param eventType to trigger
	 * @param withAction a consumer providing the created [MouseAction] for configuration
	 * @return the [MouseAction]
	 */
	@JvmOverloads
	fun addMouseAction(eventType: EventType<MouseEvent>, withAction: Consumer<MouseAction>? = null): MouseAction {
		return withAction?.let {
			mouseAction(eventType) {
				withAction.accept(this)
			}
		} ?: mouseAction(eventType)
	}

	/**
	 * Add a [MouseAction] created and configured via [withAction]
	 *
	 * @param eventType to trigger
	 * @param withAction a configuration callback in the created [MouseAction]s scope
	 * @return the [MouseAction]
	 */
	@JvmSynthetic
	fun mouseAction(eventType: EventType<MouseEvent>, withAction: MouseAction.() -> Unit = {}): MouseAction {
		return MouseAction(eventType).apply {
			keyTracker = this@ActionSet.keyTracker

			/* set the default name */
			name = this@ActionSet.name

			withAction()
		}.also { addAction(it) }
	}


	private operator fun <E : Event> invoke(event: E, action: Action<E>) {
		if (preInvokeCheck(action, event)) {
			try {
				action(event)
			} catch (e: Exception) {
				action.logger.error(e) {"${event.eventType} was valid, but failed" }
				throw e
			}
		} else {
			action.logger.trace { "preInvokeCheck failed" }
		}
	}

	/**
	 * This check is tested when an [EventType] that is a trigger for an [Action] in this [ActionSet] is detected.
	 * In that case, the [ActionSet]s checks are tested prior to testing the [Action] itself.
	 * This allows certain checks to apply to all [Action]s in this set, for example.
	 *
	 * @param E the event that [action] could handle
	 * @param action the [Action] to handle [event]
	 * @param event to handle
	 */
	protected open fun <E : Event> preInvokeCheck(action: Action<E>, event: E) = action.run {
		canHandleEvent(event.eventType) && testChecksForInheritedEventTypes(event)
	}

	/**
	 * Optional callback for before the extension functions [installActionSet] are called.
	 */
	open fun preInstallSetup() {}

	/**
	 * Optional callback for after the extension functions [removeActionSet] are called.
	 */
	open fun postRemoveCleanUp() {}

	private inner class ActionSetActionEventHandler(val action: Action<out Event>) : EventHandler<Event> {

		@Suppress("UNCHECKED_CAST")
		override fun handle(event: Event) {
			this@ActionSet(event, action as Action<Event>)
		}

		override fun toString(): String {
			return if (action.name.isNullOrEmpty()) {
				this@ActionSet.name.ifEmpty { super.toString() }
			} else {
				action.name!!
			}
		}

	}

	companion object {

		/**
		 * Install [actionSet] in the receiver [Node]
		 *
		 * @param actionSet to install
		 */
		@JvmStatic
		fun Node.installActionSet(actionSet: ActionSet) {
			actionSet.preInstallSetup()
			actionSet.actionFilterMap.forEach { (eventType, actions) ->
				actions.forEach { action ->
					addEventFilter(eventType, action)
				}
			}
			actionSet.actionHandlerMap.forEach { (eventType, actions) ->
				actions.forEach { action ->
					addEventHandler(eventType, action)
				}
			}
		}

		/**
		 * Remove [actionSet] from the receiver [Node]. No effect if not installed.
		 *
		 * @param actionSet to remove
		 */
		@JvmStatic
		fun Node.removeActionSet(actionSet: ActionSet) {
			actionSet.actionFilterMap.forEach { (eventType, actions) ->
				actions.forEach { action ->
					removeEventFilter(eventType, action)
				}
			}
			actionSet.actionHandlerMap.forEach { (eventType, actions) ->
				actions.forEach { action ->
					removeEventHandler(eventType, action)
				}
			}
			actionSet.postRemoveCleanUp()
		}

		/**
		 * Install [actionSet] in the receiver [Window]
		 *
		 * @param actionSet to install
		 */
		@JvmStatic
		fun Window.installActionSet(actionSet: ActionSet) {
			actionSet.preInstallSetup()
			actionSet.actionFilterMap.forEach { (eventType, actions) ->
				actions.forEach { action ->
					addEventFilter(eventType, action)
				}
			}
			actionSet.actionHandlerMap.forEach { (eventType, actions) ->
				actions.forEach { action ->
					addEventHandler(eventType, action)
				}
			}
			actionSet.postRemoveCleanUp()
		}

		/**
		 * Remove [actionSet] from the receiver [Window]. No effect if not installed.
		 *
		 * @param actionSet to remove
		 */
		@JvmStatic
		fun Window.removeActionSet(actionSet: ActionSet) {
			actionSet.actionFilterMap.forEach { (eventType, actions) ->
				actions.forEach { action ->
					removeEventFilter(eventType, action)
				}
			}
			actionSet.actionHandlerMap.forEach { (eventType, actions) ->
				actions.forEach { action ->
					removeEventHandler(eventType, action)
				}
			}
			actionSet.postRemoveCleanUp()
		}
	}
}
