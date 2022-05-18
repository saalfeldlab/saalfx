package org.janelia.saalfeldlab.fx.actions

import javafx.event.EventType
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import java.util.function.Consumer

class MouseAction(eventType: EventType<MouseEvent>) : Action<MouseEvent>(eventType) {

    init {
        ignoreKeys()
    }

    @JvmOverloads
    fun verifyButtonTrigger(trigger: MouseButton, released: Boolean = false, exclusive: Boolean = false) {
        /* If a trigger is required, check if it was correct, on either press or release */
        verify {
            if (released) {
                it.wasButtonReleased(trigger)
            } else {
                it.button == trigger
            }
        }
        /* If the mouse down buttons are exclusive, ensure no other buttons are pressed */
        if (exclusive) {
            verify { event ->
                MouseButton.values()
                    .filter { it != trigger && it != MouseButton.NONE }
                    .map { !event.isButtonDown(it) }
                    .reduce { l, r -> l && r }
            }
        }
    }

    @JvmOverloads
    fun verifyButtonsDown(vararg buttons: MouseButton, exclusive: Boolean = false) {
        /* Check if required keys are down */
        if (buttons.isNotEmpty()) {
            verify { event ->
                buttons.map { event.isButtonDown(it) }
                    .reduce { l, r -> l && r }
            }
        }
        /* If the mouse down buttons are exclusive, ensure no other buttons are pressed */
        if (exclusive) {
            verify { event ->
                MouseButton.values()
                    .filter { it !in buttons }
                    .map { !event.isButtonDown(it) }
                    .reduce { l, r -> l && r }
            }
        }
    }

    fun MouseEvent.isButtonDown(mouseButton: MouseButton) = when (mouseButton) {
        MouseButton.NONE -> true
        MouseButton.PRIMARY -> isPrimaryButtonDown
        MouseButton.MIDDLE -> isMiddleButtonDown
        MouseButton.SECONDARY -> isSecondaryButtonDown
        MouseButton.BACK -> isBackButtonDown
        MouseButton.FORWARD -> isForwardButtonDown
    }

    fun MouseEvent.wasButtonReleased(mouseButton: MouseButton) = button == mouseButton && !isButtonDown(mouseButton)

    companion object {


        @JvmSynthetic
        fun <T : EventType<MouseEvent>> T.action(action: Action<MouseEvent>.() -> Unit) = MouseAction(this).also {
            it.action()
        }

        @JvmSynthetic
        fun <T : EventType<MouseEvent>> T.onAction(onAction: (MouseEvent) -> Unit) = MouseAction(this).also { action ->
            action.onAction { onAction(it) }
        }

        @JvmStatic
        fun <T : EventType<MouseEvent>> T.onAction(onAction: Consumer<MouseEvent>) = onAction { onAction.accept(it) }
    }
}
