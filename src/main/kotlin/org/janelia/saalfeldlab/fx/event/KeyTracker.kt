package org.janelia.saalfeldlab.fx.event

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Window
import org.janelia.saalfeldlab.fx.util.OnWindowInitListener
import java.util.function.Consumer

class KeyTracker : InstallAndRemove<Scene> {

    private val activeKeys = mutableSetOf<KeyCode>()

    private val activate = ActivateKey()

    private val deactivate = DeactivateKey()

    private val onFocusChanged = OnFocusChanged()

    fun installInto(window: Window) = installInto(window.scene, window)

    fun installInto(scene: Scene, window: Window) {
        scene.addEventFilter(KeyEvent.KEY_RELEASED, deactivate)
        scene.addEventFilter(KeyEvent.KEY_PRESSED, activate)
        window.focusedProperty().addListener(onFocusChanged)
    }

    fun removeFrom(scene: Scene, window: Window) {
        scene.removeEventFilter(KeyEvent.KEY_RELEASED, deactivate)
        scene.removeEventFilter(KeyEvent.KEY_PRESSED, activate)
        window.focusedProperty().removeListener(onFocusChanged)
    }

    @Deprecated("Install into scene and window directly instead", ReplaceWith("installInto(scene, window)"))
    override fun installInto(t: Scene) {
        t.addEventFilter(KeyEvent.KEY_RELEASED, deactivate)
        t.addEventFilter(KeyEvent.KEY_PRESSED, activate)
        t.windowProperty().addListener(OnWindowInitListener(Consumer { window -> window.focusedProperty().addListener(onFocusChanged) }))
    }

    @Deprecated("Remove from scene and window directly instead", ReplaceWith("removeFrom(scene, window)"))
    override fun removeFrom(t: Scene) {
        t.removeEventFilter(KeyEvent.KEY_PRESSED, activate)
        t.removeEventFilter(KeyEvent.KEY_RELEASED, deactivate)
        t.window?.focusedProperty()?.removeListener(onFocusChanged)
    }

    private inner class ActivateKey : EventHandler<KeyEvent> {
        override fun handle(event: KeyEvent) {
            synchronized(activeKeys) {
                activeKeys.add(event.code)
            }
        }
    }

    private inner class DeactivateKey : EventHandler<KeyEvent> {
        override fun handle(event: KeyEvent) {
            synchronized(activeKeys) {
                activeKeys.remove(event.code)
            }
        }
    }

    private inner class OnFocusChanged : ChangeListener<Boolean> {

        override fun changed(observable: ObservableValue<out Boolean>, oldValue: Boolean?, newValue: Boolean?) {
            newValue?.let {
                if (!newValue)
                    synchronized(activeKeys) {
                        activeKeys.clear()
                    }
            }
        }

    }

    fun areOnlyTheseKeysDown(vararg codes: KeyCode): Boolean {
        val codesHashSet = mutableSetOf(*codes)
        synchronized(activeKeys) {
            return codesHashSet == activeKeys
        }
    }

    fun areKeysDown(vararg codes: KeyCode): Boolean {
        synchronized(activeKeys) {
            return activeKeys.containsAll(listOf(*codes))
        }
    }

    fun activeKeyCount(): Int {
        synchronized(activeKeys) {
            return activeKeys.size
        }
    }

    fun noKeysActive() = activeKeyCount() == 0

    fun getActiveKeyCodes(includeModifiers: Boolean) = synchronized(activeKeys) {
        activeKeys.filter { includeModifiers || !it.isModifierKey }
    }

}
