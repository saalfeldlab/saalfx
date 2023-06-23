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

import javafx.scene.control.Slider
import javafx.scene.control.TextField
import org.janelia.saalfeldlab.fx.util.DoubleStringFormatter
import kotlin.math.roundToInt

class NumericSliderWithField @JvmOverloads constructor(
	min: Double,
	max: Double,
	initialValue: Double,
	numDecimals: Int = 2,
	isInteger: Boolean = false
) {

	constructor(
		min: Int,
		max: Int,
		initialValue: Int
	) : this(min.toDouble(), max.toDouble(), initialValue.toDouble(), 0, true)

	constructor(
		min: Long,
		max: Long,
		initialValue: Long
	) : this(min.toDouble(), max.toDouble(), initialValue.toDouble(), 0, true)

	val slider: Slider

	private val field: TextField
	val textField: TextField
		get() = this.field

	init {

		assert(initialValue >= min)
		assert(initialValue <= max)

		this.slider = Slider(min, max, initialValue)
		this.field = TextField(initialValue.toString())

		this.slider.isShowTickLabels = true

		val formatter = DoubleStringFormatter.createFormatter(
			slider.minProperty(),
			slider.maxProperty(),
			initialValue,
			numDecimals
		)
		this.field.textFormatter = formatter
		formatter.valueProperty().addListener { _, _, newv -> this.slider.value = newv!! }
		this.slider.valueProperty().addListener { _, _, newv -> formatter.setValue(newv.toDouble()) }
		if (isInteger)
			this.slider.valueProperty().addListener { _, _, newv -> this.slider.value = newv.toDouble().roundToInt().toDouble() }

	}

	@Deprecated("Use getter syntax insead", ReplaceWith("getSlider()"))
	fun slider() = slider

	@Deprecated("Use getter syntax insead", ReplaceWith("getTextField()"))
	fun textField() = textField

}
