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
package org.janelia.saalfeldlab.fx.undo

import javafx.beans.InvalidationListener
import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.IntegerBinding
import javafx.beans.property.BooleanProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.TitledPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.util.Pair
import org.janelia.saalfeldlab.fx.util.InvokeOnJavaFXApplicationThread
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.invoke.MethodHandles
import java.util.ArrayList
import java.util.Collections
import java.util.function.Function

class UndoFromEvents<T>(
        private val events: ObservableList<Pair<T, BooleanProperty>>,
        private val title: Function<T, String>,
        private val contents: Function<T, Node>) {

    constructor(
            events: ObservableList<Pair<T, BooleanProperty>>,
            title: (T) -> String,
            contents: (T) -> Node) : this(events, Function { title(it) }, Function { contents(it) } )

    private val eventBox = VBox()

    private val currentEventLabel = ArrayList<Label>()

    private val currentEventIndex = SimpleIntegerProperty(-1)

    private val currentEventListSize = SimpleIntegerProperty()

    private val currentIndexIsWithinList = currentEventIndex
            .greaterThanOrEqualTo(0)
            .and(currentEventIndex.lessThan(currentEventListSize))

    private val canUndo = currentIndexIsWithinList

    private val redoIndex = currentEventIndex.add(1)

    private val redoIndexIsWithinList = redoIndex
            .greaterThanOrEqualTo(0)
            .and(redoIndex.lessThan(currentEventListSize))

    private val canRedo = redoIndexIsWithinList

    val node: Node
        get() = eventBox

    init {
        currentEventIndex.addListener { _, oldv, newv ->
            val oldIndex = oldv.toInt()
            val newIndex = newv.toInt()
            LOG.debug("Updating current event index {} {}", oldIndex, newIndex)
            if (oldIndex >= 0 && oldIndex < currentEventLabel.size)
                InvokeOnJavaFXApplicationThread.invoke { currentEventLabel[oldIndex].text = "" }
            if (newIndex >= 0 && newIndex < currentEventLabel.size)
                InvokeOnJavaFXApplicationThread.invoke {
                    currentEventLabel[newIndex].text = CURRENT_EVENT_INDICATOR
                }
        }

        this.events.addListener(InvalidationListener { updateEventBox(ArrayList<Pair<T, out BooleanProperty>>(this.events)) })
        updateEventBox(ArrayList<Pair<T, out BooleanProperty>>(this.events))

        this.events.addListener(InvalidationListener { currentEventListSize.value = this.events.size })
        currentEventListSize.set(this.events.size)

    }

    fun undo() {
        if (canUndo.get()) {
            val currentIndex = currentEventIndex.get()
            this.events[currentIndex].value.set(false)
            this.currentEventIndex.set(currentIndex - 1)
        }
    }

    fun redo() {
        if (canRedo.get()) {
            val index = redoIndex.get()
            this.events[index].value.set(true)
            this.currentEventIndex.set(index)
        }
    }

    private fun updateEventBox(events: List<Pair<T, out BooleanProperty>>) {
        LOG.debug("Updating event box for events {}", events)
        val nodes = ArrayList<Node>()
        this.currentEventLabel.clear()
        this.currentEventIndex.set(-1)
        for (i in events.indices) {
            val event = events[i]
            val title = this.title.apply(event.key)
            val contents = this.contents.apply(event.key)
            val cbox = CheckBox(null)
            val currentEventLabel = Label("")

            cbox.selectedProperty().bindBidirectional(event.value)
            currentEventLabel.minWidth = 30.0
            currentEventLabel.maxWidth = 30.0
            currentEventLabel.prefWidth = 30.0

            val tp = TitledPane(title, contents)
            tp.graphic = HBox(cbox, currentEventLabel)
            tp.isExpanded = false

            this.currentEventLabel.add(currentEventLabel)
            nodes.add(tp)
        }
        this.currentEventIndex.set(events.size - 1)
        nodes.reverse()
        InvokeOnJavaFXApplicationThread.invoke { this.eventBox.children.setAll(nodes) }
    }

    companion object {

        // left facing triangle
        // https://www.fileformat.info/info/unicode/char/25c0/index.htm
        private val CURRENT_EVENT_INDICATOR = "◀"

        private val LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        fun <T> withUndoRedoButtons(
                events: ObservableList<Pair<T, BooleanProperty>>,
                title: (T) -> String,
                contents: (T) -> Node) = withUndoRedoButtons(events, Function { title(it) }, Function { contents(it) } )

        @JvmStatic
        fun <T> withUndoRedoButtons(
                events: ObservableList<Pair<T, BooleanProperty>>,
                title: Function<T, String>,
                contents: Function<T, Node>): Node {
            val undo = UndoFromEvents(events, title, contents)
            val undoButton = Button("Undo")
            val redoButton = Button("Redo")
            val filler = Region()
            val buttonBox = HBox(filler, undoButton, redoButton)
            val tp = TitledPane("Events", undo.node)

            undoButton.setOnAction { e -> undo.undo() }
            redoButton.setOnAction { e -> undo.redo() }

            undo.canUndo.addListener { obs, oldv, newv -> undoButton.isDisable = !newv }
            undo.canRedo.addListener { obs, oldv, newv -> redoButton.isDisable = !newv }
            undoButton.isDisable = !undo.canUndo.get()
            redoButton.isDisable = !undo.canRedo.get()

            tp.isExpanded = false

            return VBox(buttonBox, undo.node)
        }
    }

}
