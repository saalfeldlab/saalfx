package org.janelia.saalfeldlab.fx.actions

import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.EventType
import javafx.scene.Node
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import org.janelia.saalfeldlab.fx.actions.Action.Companion.action
import org.janelia.saalfeldlab.fx.actions.Action.Companion.installAction
import org.janelia.saalfeldlab.fx.actions.Action.Companion.onAction
import org.janelia.saalfeldlab.fx.actions.Action.Companion.removeAction
import org.janelia.saalfeldlab.fx.event.KeyTracker
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.function.Consumer

open class Action<E : Event>(val eventType: EventType<E>) {

    var name: String? = null
    var consume = true /* Indicates whether the event should be consumed if this Action is triggered*/
    var filter = false /* if false, this action is an event handler, if true, this is an event filter */
    var keysExclusive = false /* if true, all keys are required, and no other keys are allowed. If false, other keys are allowed */
    var keyTracker: KeyTracker? = null
    internal var allowDuringDrag = true
    var triggerIfDisabled = false /* Can be utilized by the called to block certain actions when the state of the application is disabled. */
    private var keysDown: List<KeyCode>? = listOf()

    private val checks = mutableListOf<(E) -> Boolean>()

    private var exceptionHandler: ((Exception) -> Unit)? = null

    private var action: (E) -> Unit = {}

    private var onException: (Exception) -> Unit = {}

    /**
     * Lazy reference to the [ActionEventHandler] created by this [Action]
     */
    val handler: EventHandler<E> by lazy { ActionEventHandler() }

    /**
     * Verify the check prior to triggering action
     *
     * @param check callback to verify the event of type  [E] is valid for this [Action].
     * @receiver
     */
    fun verify(check: (E) -> Boolean) {
        checks.add(check)
    }

    internal tailrec fun canHandleEvent(checkEventType: EventType<*>?): Boolean {
        checkEventType ?: return false
        return if (eventType == checkEventType) true else canHandleEvent(checkEventType.superType)
    }

    fun isValid(event: E): Boolean {
        return canHandleEvent(event.eventType) && verifyKeys(event) && testChecks(event)
    }

    /**
     * Verify that the expected key state matches the event.
     *
     * A call to [ignoreKeys] will cause this to always return true
     * An Action created with no keytracker will always fail, unless [ignoreKeys] is called.
     * Otherwise, the keytracker must match the expected [Action]'s [keysDown] and [keysExclusive]state
     *
     * @param event that we are verifying the keystate against
     * @return true onliy if keytracker keystate matches the event's keystate, or if [ignoreKeys] was called
     */
    protected open fun verifyKeys(event: E): Boolean {
        // only null if set intentionally, which is done if we don't care about keys
        if (keysDown == null) return true

        /* Three conditions to check;
         *  - If we expect no keys to be down
         *  - If ONLY the keys we expect are down
         *  - If AT LEAST the keys we expect are down  */
        return keyTracker?.run {
            when {
                keysDown!!.isEmpty() -> noKeysActive()
                keysExclusive -> areOnlyTheseKeysDown(*keysDown!!.toTypedArray())
                else -> areKeysDown(*keysDown!!.toTypedArray())
            }
        } ?: false

    }

    private fun testChecksForEventType(event: E, eventType: EventType<out Event> = event.eventType): Boolean {
        return checks[eventType]?.reduce { l, r -> { l(event) && r(event) } }?.invoke(event) ?: true
    }

    fun ignoreKeys() {
        keysDown = null
    }

    @JvmSynthetic
    fun onAction(handle: (E) -> Unit) {
        action = handle
    }

    fun onAction(handle: Consumer<E>) {
        action = { handle.accept(it) }
    }

    @JvmSynthetic
    fun handleException(handler: (Exception) -> Unit) {
        exceptionHandler = handler
    }

    fun handleException(handler: Consumer<Exception>) {
        exceptionHandler = { handler.accept(it) }
    }

    @JvmOverloads
    fun keysDown(vararg keyCodes: KeyCode, exclusive: Boolean = true) {
        keysExclusive = exclusive
        keysDown = listOf(*keyCodes)
    }

    fun verifyNoKeysDown() {
        keysDown()
    }


    operator fun invoke(event: E): Boolean {
        return if (!event.isConsumed && isValid(event)) {
            try {
                action(event)
            } catch (e: Exception) {
                exceptionHandler?.invoke(e) ?: throw e
            }
            if (consume) {
                event.consume()
            }
            true
        } else false
    }

    /**
     * Custom EventHandler used only to provide the name of the Action and delegate the [Action.invoke] as an [EventHandler]
     *
     * @constructor Create Action event handler
     */
    private inner class ActionEventHandler : EventHandler<E> {
        override fun handle(event: E) {
            this@Action(event)
        }

        override fun toString(): String {
            return name?.let {
                "${ActionEventHandler::class.java.simpleName}: $it"
            } ?: super.toString()
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().name)


        @Suppress("UNCHECKED_CAST")
        inline fun <reified T : EventType<E>, E : Event> T.action(noinline action: Action<E>.() -> Unit) =
            when (T::class.java) {
                KeyEvent::class.java -> KeyAction(this as EventType<KeyEvent>)
                MouseEvent::class.java -> MouseAction(this as EventType<MouseEvent>)
                else -> Action(this)
            }.apply { (this as Action<E>).action() } as Action<E>

        @Suppress("UNCHECKED_CAST")
        inline fun <reified T : EventType<E>, E : Event> T.onAction(name: String? = null, noinline onAction: (E) -> Unit): Action<E> =
            when (T::class.java) {
                KeyEvent::class.java -> KeyAction(this as EventType<KeyEvent>)
                MouseEvent::class.java -> MouseAction(this as EventType<MouseEvent>)
                else -> Action(this)
            }.apply {
                (this as Action<E>).apply {
                    this.name = name
                    onAction { onAction(it) }
                }
            } as Action<E>

        @JvmStatic
        fun <E : Event> Node.installAction(action: Action<E>) {
            if (action.filter) {
                addEventFilter(action.eventType, action.handler)
            } else {
                addEventHandler(action.eventType, action.handler)
            }
        }

        /**
         * Remove this [Action]s [EventHandler] from this [Node]. No effect if not installed.
         *
         * @param E the [Event] [action] should handle
         * @param action the [Action] to remove to this [Node]
         */
        @JvmStatic
        fun <E : Event> Node.removeAction(action: Action<E>) {
            if (action.filter) {
                removeEventFilter(action.eventType, action.handler)
            } else {
                removeEventHandler(action.eventType, action.handler)
            }
        }
    }
}
