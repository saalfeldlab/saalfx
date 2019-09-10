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
