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
