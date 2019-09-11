package org.janelia.saalfeldlab.fx.ui

import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.stage.Stage

import java.io.PrintWriter
import java.io.StringWriter
import java.util.function.Consumer

class Exceptions {

    companion object {

        @JvmStatic
        fun exceptionAlert(
                title: String,
                headerText: String,
                e: Exception): Alert {
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = title
            alert.headerText = headerText
            alert.contentText = String.format("%s", e.message)

            // Create expandable Exception.
            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            e.printStackTrace(printWriter)
            val exceptionText = stringWriter.toString()

            val label = Label("Stack trace:")

            val textArea = TextArea(exceptionText)
            textArea.isEditable = false
            textArea.isWrapText = true

            textArea.maxWidth = java.lang.Double.MAX_VALUE
            textArea.maxHeight = java.lang.Double.MAX_VALUE
            GridPane.setVgrow(textArea, Priority.ALWAYS)
            GridPane.setHgrow(textArea, Priority.ALWAYS)

            val grid = GridPane()
            grid.maxWidth = java.lang.Double.MAX_VALUE
            grid.add(label, 0, 0)
            grid.add(textArea, 0, 1)

            // Set expandable Exception into the dialog pane.
            alert.dialogPane.expandableContent = grid

            // workaround to make resize work properly
            // https://stackoverflow.com/a/30805637/1725687
            alert.dialogPane.expandedProperty().addListener { l ->
                Platform.runLater {
                    alert.dialogPane.requestLayout()
                    val stage = alert.dialogPane.scene.window as Stage
                    stage.sizeToScene()
                }
            }

            return alert

        }

        @JvmStatic
        fun handler(
                title: String,
                headerText: String): Consumer<Exception> {
            return Consumer { e -> exceptionAlert(title, headerText, e) }
        }
    }

}
