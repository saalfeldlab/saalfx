package org.janelia.saalfeldlab.fx.actions

import javafx.event.EventType
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import java.util.function.Consumer

/**
 * Convenient subclass of [Action] for [MouseEvent]s.
 *
 * By default, will [ignoreKeys]
 *
 * @constructor create the [MouseAction]
 *
 * @param eventType
 */
class MouseAction(eventType: EventType<MouseEvent>) : Action<MouseEvent>(eventType) {

    init {
        ignoreKeys()
    }

    /**
     * Verify that ther event triggering this action was caused by a button trigger. Either Click or Release.
     *
     * @param trigger the mosue button that must trigger this action to be valid
     * @param released whether to check if [trigger] should be relased, or clicked
     * @param exclusive if no other [MouseButton]s other than [trigger] can be pressed for this action to be valid
     */
    @JvmOverloads
    fun verifyButtonTrigger(trigger: MouseButton, released: Boolean = false, exclusive: Boolean = false) {
        /* If a trigger is required, check if it was correct, on either press or release */
        var expected = ""
        val desc = "$trigger was ${if (released) "Released" else "Pressed"}"
        verify(desc) { mouseEvent ->
            mouseEvent?.let {
                if (released) {
                    it.wasButtonReleased(trigger).also { release ->
                        if (!release) expected = "expected $trigger to be released, but wasn't"
                    }
                } else {
                    (it.button == trigger).also { match -> if (!match) expected = "expected $trigger but button was ${it.button}" }
                }.also { valid ->
                    if (!valid) logger.trace("button trigger was not valid: $expected")
                }
            } ?: true
        }
        /* If the mouse down buttons are exclusive, ensure no other buttons are pressed */
        if (exclusive) {
            verify(" Only $trigger Was Active") { mouseEvent ->
                mouseEvent?.let { event ->
                    MouseButton.values()
                        .filter { it != trigger && it != MouseButton.NONE }
                        .map { !event.isButtonDown(it) }
                        .reduce { l, r -> l && r }.also {
                            if (!it) logger.trace("expected only $trigger but other mouse buttons were down")
                        }
                } ?: true
            }
        }
    }

    /**
     * Verify that [buttons] are held down.
     *
     * @param buttons that must be held down for this action to be valid
     * @param exclusive if no [MouseButton]s other than [buttons] can be held down for the action to be valid
     */
    @JvmOverloads
    fun verifyButtonsDown(vararg buttons: MouseButton, exclusive: Boolean = false) {
        /* Check if required keys are down */
        if (buttons.isNotEmpty()) {
            verify { mouseEvent ->
                mouseEvent?.let { event ->
                    buttons.map { event.isButtonDown(it) }
                        .reduce { l, r -> l && r }.also {
                            if (!it) logger.trace("expected buttons ${buttons.contentToString()} to be down, but some were not. ")
                        }
                } ?: true
            }
        }
        /* If the mouse down buttons are exclusive, ensure no other buttons are pressed */
        if (exclusive) {
            verify { mouseEvent ->
                mouseEvent?.let { event ->
                    MouseButton.values()
                        .filter { it !in buttons }
                        .map { !event.isButtonDown(it) }
                        .reduce { l, r -> l && r }.also {
                            if (!it) logger.trace("expected only buttons ${buttons.contentToString()} to be down, but other buttons were down also. ")
                        }
                } ?: true
            }
        }
    }

    private fun MouseEvent.isButtonDown(mouseButton: MouseButton) = when (mouseButton) {
        MouseButton.NONE -> true
        MouseButton.PRIMARY -> isPrimaryButtonDown
        MouseButton.MIDDLE -> isMiddleButtonDown
        MouseButton.SECONDARY -> isSecondaryButtonDown
        MouseButton.BACK -> isBackButtonDown
        MouseButton.FORWARD -> isForwardButtonDown
    }

    private fun MouseEvent.wasButtonReleased(mouseButton: MouseButton) = button == mouseButton && !isButtonDown(mouseButton)

    companion object {


        /**
         * Create a [MouseAction] to trigger on [EventType] [T]
         *
         * @param T EventType for the [MouseAction] being created to trigger on
         * @param action callback to configure the created [MouseAction]
         * @receiver the [EventType] to trigger this [MouseAction] on
         */
        @JvmSynthetic
        fun <T : EventType<MouseEvent>> T.action(action: Action<MouseEvent>.() -> Unit) = MouseAction(this).also {
            it.action()
        }

        /**
         * Create a [MouseAction] to trigger on [EventType] [T] with no configuration, other than [onAction] defining the trigger callback.
         *
         * @param T EventType for the [MouseAction] being created to trigger on
         * @param onAction callback to be triggered when this [MouseAction] is valid
         * @receiver the [EventType] to trigger this [MouseAction] on
         */
        @JvmSynthetic
        fun <T : EventType<MouseEvent>> T.onAction(onAction: (MouseEvent?) -> Unit) = MouseAction(this).also { action ->
            action.onAction { onAction(it) }
        }

        @JvmStatic
        fun <T : EventType<MouseEvent>> T.onAction(onAction: Consumer<MouseEvent?>) = onAction { onAction.accept(it) }
    }
}
