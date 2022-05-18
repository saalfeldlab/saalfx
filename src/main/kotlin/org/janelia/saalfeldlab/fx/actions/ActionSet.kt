package org.janelia.saalfeldlab.fx.actions

import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.EventType
import javafx.scene.Node
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.stage.Window
import org.janelia.saalfeldlab.fx.event.KeyTracker
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.function.Consumer

open class ActionSet(val name: String, var keyTracker: KeyTracker? = null, apply: (ActionSet.() -> Unit)? = null) {

    @JvmOverloads
    constructor(name: String, keyTracker: KeyTracker? = null, apply: Consumer<ActionSet>?) : this(name, keyTracker, { apply?.accept(this) })

    private val actions = mutableListOf<Action<out Event>>()
    val actionHandlerMap = mutableMapOf<EventType<Event>, MutableList<EventHandler<Event>>>()
    val actionFilterMap = mutableMapOf<EventType<Event>, MutableList<EventHandler<Event>>>()
    val checks = mutableMapOf<EventType<out Event>, MutableList<(Event) -> Boolean>>()

    init {
        apply?.let { it(this) }
    }

    private fun testChecksForEventType(event: Event, eventType: EventType<out Event> = event.eventType): Boolean {
        return (checks[eventType]?.reduce { l, r -> { l(event) && r(event) } }?.invoke(event) ?: true)
    }

    private tailrec fun testChecksForInheritedEventTypes(event: Event, eventType: EventType<out Event>? = event.eventType): Boolean {
        if (eventType == null) return true
        return if (!testChecksForEventType(event, eventType)) false else testChecksForInheritedEventTypes(event, eventType.superType)
    }

    @Suppress("UNCHECKED_CAST")
    fun <E : Event> verifyAll(eventType: EventType<E>, check: (E) -> Boolean) {
        checks[eventType]?.add(check as (Event) -> Boolean) ?: let {
            checks[eventType] = mutableListOf(check as (Event) -> Boolean)
        }
    }

    fun <E : Event> action(eventType: EventType<E>, withAction: Action<E>.() -> Unit = {}): Action<E> {
        return Action(eventType)
            .also { it.keyTracker = this.keyTracker }
            .apply(withAction)
            .also { addAction(it) }
    }

