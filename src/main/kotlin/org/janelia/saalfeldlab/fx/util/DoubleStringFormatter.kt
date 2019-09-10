package org.janelia.saalfeldlab.fx.util

import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.control.TextFormatter

class DoubleStringFormatter {

    companion object {

        @JvmStatic
        fun createFormatter(
                initialValue: Double,
                numDecimals: Int): TextFormatter<Double> {
            return createFormatter(
                    SimpleDoubleProperty(java.lang.Double.NEGATIVE_INFINITY),
                    SimpleDoubleProperty(java.lang.Double.POSITIVE_INFINITY),
                    initialValue,
                    numDecimals
            )
        }

        @JvmStatic
        fun createFormatter(
                min: DoubleProperty,
                max: DoubleProperty,
                initialValue: Double,
                numDecimals: Int): TextFormatter<Double> {
            val filter = DoubleFilter()
            val converter = DoubleStringConverter(numDecimals, min, max)
            return TextFormatter(converter, initialValue, filter)
        }
    }

}
