package org.janelia.saalfeldlab.fx.actions

import javafx.event.EventType
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import java.util.function.Consumer

class KeyAction(eventType: EventType<KeyEvent>) : Action<KeyEvent>(eventType) {

    private var keysReleased: List<KeyCode>? = null

    fun keyMatchesBinding(keyBindings: NamedKeyCombination.CombinationMap, keyName: String) {
        if (name == null) name = keyName
        ignoreKeys()
        verify(eventType) { keyBindings.matches(keyName, it) }
    }

    fun keysReleased(vararg keyCodes: KeyCode) {
        ignoreKeys()
        keysReleased = listOf(*keyCodes)
    }

    override fun verifyKeys(event: KeyEvent): Boolean {
        return super.verifyKeys(event) && keysReleased?.let {
            // If we are checking a key on a release, we can't use the keyTracker
            eventType == KeyEvent.KEY_RELEASED && (event as? KeyEvent)?.code in keysReleased!!
        } ?: true
    }

    companion object {

        @JvmSynthetic
        fun <T : EventType<KeyEvent>> T.action(keyBindings: NamedKeyCombination.CombinationMap, keys: String, action: Action<KeyEvent>.() -> Unit) = KeyAction(this).also {
            it.keyMatchesBinding(keyBindings, keys)
            it.action()
        }

        @JvmSynthetic
        fun <T : EventType<KeyEvent>> T.onAction(keyBindings: NamedKeyCombination.CombinationMap, keys: String, onAction: (KeyEvent) -> Unit) = KeyAction(this).also { action ->
            action.keyMatchesBinding(keyBindings, keys)
            action.onAction { onAction(it) }
        }

        @JvmStatic
        fun <T : EventType<KeyEvent>> T.onAction(keyBindings: NamedKeyCombination.CombinationMap, keys: String, onAction: Consumer<KeyEvent>) =
            onAction(keyBindings, keys) { onAction.accept(it) }
    }
}
