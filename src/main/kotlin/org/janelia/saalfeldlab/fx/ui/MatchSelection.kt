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
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import me.xdrop.fuzzywuzzy.FuzzySearch
import me.xdrop.fuzzywuzzy.model.ExtractedResult
import org.janelia.saalfeldlab.fx.Labels
import org.janelia.saalfeldlab.fx.Separators
import org.janelia.saalfeldlab.fx.extensions.LazyForeignValue
import org.janelia.saalfeldlab.fx.ui.MatchSelection.Companion.fuzzySorted
import org.janelia.saalfeldlab.fx.util.InvokeOnJavaFXApplicationThread
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.Optional
import java.util.function.BiFunction
import java.util.function.Consumer
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
    private val onConfirm: (String?) -> Unit
) : VBox() {

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
        val labelBox = VBox().also {
            it.maxWidthProperty().bind(maxWidthProperty())
        }
        currentOrder.addListener(ListChangeListener {
            val copy = currentOrder.map { it }
            val labels = ArrayList<Label>()
            for (i in copy.indices) {
                val text = copy[i]
                val label = Labels.withTooltip(text).apply {
                    setOnMouseEntered { currentSelection.set(i) }
                    setOnMouseExited { currentSelection.set(0) }
                    setOnMouseMoved { currentSelection.set(i) }
                    setOnMousePressed { e ->
                        if (e.isPrimaryButtonDown) {
                            onConfirm(text)
                            e.consume()
                        }
                    }
                    maxWidthProperty().set(Double.MAX_VALUE)
                    textFill = Color.BLACK
                }
                labels.add(label)
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
                    onConfirm(if (selection < 0 || selection >= currentOrder.size) null else currentOrder[selection])
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
                else -> {
                }
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
                else -> {
                }
            }
        }
        contents.focusedProperty().addListener { _, _, newv -> if (newv != null && newv) fuzzySearchField.requestFocus() }
        return contents
    }

    /**
     *
     * @return [Region.getChildrenUnmodifiable]
     */
    override fun getChildren(): ObservableList<Node?> = super.getChildrenUnmodifiable()

    companion object {

        private val LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())


        fun fuzzySorted(candidates: List<String>, onConfirm: (String?) -> Unit, cutoff: Int? = null): MatchSelection {
            cutoff?.let {
                return MatchSelection(candidates, FuzzyMatcher { query, from -> FuzzySearch.extractSorted(query, from, cutoff) }, onConfirm)
            }
            return MatchSelection(candidates, FuzzyMatcher { query, choices -> FuzzySearch.extractSorted(query, choices) }, onConfirm)
        }

        fun fuzzyTop(candidates: List<String>, onConfirm: ((String?) -> Unit), limit: Int, cutoff: Int? = null): MatchSelection {
            cutoff?.let {
                return MatchSelection(candidates, FuzzyMatcher { query, from -> FuzzySearch.extractTop(query, from, limit, cutoff) }, onConfirm)
            }
            return MatchSelection(candidates, FuzzyMatcher { query, from -> FuzzySearch.extractTop(query, from, limit) }, onConfirm)
        }


        @JvmOverloads
        @JvmStatic
        fun fuzzySorted(candidates: List<String>, onConfirm: Consumer<String?>, cutoff: Int? = null): MatchSelection {
            val onConfirmConverted: (String?) -> Unit = { onConfirm.accept(it) }
            return fuzzySorted(candidates, onConfirmConverted, cutoff)
        }

        @JvmOverloads
        @JvmStatic
        fun fuzzyTop(candidates: List<String>, onConfirm: Consumer<String?>, limit: Int, cutoff: Int? = null): MatchSelection {
            val convertOnConfirm: (String?) -> Unit = { onConfirm.accept(it) }
            return fuzzyTop(candidates, convertOnConfirm, limit, cutoff)
        }

    }


}

interface MatchSelectionNode {

    val processSelection: (String?) -> Unit

    var cutoff: Int?
    var maxWidth: Double?

    fun getItems(): ObservableList<MenuItem>

    fun hide()

    fun hideAndProcess(value: String?) {
        hide()
        processSelection(value)
    }

    fun getMatcher(candidates: List<String>): MatchSelection {
        return fuzzySorted(candidates, { hideAndProcess(it) }, cutoff).also { matcher ->
            maxWidth?.let { matcher.maxWidth = it }
            val cmi = CustomMenuItem(matcher, false)
            cmi.styleClass.clear()
            getItems().setAll(cmi)
        }
    }
}


class MatchSelectionMenuButton(candidates: List<String>, text: String? = null, matcherMaxWidth: Double? = null, override val processSelection: (String?) -> Unit) : MenuButton(text), MatchSelectionNode {

    @JvmOverloads
    constructor(candidates: List<String>, menuText: String, matcherMaxWidth: Double? = null, processSelection: Consumer<String?>) : this(candidates, menuText, matcherMaxWidth, processSelection::accept)

    override var cutoff: Int? = null
    override var maxWidth: Double? = matcherMaxWidth
        set(value) {
            matcher.maxWidth = value ?: Region.USE_COMPUTED_SIZE
            field = value
        }

    private val matcher by LazyForeignValue({ cutoff }) { getMatcher(candidates) }

    init {
        /* Call manually the first time */
        matcher
        setOnShowing {
            InvokeOnJavaFXApplicationThread {
                matcher.requestFocus()
            }
        }
    }
}

class MatchSelectionMenu(candidates: List<String>, menuText: String = "", matcherMaxWidth: Double? = null, override val processSelection: (String?) -> Unit) : Menu(menuText), MatchSelectionNode {

    @JvmOverloads
    constructor(candidates: List<String>, menuText: String = "", maxWidth: Double? = null, processSelection: Consumer<String?>) : this(candidates, menuText, maxWidth, processSelection::accept)

    override var cutoff: Int? = null
    override var maxWidth: Double? = matcherMaxWidth
        set(value) {
            matcher.maxWidth = value ?: Region.USE_COMPUTED_SIZE
            field = value
        }

    private val matcher by LazyForeignValue({ cutoff }) {
        getMatcher(candidates).also {
            this.maxWidth?.let { w -> it.maxWidth = w }
        }
    }

    init {
        matcher
        setOnShowing {
            InvokeOnJavaFXApplicationThread {
                matcher.requestFocus()
            }
        }
    }

}
