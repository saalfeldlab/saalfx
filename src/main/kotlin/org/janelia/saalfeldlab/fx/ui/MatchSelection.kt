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

import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.control.Tooltip
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import me.xdrop.fuzzywuzzy.FuzzySearch
import me.xdrop.fuzzywuzzy.model.ExtractedResult
import org.janelia.saalfeldlab.fx.Labels
import org.janelia.saalfeldlab.fx.Separators
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.ArrayList
import java.util.Optional
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.IntSupplier
import kotlin.math.abs
import kotlin.math.min

/**
 * Menus cannot be updated dynamically. Use custom node for (fuzzy) filtering
 *
 * https://stackoverflow.com/questions/54834206/javafx-dynamically-update-menu-while-showing
 * https://bugs.openjdk.java.net/browse/JDK-8219620
 */
class MatchSelection(
        candidates: List<String>,
        private val matcher: BiFunction<String, List<String>, List<String>>,
        private val onConfirm: Consumer<String?>) : Region() {

    private val candidates = candidates.map { it }

    private val currentSelection = SimpleIntegerProperty(this, "current selection", 0)

    private val defaultLabelBackground = Label().background

    private val highlightLabelBackground = Background(BackgroundFill(Color.GRAY.brighter(), CornerRadii.EMPTY, Insets.EMPTY))

    private val fuzzySearchField = TextField(null)

    private class FuzzyMatcher(private val matcher: BiFunction<String, List<String>, List<ExtractedResult>>) :
            BiFunction<String, List<String>, List<String>> {

        constructor(matcher: (String, List<String>) -> List<ExtractedResult>) : this(BiFunction { s, l -> matcher(s, l) })

        override fun apply(query: String, from: List<String>) = matcher.apply(query, from).map { it.string }
    }

    init {
        super.getChildren().setAll(makeNode())
        this.fuzzySearchField.maxWidthProperty().bind(maxWidthProperty())
        this.focusedProperty().addListener { _, _, newv -> if (newv != null && newv) this.fuzzySearchField.requestFocus() }
    }

    private fun makeNode(): Node {
        fuzzySearchField.tooltip = Tooltip("Type to filter (fuzzy matching)")
        fuzzySearchField.promptText = "Type to filter"

        fuzzySearchField.addEventFilter(MouseEvent.MOUSE_MOVED) {
            fuzzySearchField.requestFocus()
            fuzzySearchField.selectEnd()
        }
        val currentOrder = FXCollections.observableArrayList<String>()
        fuzzySearchField.textProperty().addListener { _, _, newv -> currentOrder.setAll(if (newv == null || newv.isEmpty()) candidates else matcher.apply(newv, candidates)) }
        currentSelection.addListener { _, oldv, newv ->
            LOG.debug("Updating current selection from {} to {}", oldv, newv)
            if (currentOrder.size > 0) {
                if (newv.toInt() < 0)
                    currentSelection.value = currentOrder.size - min(currentOrder.size, abs(newv.toInt()))
                else if (newv.toInt() >= currentOrder.size)
                    currentSelection.value = currentOrder.size - 1
            }
        }

        val labelBox = VBox()
        currentOrder.addListener(ListChangeListener {
            val copy = currentOrder.map { it }
            val labels = ArrayList<Label>()
            for (i in copy.indices) {
                val text = copy[i]
                labels.add(toLabel(
                        text,
                        { currentSelection.set(i) },
                        { currentSelection.set(0) },
                        { onConfirm.accept(text) },
                        { currentSelection.set(i) }))
            }
            labelBox.children.setAll(labels)
            currentSelection.set(-1)
            currentSelection.set(0)
        })

        currentSelection.addListener { _, oldv, newv ->
            LOG.debug("Updating current selection from {} to {}", oldv, newv)
            val items = labelBox.children
            val newIndex = newv.toInt()
            val oldIndex = oldv.toInt()
            if (newIndex >= 0 && newIndex < items.size)
                (items[newIndex] as Label).background = highlightLabelBackground
            if (oldIndex >= 0 && oldIndex < items.size)
                (items[oldIndex] as Label).background = defaultLabelBackground
        }

        currentSelection.set(-1)
        fuzzySearchField.text = ""

        val contents = VBox(fuzzySearchField, Separators.horizontal(), labelBox)
        contents.setOnKeyPressed { e ->
            LOG.debug("Key pressed in contents with code {}", e.code)
            when (e.code) {
                KeyCode.ESCAPE -> if (Optional.ofNullable(fuzzySearchField.text).filter { it.isNotEmpty() }.isPresent) {
                    fuzzySearchField.text = ""
                    e.consume()
                }
                KeyCode.ENTER -> {
                    val selection = currentSelection.get()
                    onConfirm.accept(if (selection < 0 || selection >= currentOrder.size) null else currentOrder[selection])
                    e.consume()
                }
                KeyCode.DOWN -> {
                    currentSelection.set(currentSelection.value!! + 1)
                    e.consume()
                }
                KeyCode.UP -> {
                    currentSelection.set(currentSelection.value!! - 1)
                    e.consume()
                }
                else -> {}
            }
        }
        fuzzySearchField.setOnKeyPressed { e ->
            LOG.debug("Key pressed in fuzzy search field with code {}", e.code)
            when (e.code) {
                KeyCode.DOWN -> {
                    currentSelection.set(currentSelection.value!! + 1)
                    e.consume()
                }
                KeyCode.UP -> {
                    currentSelection.set(currentSelection.value!! - 1)
                    e.consume()
                }
                else -> {}
            }
        }
        contents.focusedProperty().addListener { _, _, newv -> if (newv != null && newv) fuzzySearchField.requestFocus() }
        return contents
    }

    /**
     *
     * @return [Region.getChildrenUnmodifiable]
     */
    public override fun getChildren(): ObservableList<Node?> = super.getChildrenUnmodifiable()

    private fun toLabel(
            text: String,
            onMouseEntered: () -> Unit,
            onMouseExited: () -> Unit,
            onMousePressed: () -> Unit,
            onMouseMoved: () -> Unit): Label {
        val label = Labels.withTooltip(text)
        label.setOnMouseEntered { e -> onMouseEntered() }
        label.setOnMouseExited { e -> onMouseExited() }
        label.setOnMouseMoved { e -> onMouseMoved() }
        label.setOnMousePressed { e ->
            if (e.isPrimaryButtonDown) {
                onMousePressed()
                e.consume()
            }
        }
        label.maxWidthProperty().bind(maxWidthProperty())
        return label
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        fun fuzzySorted(candidates: List<String>, onConfirm: (String?) -> Unit)  =
                fuzzySorted(candidates, Consumer { onConfirm(it) })

        fun fuzzySorted(candidates: List<String>, onConfirm: (String?) -> Unit, cutOff: () -> Int)  =
                fuzzySorted(candidates, Consumer { onConfirm(it) }, IntSupplier { cutOff() })

        fun fuzzyTop(candidates: List<String>, onConfirm: (String?) -> Unit, limit: () -> Int)  =
                fuzzyTop(candidates, Consumer { onConfirm(it) }, IntSupplier { limit() })

        fun fuzzyTop(candidates: List<String>, onConfirm: (String?) -> Unit, limit: () -> Int, cutOff: () -> Int)  =
                fuzzyTop(candidates, Consumer { onConfirm(it) }, IntSupplier { limit() }, IntSupplier { cutOff() })

        @JvmStatic
        fun fuzzySorted(candidates: List<String>, onConfirm: Consumer<String?>) =
                MatchSelection(candidates, FuzzyMatcher(BiFunction { query, choices -> FuzzySearch.extractSorted(query, choices) }), onConfirm)

        @JvmStatic
        fun fuzzySorted(candidates: List<String>, onConfirm: Consumer<String?>, cutoff: IntSupplier) =
            MatchSelection(candidates, FuzzyMatcher { query, from -> FuzzySearch.extractSorted(query, from, cutoff.asInt) }, onConfirm)

        @JvmStatic
        fun fuzzyTop(candidates: List<String>, onConfirm: Consumer<String?>, limit: IntSupplier) =
            MatchSelection(candidates, FuzzyMatcher { query, from -> FuzzySearch.extractTop(query, from, limit.asInt) }, onConfirm)

        @JvmStatic
        fun fuzzyTop(candidates: List<String>, onConfirm: Consumer<String?>, limit: IntSupplier, cutoff: IntSupplier) =
            MatchSelection(candidates, FuzzyMatcher { query, from -> FuzzySearch.extractTop(query, from, limit.asInt, cutoff.asInt) }, onConfirm)
    }

}
