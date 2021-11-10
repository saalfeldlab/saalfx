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
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Window

class KeyTracker : InstallAndRemove<Window> {

    private val activeKeys = mutableSetOf<KeyCode>()

    private val activateKeyHandler = EventHandler<KeyEvent> { activeKeys.synchronized { add(it.code) } }

    private val deactivateKeyHandler = EventHandler<KeyEvent> { activeKeys.synchronized { remove(it.code) } }

    private val onFocusChanged = ChangeListener<Boolean?> { _, _, new -> new?.let { activeKeys.synchronized { clear() } } }

    override fun installInto(t: Window) = installInto(t.scene, t)
    override fun removeFrom(t: Window) = removeFrom(t.scene, t)

    fun installInto(scene: Scene, window: Window) {
        scene.addEventFilter(KeyEvent.KEY_RELEASED, deactivateKeyHandler)
        scene.addEventFilter(KeyEvent.KEY_PRESSED, activateKeyHandler)
        window.focusedProperty().addListener(onFocusChanged)
    }

    fun removeFrom(scene: Scene, window: Window) {
        scene.removeEventFilter(KeyEvent.KEY_RELEASED, deactivateKeyHandler)
        scene.removeEventFilter(KeyEvent.KEY_PRESSED, activateKeyHandler)
        window.focusedProperty().removeListener(onFocusChanged)
    }

    fun areOnlyTheseKeysDown(vararg codes: KeyCode) = activeKeys.synchronized { mutableSetOf(*codes) == this }

    fun areKeysDown(vararg codes: KeyCode) = activeKeys.synchronized { containsAll(listOf(*codes)) }

    fun activeKeyCount() = activeKeys.synchronized { size }

    private fun <R> MutableSet<KeyCode>.synchronized(run: MutableSet<KeyCode>.() -> R): R {
        synchronized(this) {
            return run(this)
        }
    }

    fun noKeysActive() = activeKeyCount() == 0

    fun getActiveKeyCodes(includeModifiers: Boolean) = activeKeys.synchronized { filter { includeModifiers || !it.isModifierKey } }

}
