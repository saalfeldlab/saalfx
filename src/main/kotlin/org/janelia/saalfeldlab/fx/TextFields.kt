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
package org.janelia.saalfeldlab.fx

import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent

class TextFields {
	companion object {
		@JvmStatic
		@JvmOverloads
		fun editableOnDoubleClick(text: String? = null): TextField {
			val field = TextField(text).also { it.isEditable = false }
			val initialValue = SimpleStringProperty()

			val setEditable = { initialValue.value = field.text; field.isEditable = true }
			val setText = { newText: String? -> field.isEditable = false; field.text = newText;}

			field.addEventHandler(MouseEvent.MOUSE_PRESSED) {
				if (it.clickCount == 2 && !field.isEditable) {
					it.consume()
					setEditable()
				}
			}

			field.addEventHandler(KeyEvent.KEY_PRESSED) {
				if (ESCAPE_COMBINATION.match(it) && field.isEditable) {
					it.consume()
					setText(initialValue.value)
				} else if (ENTER_COMBINATION.match(it) && field.isEditable) {
					it.consume()
					setText(field.text)
				} else if (ENTER_COMBINATION.match(it) && !field.isEditable) {
					it.consume()
					setEditable()
				}
			}

			field.focusedProperty().addListener { _,_, new -> if (!new && field.isEditable) setText(initialValue.value) }

			return field

		}

		private val ENTER_COMBINATION = KeyCodeCombination(KeyCode.ENTER)

		private val ESCAPE_COMBINATION = KeyCodeCombination(KeyCode.ESCAPE)
	}
}
