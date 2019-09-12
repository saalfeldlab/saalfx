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

import javafx.beans.property.Property
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.stage.DirectoryChooser
import java.io.File
import java.util.function.Predicate

class DirectoryField(initialFile: File, browseButtonWidth: Double) {

    private val chooser = DirectoryChooser()

    private val directory: ObjectField<File, Property<File>>

    private val browseButton = Button("Browse")

    private val contents: HBox

    constructor(initialFile: String, browseButtonWidth: Double) : this(File(initialFile), browseButtonWidth) {}

    init {
        this.directory = ObjectField.fileField(initialFile, Predicate { it.exists() && it.isDirectory() }, *ObjectField.SubmitOn.values())
        chooser.initialDirectoryProperty().bind(this.directory.valueProperty())
        browseButton.prefWidth = browseButtonWidth
        HBox.setHgrow(directory.textField(), Priority.ALWAYS)
        this.contents = HBox(this.directory.textField(), browseButton)
        this.browseButton.setOnAction { e ->
            e.consume()
            val d: File? = chooser.showDialog(browseButton.scene.window)
           d?.let { directory.valueProperty().setValue(it) }
        }
    }

    fun asNode(): Node {
        return this.contents
    }

    fun directoryProperty(): Property<File> {
        return this.directory.valueProperty()
    }

}
