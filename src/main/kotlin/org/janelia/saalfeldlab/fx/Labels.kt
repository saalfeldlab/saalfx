package org.janelia.saalfeldlab.fx

import javafx.scene.control.Label
import javafx.scene.control.Tooltip

class Labels {

    companion object {
        @JvmOverloads
        @JvmStatic
        fun withTooltip(labelText: String, tooltipText: String = labelText): Label {
            val label = Label(labelText)
            label.tooltip = Tooltip(tooltipText)
            return label
        }
    }

}
