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

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.*
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.janelia.saalfeldlab.fx.SaalFxStyle
import org.janelia.saalfeldlab.fx.extensions.addTriggeredListener
import org.janelia.saalfeldlab.fx.extensions.nonnull
import java.util.function.DoublePredicate
import java.util.function.IntPredicate
import java.util.function.LongPredicate

class SpatialField<P : Property<Number>> private constructor(
	val x: NumberField<P>,
	val y: NumberField<P>,
	val z: NumberField<P>,
	textFieldWidth: Double = Region.USE_COMPUTED_SIZE
) {

	val node
		get() = makeNode()
	val editableProperty = SimpleBooleanProperty(true)
	var editable: Boolean by editableProperty.nonnull()
	private val showHeaderProperty = SimpleBooleanProperty(false)
	var showHeader: Boolean by showHeaderProperty.nonnull()
	val headerTextProperty = SimpleStringProperty("")
	var headerText: String by headerTextProperty.nonnull()

	init {
		x.textField.promptText = "X"
		y.textField.promptText = "Y"
		z.textField.promptText = "Z"
		x.textField.prefWidth = textFieldWidth
		y.textField.prefWidth = textFieldWidth
		z.textField.prefWidth = textFieldWidth

		listOf(x, y, z).forEach { it.textField.editableProperty().bind(editableProperty) }
	}

	private fun makeNode(): Node {
		val header = createHeader()
		val fieldsHeader = createFieldsHeader()
		val fields = HBox(x.textField, y.textField, z.textField)
		VBox.setVgrow(header, Priority.ALWAYS)
		VBox.setVgrow(fieldsHeader, Priority.ALWAYS)
		VBox.setVgrow(fields, Priority.ALWAYS)
		return VBox(header, fieldsHeader, fields)
	}

	private fun createHeader() = VBox().apply {
		children += Label().apply {
			showHeaderProperty.addTriggeredListener { _, _, show -> showHeader(show) }
			alignment = Pos.BOTTOM_CENTER
			isFillWidth = true
			padding = Insets(0.0, 0.0, 3.0, 0.0)
		}
		isFillWidth = true
		alignment = Pos.BOTTOM_CENTER
	}

	private fun Label.showHeader(show: Boolean) {
		if (show && headerText.isNotEmpty()) {
			visibleProperty().set(true)
			managedProperty().set(true)
			textProperty().bind(headerTextProperty)
		} else {
			textProperty().unbind()
			textProperty().set("")
			visibleProperty().set(false)
			managedProperty().set(false)
		}
	}

	private fun createFieldsHeader(): HBox {
		val labels = arrayOf(
			VBox(Label(x.textField.promptText)),
			VBox(Label(y.textField.promptText)),
			VBox(Label(z.textField.promptText))
		)
		val header = HBox(*labels)
		header.alignment = Pos.BOTTOM_CENTER
		header.padding = Insets(0.0, 0.0, 3.0, 0.0)
		labels.forEach {
			HBox.setHgrow(it, Priority.ALWAYS)
			it.isFillWidth = true
			it.alignment = Pos.BOTTOM_CENTER
		}
		header.visibleProperty().bind(showHeaderProperty)
		header.managedProperty().bind(showHeaderProperty)
		return header
	}


	fun setValues(x: Number = this.x.value, y: Number = this.y.value, z: Number = this.z.value) {
		this.x.value = x
		this.y.value = y
		this.z.value = z
	}

	fun asLongArray() = LongArray(3).apply {
		this[0] = x.value.toLong()
		this[1] = y.value.toLong()
		this[2] = z.value.toLong()
	}

	fun asIntArray() = IntArray(3).apply {
		this[0] = x.value.toInt()
		this[1] = y.value.toInt()
		this[2] = z.value.toInt()
	}

	fun asDoubleArray() = DoubleArray(3).apply {
		this[0] = x.value.toDouble()
		this[1] = y.value.toDouble()
		this[2] = z.value.toDouble()
	}

	companion object {

		@JvmStatic
		fun doubleField(
			initialValue: Double,
			test: DoublePredicate,
			textFieldWidth: Double = Region.USE_COMPUTED_SIZE,
			vararg submitOn: ObjectField.SubmitOn
		): SpatialField<DoubleProperty> {
			return SpatialField(
				NumberField.doubleField(initialValue, test, *submitOn),
				NumberField.doubleField(initialValue, test, *submitOn),
				NumberField.doubleField(initialValue, test, *submitOn),
				textFieldWidth
			)
		}

		@JvmStatic
		fun intField(
			initialValue: Int,
			test: IntPredicate,
			textFieldWidth: Double = Region.USE_COMPUTED_SIZE,
			vararg submitOn: ObjectField.SubmitOn
		): SpatialField<IntegerProperty> {
			return SpatialField(
				NumberField.intField(initialValue, test, *submitOn),
				NumberField.intField(initialValue, test, *submitOn),
				NumberField.intField(initialValue, test, *submitOn),
				textFieldWidth
			)
		}

		@JvmStatic
		fun longField(
			initialValue: Long,
			test: LongPredicate,
			textFieldWidth: Double = Region.USE_COMPUTED_SIZE,
			vararg submitOn: ObjectField.SubmitOn
		): SpatialField<LongProperty> {
			return SpatialField(
				NumberField.longField(initialValue, test, *submitOn),
				NumberField.longField(initialValue, test, *submitOn),
				NumberField.longField(initialValue, test, *submitOn),
				textFieldWidth
			)
		}


		@JvmStatic
		fun main(args: Array<String>) {
			Platform.startup { }

			val xyz = SpatialField.doubleField(1.0, { true})
			xyz.showHeader = true

			val abc = SpatialField.doubleField(1.0, { true})
			abc.headerText = "abc"
			abc.editable = false
			abc.x.textField.promptText = "A"
			abc.y.textField.promptText = "B"
			abc.z.textField.promptText = "C"
			abc.showHeader = true

			val one23 = SpatialField.doubleField(1.0, { true})
			one23.headerText = "one23"
			one23.x.textField.editableProperty().let {
				it.unbind()
				it.value = false
			}
			one23.y.textField.editableProperty().let {
				it.unbind()
				it.value = true
			}
			one23.z.textField.editableProperty().let {
				it.unbind()
				it.value = false
			}
			one23.x.textField.promptText = "A"
			one23.y.textField.promptText = "B"
			one23.z.textField.promptText = "C"
			one23.showHeader = true

			val df = NumberField.doubleField(
				5.0,
				{ _ -> true },
				ObjectField.SubmitOn.ENTER_PRESSED,
				ObjectField.SubmitOn.FOCUS_LOST
			)
			val lbl1 = TextField()
			val converted1 = Bindings.convert(df.valueProperty())
			lbl1.textProperty().bind(converted1)
			val hb1 = HBox(df.textField, lbl1)

			val lf = NumberField.longField(
				4,
				{ _ -> true },
				ObjectField.SubmitOn.ENTER_PRESSED,
				ObjectField.SubmitOn.FOCUS_LOST
			)
			val lbl2 = TextField()
			val converted2 = Bindings.convert(lf.valueProperty())
			lbl2.textProperty().bind(converted2)
			val hb2 = HBox(lf.textField, lbl2)

			val ulf = NumberField.longField(
				4,
				{ d -> d >= 0 },
				ObjectField.SubmitOn.ENTER_PRESSED,
				ObjectField.SubmitOn.FOCUS_LOST
			)
			val lbl3 = TextField()
			val converted3 = Bindings.convert(ulf.valueProperty())
			lbl3.textProperty().bind(converted3)
			val hb3 = HBox(ulf.textField, lbl3)

			Platform.runLater {
				val pane = VBox(
					abc.node,
					xyz.node,
					one23.node,
					hb1,
					hb2,
					hb3)
				val scene = Scene(pane)
				SaalFxStyle.registerStylesheets(scene)
				val stage = Stage()
				stage.scene = scene
				stage.show()
			}
		}
	}
}
