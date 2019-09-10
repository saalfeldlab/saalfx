package org.janelia.saalfeldlab.fx.util

import javafx.scene.control.TextFormatter
import java.util.function.UnaryOperator

class DoubleFilter : UnaryOperator<TextFormatter.Change?> {

    private val validEditingState = "-?(([1-9][0-9]*)|0)?(\\.[0-9]*)?".toRegex()

    override fun apply(c: TextFormatter.Change?) = c?.controlNewText?.let { if (validEditingState.matches(it)) c else null }

}
