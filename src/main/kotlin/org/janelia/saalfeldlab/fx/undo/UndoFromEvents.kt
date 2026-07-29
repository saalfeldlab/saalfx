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

import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.beans.InvalidationListener
import javafx.beans.binding.Bindings
import javafx.beans.property.BooleanProperty
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.TitledPane
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.util.Pair
import org.janelia.saalfeldlab.fx.util.InvokeOnJavaFXApplicationThread
import java.util.function.Consumer
import java.util.function.Function

class UndoFromEvents<T>(
	private val events: ObservableList<Pair<T, BooleanProperty>>,
	private val title: (T) -> String,
	private val contents: (T) -> Node,
	private val onDelete: ((Pair<T, BooleanProperty>) -> Unit)? = null
) {

	@JvmOverloads
	constructor(
		events: ObservableList<Pair<T, BooleanProperty>>,
		title: Function<T, String>,
		contents: Function<T, Node>,
		onDelete: Consumer<Pair<T, BooleanProperty>>? = null
	) : this(
		events,
		{ title.apply(it) },
		{ contents.apply(it) },
		onDelete?.let { consumer -> { event: Pair<T, BooleanProperty> -> consumer.accept(event) } }
	)

	private val history = EventHistory(events)

	private val eventBox = VBox()

	private val currentEventLabel = ArrayList<Label>()

	private val canUndo = history.canUndo

	private val canRedo = history.canRedo

	val node: Node
		get() = eventBox

	init {
		history.currentIndexProperty.addListener { _, _, newv ->
			LOG.debug { "Updating current event index $newv" }
			InvokeOnJavaFXApplicationThread.invoke { showCurrentEventIndicator(newv.toInt()) }
		}

		this.events.addListener(InvalidationListener { updateEventBox(ArrayList(this.events)) })
		updateEventBox(ArrayList(this.events))
	}

	fun undo() = history.undo()

	fun redo() = history.redo()

	private fun updateEventBox(events: List<Pair<T, BooleanProperty>>) {
		LOG.debug { "Updating event box for events $events" }
		val nodes = ArrayList<Node>()
		this.currentEventLabel.clear()
		for (i in events.indices) {
			val event = events[i]
			val title = this.title(event.key)
			val contents = this.contents(event.key)
			val cbox = CheckBox(null)
			val currentEventLabel = Label("")

			cbox.selectedProperty().bindBidirectional(event.value)
			currentEventLabel.minWidth = 30.0
			currentEventLabel.maxWidth = 30.0
			currentEventLabel.prefWidth = 30.0

			val graphic = HBox(cbox, currentEventLabel)
			onDelete?.let { delete ->
				val deleteButton = Button(DELETE_INDICATOR)
				deleteButton.tooltip = Tooltip("Delete this event")
				deleteButton.setOnAction { delete(event) }
				graphic.children += deleteButton
			}

			val tp = TitledPane(title, contents)
			tp.graphic = graphic
			tp.isExpanded = false

			this.currentEventLabel.add(currentEventLabel)
			nodes.add(tp)
		}
		nodes.reverse()
		InvokeOnJavaFXApplicationThread {
			this@UndoFromEvents.eventBox.children.setAll(nodes)
			/* the labels are only the current ones now, so the indicator has to be placed again */
			showCurrentEventIndicator(history.currentIndexProperty.get())
		}
	}

    private fun showCurrentEventIndicator(index: Int) {
        currentEventLabel.forEachIndexed { idx, label -> label.text = if (idx == index) CURRENT_EVENT_INDICATOR else "" }
    }

	companion object {

		// left facing triangle
		// https://www.fileformat.info/info/unicode/char/25c0/index.htm
		private val CURRENT_EVENT_INDICATOR = "◀"

		// multiplication x
		// https://www.fileformat.info/info/unicode/char/2715/index.htm
		private val DELETE_INDICATOR = "✕"

		private val LOG = KotlinLogging.logger {  }

		/**
         * deleting from [events] is up to the caller, as is any confirmation/warning.
         *
		 * @param onDelete if provided, each event gets a button that passes it here
		 * @param onDeleteAll if provided, a button for it is added next to undo and redo
		 */
		fun <T> withUndoRedoButtons(
			events: ObservableList<Pair<T, BooleanProperty>>,
			title: (T) -> String,
			contents: (T) -> Node,
			onDelete: ((Pair<T, BooleanProperty>) -> Unit)? = null,
			onDeleteAll: (() -> Unit)? = null
		): Node {

			val undo = UndoFromEvents(events, title, contents, onDelete)

            return VBox().apply {
                children += HBox().apply {
                    children += Region().also { filler ->
                        HBox.setHgrow(filler, Priority.ALWAYS)
                    }
                    children += Button("Undo").apply {
                        setOnAction { undo.undo() }
                        disableProperty().bind(undo.canUndo.not())
                    }
                    children += Button("Redo").apply {
                        setOnAction { undo.redo() }
                        disableProperty().bind(undo.canRedo.not())
                    }
                    onDeleteAll?.let { deleteAll ->
                        children += Button("Delete All").apply {
                            setOnAction { deleteAll() }
                            disableProperty().bind(Bindings.isEmpty(events))
                        }
                    }
                }
                children += undo.node
            }
		}

		@JvmStatic
		@JvmOverloads
		fun <T> withUndoRedoButtons(
			events: ObservableList<Pair<T, BooleanProperty>>,
			title: Function<T, String>,
			contents: Function<T, Node>,
			onDelete: Consumer<Pair<T, BooleanProperty>>? = null,
			onDeleteAll: Runnable? = null
		): Node = withUndoRedoButtons(
			events,
			{ title.apply(it) },
			{ contents.apply(it) },
			onDelete?.let { consumer -> { event: Pair<T, BooleanProperty> -> consumer.accept(event) } },
			onDeleteAll?.let { runnable -> { runnable.run() } }
		)
	}

}
