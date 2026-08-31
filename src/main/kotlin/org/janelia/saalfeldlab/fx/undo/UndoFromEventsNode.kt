package org.janelia.saalfeldlab.fx.undo

import javafx.beans.binding.Bindings
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.TitledPane
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.janelia.saalfeldlab.fx.Labels
import org.janelia.saalfeldlab.fx.extensions.component1
import org.janelia.saalfeldlab.fx.extensions.component2
import org.janelia.saalfeldlab.fx.util.InvokeOnJavaFXApplicationThread

interface EventDisplay<T> {

	fun title(event: T): String = "$event"

	fun node(event: T): Node = Labels.withTooltip("$event")

	companion object {

		@JvmStatic
		@JvmOverloads
		fun <T> defaultDisplay(
			title: (T) -> String = { "$it" },
			node: (T) -> Node = { Labels.withTooltip("$it") }
		) = object : EventDisplay<T> {

			override fun title(event: T) = title(event)

			override fun node(event: T) = node(event)
		}
	}
}

/**
 * Display an [EventHistory], with undo/redo/delete buttons
 */
open class UndoFromEventsNode<T> @JvmOverloads constructor(
    private val history: EventHistory<T>,
    private val display: EventDisplay<T> = EventDisplay.defaultDisplay()
) : VBox() {

	private val eventBox = VBox()

	private val eventScrollPane = ScrollPane(eventBox).apply {
		isFitToWidth = true
		hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
		setVgrow(this, Priority.ALWAYS)
	}

	private val currentEventLabels = mutableListOf<Label>()

	init {
		children.setAll(controlBar(), eventScrollPane)
		InvokeOnJavaFXApplicationThread { updateEventBox() }
		history.events.subscribe { InvokeOnJavaFXApplicationThread { updateEventBox() } }
		history.currentIndexProperty.subscribe { index ->
			InvokeOnJavaFXApplicationThread { showCurrentEventIndicator(index.toInt()) }
		}
	}

	/**
	 * Confirmation before delete all is run.
     * Default is no confirmation, but can be overridden to trigger some confirmation
	 */
	protected open fun confirmDeleteAll(): Boolean = true

	private fun controlBar() = HBox().apply {
		children += Button("Undo").apply {
			setOnAction { history.undo() }
			disableProperty().bind(Bindings.not(history.canUndo))
		}
		children += Button("Redo").apply {
			setOnAction { history.redo() }
			disableProperty().bind(Bindings.not(history.canRedo))
		}
		children += Region().also { filler ->
			HBox.setHgrow(filler, Priority.ALWAYS)
		}
		children += Button("Delete Disabled").apply {
			tooltip = Tooltip("Delete every event that is currently undone")
			setOnAction { history.deleteDisabled() }
			disableProperty().bind(Bindings.not(history.hasDisabledEvents))
		}
		children += Button("Delete All").apply {
			setOnAction { if (confirmDeleteAll()) history.deleteAll() }
			disableProperty().bind(Bindings.isEmpty(history.events))
		}
	}

	/* fx thread only; currentEventLabels is not otherwise confined */
	private fun updateEventBox() {
		currentEventLabels.clear()
		val eventEntries = history.events.toList().map { entry ->
			val (event, isApplied) = entry
			val indicator = Label("").apply {
				minWidth = INDICATOR_WIDTH
				maxWidth = INDICATOR_WIDTH
				prefWidth = INDICATOR_WIDTH
			}
			currentEventLabels += indicator
			TitledPane(display.title(event), display.node(event)).apply {
				isExpanded = false
				graphic = HBox(
					CheckBox(null).apply { selectedProperty().bindBidirectional(isApplied) },
					indicator,
					Button(DELETE_INDICATOR).apply {
						tooltip = Tooltip("Delete this event")
						setOnAction { history.delete(entry) }
					}
				)
			}
		}
		eventBox.children.setAll(eventEntries.reversed())
		/* the labels are only the current ones now, so the indicator has to be placed again */
		showCurrentEventIndicator(history.currentIndexProperty.get())
	}

	private fun showCurrentEventIndicator(index: Int) {
		currentEventLabels.forEachIndexed { idx, label -> label.text = if (idx == index) CURRENT_EVENT_INDICATOR else "" }
	}

	companion object {

		// left facing triangle
		// https://www.fileformat.info/info/unicode/char/25c0/index.htm
		private const val CURRENT_EVENT_INDICATOR = "◀"

		// multiplication x
		// https://www.fileformat.info/info/unicode/char/2715/index.htm
		private const val DELETE_INDICATOR = "✕"

		private const val INDICATOR_WIDTH = 30.0
	}
}
