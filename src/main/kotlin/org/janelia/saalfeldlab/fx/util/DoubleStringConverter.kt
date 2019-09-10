package org.janelia.saalfeldlab.fx.util

import javafx.beans.value.ObservableDoubleValue
import javafx.util.StringConverter
import kotlin.math.max
import kotlin.math.min

class DoubleStringConverter(
        numDecimals: Int,
        private val min: ObservableDoubleValue,
        private val max: ObservableDoubleValue) : StringConverter<Double>() {

    private val format: String? = String.format("%s%df", "%.", numDecimals)

    override fun fromString(s: String): Double? {
        return if (s.isEmpty() || "-" == s || "." == s || "-." == s)
            min(max(0.0, min.get()), max.get())
        else
            min(max(java.lang.Double.valueOf(s), min.get()), max.get())
    }

    override fun toString(d: Double?): String {
        return String.format(format!!, d)
    }

}
