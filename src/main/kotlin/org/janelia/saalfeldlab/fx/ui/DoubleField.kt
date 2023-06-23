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
package org.janelia.saalfeldlab.fx.ui

import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.Event
import javafx.event.EventHandler
import javafx.scene.control.TextField
import javafx.util.StringConverter

class DoubleField(initialValue: Double) {
	private val field = TextField()

	private val valueAsString = SimpleStringProperty()

	private val value = SimpleDoubleProperty()

	init {

		valueAsString.addListener { obs, oldv, newv -> this.field.text = newv }

		valueAsString.bindBidirectional(value, object : StringConverter<Number>() {
			override fun toString(value: Number): String {
				return value.toString()
			}

			override fun fromString(string: String): Double? {
				try {
					return java.lang.Double.valueOf(string)
				} catch (e: NumberFormatException) {
					return value.get()
				} catch (e: NullPointerException) {
					return value.get()
				}

			}
		})

		this.value.set(initialValue)

		this.field.onAction = wrapAsEventHandler(Runnable { this.submitText() })
		this.field.focusedProperty().addListener { obs, oldv, newv -> submitText(!newv) }

	}

	fun textField(): TextField {
		return this.field
	}

	fun valueProperty(): DoubleProperty {
		return this.value
	}

	private fun <E : Event> wrapAsEventHandler(r: Runnable): EventHandler<E> {
		return wrapAsEventHandler(r, true)
	}

	private fun <E : Event> wrapAsEventHandler(r: Runnable, consume: Boolean): EventHandler<E> {
		return EventHandler { e ->
			if (consume)
				e.consume()
			r.run()
		}
	}

	private fun submitText(submit: Boolean = true) {
		if (!submit)
			return
		try {
			val `val` = java.lang.Double.valueOf(textField().text)
			value.set(`val`)
		} catch (e: NumberFormatException) {
			this.field.text = this.valueAsString.get()
		} catch (e: NullPointerException) {
			this.field.text = this.valueAsString.get()
		}

	}
}
