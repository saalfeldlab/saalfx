/*-
 * #%L
 * Saalfeld lab JavaFX tools and extensions
 * %%
 * Copyright (C) 2019 Philipp Hanslovsky, Stephan Saalfeld
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
