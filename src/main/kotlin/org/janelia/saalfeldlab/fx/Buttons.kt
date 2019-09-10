package org.janelia.saalfeldlab.fx

import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.control.Tooltip

class Buttons {

    companion object {

        @JvmStatic
        fun withTooltip(labelText: String, handler: EventHandler<ActionEvent>): Button {
            return withTooltip(labelText, labelText, handler)
        }

        @JvmStatic
        fun withTooltip(
                labelText: String,
                tooltipText: String,
                handler: EventHandler<ActionEvent>): Button {
            val button = Button(labelText)
            button.tooltip = Tooltip(tooltipText)
            button.onAction = handler
            return button
        }
    }

}
