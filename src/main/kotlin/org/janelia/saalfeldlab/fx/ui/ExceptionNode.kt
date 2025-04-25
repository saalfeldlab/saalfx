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


import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.TextArea
import org.apache.commons.lang.exception.ExceptionUtils
import org.janelia.saalfeldlab.fx.ui.ExceptionNode.Companion.exceptionDialog
import org.janelia.saalfeldlab.fx.util.InvokeOnJavaFXApplicationThread

class ExceptionNode(e: Exception) : TextArea() {

	init {
		text = ExceptionUtils.getStackTrace(e)
		prefColumnCount = (prefColumnCount * 1.5).toInt()
		prefRowCount = prefRowCount * 2
		isEditable = false
		isWrapText = false
	}

	companion object {

		@JvmStatic
		fun exceptionDialog(e: Exception): Dialog<Exception> {
			return Dialog<Exception>().apply {
				title = "Caught Exception"
				dialogPane.content = ExceptionNode(e)
				dialogPane.buttonTypes.setAll(ButtonType.OK)
				isResizable = true
			}
		}
	}
}

fun main() {
	InvokeOnJavaFXApplicationThread {
		exceptionDialog(RuntimeException("Some exception")).showAndWait()
	}
}