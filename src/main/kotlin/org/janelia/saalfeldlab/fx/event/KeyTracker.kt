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
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.KeyEvent.KEY_RELEASED
import javafx.stage.Window
import org.janelia.saalfeldlab.fx.actions.ActionSet
import org.janelia.saalfeldlab.fx.actions.ActionSet.Companion.installActionSet
import org.janelia.saalfeldlab.fx.actions.ActionSet.Companion.removeActionSet

class KeyTracker {

	private val activeKeys = mutableSetOf<KeyCode>()

	private val actions by lazy {
		ActionSet("Key Tracker", { this }) {
			KEY_PRESSED {
				ignoreKeys()
				filter = true
				consume = false
				verifyEventNotNull()
				onAction { addKey(it!!.code) }
			}
			KEY_RELEASED {
				ignoreKeys()
				filter = true
				consume = false
				verifyEventNotNull()
				onAction { removeKey(it!!.code) }
			}
		}
	}

	val clearOnUnfocused = ChangeListener<Boolean> { _, _, isFocused ->
		if (isFocused) {
			activeKeys.clear()
		}
	}

	fun installInto(window: Window) {
		window.installActionSet(actions)
		window.focusedProperty().addListener(clearOnUnfocused)
	}

	fun removeFrom(window: Window) {
		window.removeActionSet(actions)
		window.focusedProperty().removeListener(clearOnUnfocused)
	}

	fun areOnlyTheseKeysDown(vararg codes: KeyCode) = activeKeys.synchronized { mutableSetOf(*codes) == this }

	fun areKeysDown(vararg codes: KeyCode) = activeKeys.synchronized { containsAll(listOf(*codes)) }

	fun activeKeyCount() = activeKeys.synchronized { size }

	fun noKeysActive() = activeKeyCount() == 0

	fun getActiveKeyCodes(includeModifiers: Boolean) = activeKeys.synchronized { filter { includeModifiers || !it.isModifierKey } }

	fun addKey(key: KeyCode) = activeKeys.synchronized { add(key) }

	fun removeKey(key: KeyCode) = activeKeys.synchronized { remove(key) }

	private fun <R> MutableSet<KeyCode>.synchronized(run: MutableSet<KeyCode>.() -> R): R {
		synchronized(this) {
			return run(this)
		}
	}

	companion object {
		@JvmStatic
		fun keysToString(vararg keys: KeyCode): String {
			val codes = mutableListOf<KeyCode>()
			val modifiers = mutableListOf<KeyCode>()

			keys.forEach { key ->
				if (key.isModifierKey) {
					modifiers += key
				} else {
					codes += key
				}
			}
			val orderedKeys = mutableListOf<KeyCode>()
			orderedKeys += modifiers
			orderedKeys += codes
			return orderedKeys.map { it.name }.reduce { l, r -> "$l + $r" }
		}
	}

}
