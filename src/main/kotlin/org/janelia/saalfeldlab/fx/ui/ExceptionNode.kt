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