    @Suppress("UNCHECKED_CAST")
    fun <E : Event> addAction(action: Action<E>) {
        actions += action

        val handler = object : EventHandler<Event> {
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

        val eventType = action.eventType as EventType<Event>
        /* Add as filter or handler, depending on action flag */
        val actionMap = if (action.filter) actionFilterMap else actionHandlerMap
        actionMap[eventType]?.let { it += handler } ?: let {
            actionMap[eventType] = mutableListOf(handler)
        }
    }

    @JvmOverloads
    fun addKeyAction(eventType: EventType<KeyEvent>, withAction: Consumer<KeyAction>? = null): KeyAction {
        return withAction?.let {
            keyAction(eventType) {
                withAction.accept(this)
            }
        } ?: keyAction(eventType)
    }

    @JvmSynthetic
    fun keyAction(eventType: EventType<KeyEvent>, withAction: KeyAction.() -> Unit = {}): KeyAction {
        return KeyAction(eventType)
            .also { it.keyTracker = this.keyTracker }
            .apply(withAction)
            .also { action -> addAction(action) }
    }

    operator fun EventType<KeyEvent>.invoke(vararg withKeysDown: KeyCode, withAction: KeyAction.() -> Unit): KeyAction {
        return keyAction(this, withAction).apply {
            if (withKeysDown.isNotEmpty()) {
                keysDown(*withKeysDown)
            }
        }
    }

    operator fun EventType<KeyEvent>.invoke(keyBindings: NamedKeyCombination.CombinationMap, keyName: String, withAction: KeyAction.() -> Unit): KeyAction {
        return keyAction(this, withAction).apply {
            keyMatchesBinding(keyBindings, keyName)
        }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <E : MouseEvent> EventType<E>.invoke(withAction: MouseAction.() -> Unit): MouseAction {
        return mouseAction(this as EventType<MouseEvent>, withAction)
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <E : MouseEvent> EventType<E>.invoke(vararg withKeysDown: KeyCode, withAction: MouseAction.() -> Unit): MouseAction {
        return mouseAction(this as EventType<MouseEvent>, withAction).apply {
            if (withKeysDown.isNotEmpty()) keysDown(*withKeysDown)
        }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <E : MouseEvent> EventType<E>.invoke(vararg withOnlyButtonsDown: MouseButton, withAction: MouseAction.() -> Unit): MouseAction {
        return mouseAction(this as EventType<MouseEvent>, withAction).apply {
            verifyButtonsDown(*withOnlyButtonsDown, exclusive = true)
        }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <E : MouseEvent> EventType<E>.invoke(withButtonTrigger: MouseButton, released : Boolean = false, withAction: MouseAction.() -> Unit): MouseAction {
        return mouseAction(this as EventType<MouseEvent>, withAction).apply {
            /* default to exclusive if pressed, and NOT exclusive if released*/
            verifyButtonTrigger(withButtonTrigger, released = released, exclusive = !released)
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline operator fun <reified E : Event, reified R : Action<E>> EventType<E>.invoke(vararg withKeysDown: KeyCode, noinline withAction: R.() -> Unit): R {
        return when (E::class.java) {
            KeyEvent::class.java -> keyAction(this as EventType<KeyEvent>, withAction as KeyAction.() -> Unit) as R
            MouseEvent::class.java -> mouseAction(this as EventType<MouseEvent>, withAction as MouseAction.() -> Unit) as R
            else -> action(this, withAction as Action<E>.() -> Unit) as R
        }.apply {
            if (withKeysDown.isNotEmpty()) keysDown(*withKeysDown)
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline operator fun <reified E : Event, reified R : Action<E>> EventType<E>.invoke(noinline withAction: R.() -> Unit): R {
        return when (E::class.java) {
            KeyEvent::class.java -> keyAction(this as EventType<KeyEvent>, withAction as KeyAction.() -> Unit) as R
            MouseEvent::class.java -> mouseAction(this as EventType<MouseEvent>, withAction as MouseAction.() -> Unit) as R
            else -> action(this, withAction as Action<E>.() -> Unit) as R
        }
    }

    @JvmOverloads
    fun addMouseAction(eventType: EventType<MouseEvent>, withAction: Consumer<MouseAction>? = null): MouseAction {
        return withAction?.let {
            mouseAction(eventType) {
                withAction.accept(this)
            }
        } ?: mouseAction(eventType)
    }

    @JvmSynthetic
    fun mouseAction(eventType: EventType<MouseEvent>, withAction: MouseAction.() -> Unit = {}): MouseAction {
        return MouseAction(eventType)
            .also { it.keyTracker = this.keyTracker }
            .apply(withAction)
            .also { addAction(it) }
    }


    operator fun <E : Event> invoke(event: E, action: Action<E>) {
        if (preInvokeCheck(action, event)) {
            try {
                if (action(event)) {
                    val logger = if (action.filter) FILTER_LOGGER else ACTION_LOGGER
                    val nameText = action.name?.let { "$name: $it" } ?: name
                    /* Log success and not a filter */
                    logger.trace(" $nameText performed")
                }
            } catch (e: Exception) {
                val logger = if (action.filter) FILTER_LOGGER else ACTION_LOGGER
                val nameText = action.name?.let { "$name: $it" } ?: name
                logger.error("$nameText (${event.eventType} was valid, but failed (${e.localizedMessage})")
                throw e
            }
        }
    }

    protected open fun <E : Event> preInvokeCheck(action: Action<E>, event: E) = action.canHandleEvent(event.eventType) && testChecksForInheritedEventTypes(event)

    fun createHandler(): EventHandler<Event> {
        return EventHandler {
            var type: EventType<*>? = it.eventType
            while (type != null) {
                actionHandlerMap[type]?.let { handlers ->
                    handlers.forEach { handler ->
                        if (!it.isConsumed) {
                            handler.handle(it)
                        }
                    }
                }
                type = type.superType
            }
        }
    }

    companion object {
        private val ACTION_LOGGER: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().name)
        private val FILTER_LOGGER: Logger = LoggerFactory.getLogger("${MethodHandles.lookup().lookupClass().name}-Filter")

        @JvmStatic
        fun Node.installActionSet(actionSet: ActionSet) {
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
        }

        @JvmStatic
        fun Window.installActionSet(actionSet: ActionSet) {
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
        }
    }
}
