package org.janelia.saalfeldlab.fx

import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.control.Tooltip

class Buttons {

    companion object {

        fun withTooltip(
                labelText: String?,
                handler: (ActionEvent) -> Unit) = withTooltip(labelText, EventHandler { handler(it) })

        @JvmStatic
        fun withTooltip(
                labelText: String?,
                handler: EventHandler<ActionEvent>) = withTooltip(labelText, labelText, handler)

        fun withTooltip(
                labelText: String?,
                tooltipText: String?,
                handler: (ActionEvent) -> Unit) = withTooltip(labelText, tooltipText, EventHandler { handler(it) })

        @JvmStatic
        fun withTooltip(
                labelText: String?,
                tooltipText: String?,
                handler: EventHandler<ActionEvent>): Button {
            val button = Button(labelText)
            button.tooltip = Tooltip(tooltipText)
            button.onAction = handler
            return button
        }
    }

}
