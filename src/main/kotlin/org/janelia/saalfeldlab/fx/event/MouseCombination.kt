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

import com.sun.javafx.application.PlatformImpl
import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.input.*
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class MouseCombination private constructor(keyCombination: KeyCombination) {

	constructor(keyCode: KeyCode, vararg modifier: KeyCombination.Modifier): this(KeyCodeCombination(keyCode, *modifier))

	constructor(vararg modifier: KeyCombination.Modifier): this(OnlyModifierKeyCombination(*modifier))

	private val _keyCombination: ObjectProperty<KeyCombination> = SimpleObjectProperty(keyCombination)

	var keyCombination: KeyCombination
		get() = _keyCombination.value
		set(keyCombination) = setKeyCombinationChecked(keyCombination)

	private fun setKeyCombinationChecked(keyCombination: KeyCombination) {
		require(keyCombination is KeyCodeCombination || keyCombination is OnlyModifierKeyCombination) {
			"Currently only ${KeyCodeCombination::class} and ${OnlyModifierKeyCombination::class} are supported but got $keyCombination."
		}
		_keyCombination.value = keyCombination
	}

	fun match(event: MouseEvent, tracker: KeyTracker): Boolean {

		val keyCodes = tracker.getActiveKeyCodes(false);

		return if (keyCodes.size > 1) {
			false.also { LOG.trace("Mouse combinations with more than one non-modifier key are not supported.") }
		} else {
			val keyCode = if (keyCodes.size == 0) null else keyCodes[0]
			val keyEvent = KeyEvent(
					null,
					null,
					null,
					null,
					null,
					keyCode,
					event.isShiftDown,
					event.isControlDown,
					event.isAltDown,
					event.isMetaDown)
			keyCombination.match(keyEvent)
		}
	}

	val deepCopy: MouseCombination
		get() = MouseCombination(keyCombination)

	class OnlyModifierKeyCombination(vararg modifier: Modifier): KeyCombination(*modifier)

	companion object {
		private val LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
	}


}

fun main() {
	PlatformImpl.startup {  }
	Platform.runLater {
		val node = Button("DSLFKJSDLKFJSDLKGFJSDLGKDSJGSDG")


		val keyTracker = KeyTracker()
		val combination1 = MouseCombination(KeyCode.F, KeyCombination.CONTROL_DOWN)
		val combination2 = MouseCombination(KeyCombination.CONTROL_ANY)

		node.addEventHandler(MouseEvent.MOUSE_MOVED) {
			if (combination1.match(it, keyTracker)) {
				it.consume()
				println("MATCHED1!")
			}
		}
		node.addEventHandler(MouseEvent.MOUSE_MOVED) {
			if (combination2.match(it, keyTracker)) {
				it.consume()
				println("MATCHED2!")
			}
		}

		val stage = Stage().also { it.scene = Scene(node) }
		keyTracker.installInto(stage)
		stage.show()
	}
}
