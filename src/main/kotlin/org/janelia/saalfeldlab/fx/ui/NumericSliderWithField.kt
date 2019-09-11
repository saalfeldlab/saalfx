package org.janelia.saalfeldlab.fx.ui

import javafx.scene.control.Slider
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import org.janelia.saalfeldlab.fx.util.DoubleStringFormatter
import kotlin.math.roundToInt

class NumericSliderWithField @JvmOverloads constructor(
        min: Double,
        max: Double,
        initialValue: Double,
        numDecimals: Int = 2,
        isInteger: Boolean = false) {

    constructor(
            min: Int,
            max: Int,
            initialValue: Int) : this(min.toDouble(), max.toDouble(), initialValue.toDouble(), 0, true)

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
