package org.janelia.saalfeldlab.fx.ui

import java.util.Arrays

import javafx.scene.Node
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextArea
import javafx.scene.control.TitledPane
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox

class ExceptionNode(private val e: Exception) {

    val pane: Pane
        get() {
            val typePane = leftRight(Label("Type"), Label(e.javaClass.name))
            val messagePane = leftRight(Label("Message"), Label(e.message))
            val stackTrace = TitledPane("Stack Trace",fromStackTrace(e))
            stackTrace.isExpanded = false

            return VBox(typePane, messagePane, stackTrace)
        }

    companion object {

        private fun leftRight(left: Node, right: Node, leftDistance: Double = 0.0, rightDistance: Double = 0.0): AnchorPane {
            val pane = AnchorPane(left, right)
            AnchorPane.setLeftAnchor(left, leftDistance)
            AnchorPane.setRightAnchor(right, rightDistance)
            return pane
        }

        private fun fromStackTrace(e: Exception) = TextArea(e.stackTrace.joinToString("\n"))
                .also { it.isEditable = false }
                .also { it.isWrapText = false }

        @JvmStatic
        fun exceptionDialog(e: Exception): Dialog<Exception> {
            val d = Dialog<Exception>()
            val notify = ExceptionNode(e)
            d.title = "Caught Exception"
            d.dialogPane.graphic = notify.pane
            d.dialogPane.buttonTypes.setAll(ButtonType.OK)
            d.isResizable = true
            return d
        }
    }

}
