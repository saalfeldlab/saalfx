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

import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.skin.ListViewSkin
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import me.xdrop.fuzzywuzzy.FuzzySearch
import me.xdrop.fuzzywuzzy.model.ExtractedResult
import org.apache.commons.lang.builder.HashCodeBuilder
import org.janelia.saalfeldlab.fx.Separators
import org.janelia.saalfeldlab.fx.extensions.LazyForeignValue
import org.janelia.saalfeldlab.fx.extensions.bindHeightToItemSize
import org.janelia.saalfeldlab.fx.ui.MatchSelection.Companion.fuzzySorted
import org.janelia.saalfeldlab.fx.ui.MatchSelection.Companion.fuzzyTop
import org.janelia.saalfeldlab.fx.util.InvokeOnJavaFXApplicationThread
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.function.BiFunction
import java.util.function.Consumer

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
) : Region() {

    private val candidates = candidates.map { it }

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

        val labelList = ListView(currentOrder)
        labelList.selectionModel.selectionMode = SelectionMode.SINGLE
        labelList.stylesheets.add("matcher.css")

        labelList.maxWidthProperty().bind(maxWidthProperty())
        labelList.prefWidthProperty().bind(maxWidthProperty())
        labelList.bindHeightToItemSize()


        /* NOTE: I would have prefered that `labelList.scrollTo(idx)` would have worked here,
        * but that always calls `scrollToTop()` which we don't want. We create our own skin here,
        * expose it's virtual flow, and call the correct `scrollTo` method on it */
        val listViewSkin = object : ListViewSkin<String>(labelList) {
            val flow = virtualFlow
        }
        labelList.skin = listViewSkin
        labelList.focusModel.focusedIndexProperty().addListener { _, _, new ->
            (new?.toInt())?.let { idx ->
                listViewSkin.flow.scrollTo(idx)
            }
        }

        labelList.cellFactoryProperty().set {
            object : ListCell<String>() {
                init {
                    hoverProperty().addListener { _, _, hovered ->
                        if (hovered) listView.focusModel.focus(index)
                    }
                }

                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    item?.let { text = it }
                }
            }
        }

        labelList.selectionModel.selectedItemProperty().addListener { _, _, selected -> selected?.let(onConfirm) }

        fuzzySearchField.text = ""

        val handleNavigationKeys: (KeyEvent) -> Boolean = { e ->
            when (e.code) {
                KeyCode.DOWN -> {
                    labelList.focusModel.focusNext()
                    e.consume()
                }
                KeyCode.UP -> {
                    labelList.focusModel.focusPrevious()
                    e.consume()
                }
                else -> {
                }
            }
            e.isConsumed
        }

        val contents = VBox(fuzzySearchField, Separators.horizontal(), labelList)
        contents.setOnKeyPressed { e ->
            LOG.debug("Key pressed in contents with code {}", e.code)
            if (!handleNavigationKeys(e)) {
                if (e.code == KeyCode.ESCAPE && fuzzySearchField.text?.isNotEmpty() == true) {
                    fuzzySearchField.text = ""
                    e.consume()
                } else if (e.code == KeyCode.ENTER) {
                    labelList.focusModel.focusedItem?.let(onConfirm)
                    e.consume()
                }
            }
        }
        fuzzySearchField.setOnKeyPressed { e ->
            LOG.debug("Key pressed in fuzzy search field with code {}", e.code)
            handleNavigationKeys(e)
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
    var limit: Int?

    fun getItems(): ObservableList<MenuItem>

    fun hide()

    fun hideAndProcess(value: String?) {
        hide()
        processSelection(value)
    }

    fun getMatcher(candidates: List<String>): MatchSelection {
        val matcher = limit?.let { topN ->
            fuzzyTop(candidates, { hideAndProcess(it) }, topN, cutoff)
        } ?: fuzzySorted(candidates, { hideAndProcess(it) }, cutoff)
        return matcher.also {
            maxWidth?.let { matcher.maxWidth = it }
            val cmi = CustomMenuItem(matcher, false)
            cmi.styleClass.clear()
            getItems().setAll(cmi)
        }
    }
}


open class MatchSelectionMenuButton(private val candidates: List<String>, menuText: String? = null, matcherMaxWidth: Double? = null, override val processSelection: (String?) -> Unit) : MenuButton(menuText), MatchSelectionNode {

    @JvmOverloads
    constructor(candidates: List<String>, menuText: String, matcherMaxWidth: Double? = null, processSelection: Consumer<String?>) : this(candidates, menuText, matcherMaxWidth, processSelection::accept)

    final override var limit: Int? = null
    final override var cutoff: Int? = null
    final override var maxWidth: Double? = matcherMaxWidth
        set(value) {
            matcher.maxWidth = value ?: Region.USE_COMPUTED_SIZE
            field = value
        }

    private var forceupdateMatcher = 0

    private val matcherHash: Int
        get() = HashCodeBuilder().append(limit).append(cutoff).append(forceupdateMatcher).append(candidates).toHashCode()
    private val matcher by LazyForeignValue(this::matcherHash) { getMatcher(candidates) }

    init {
        matcher.maxWidth = maxWidth ?: Region.USE_COMPUTED_SIZE
        setOnShowing {
            InvokeOnJavaFXApplicationThread {
                matcher.requestFocus()
            }
        }
    }
}

open class MatchSelectionMenu(private val candidates: List<String>, menuText: String = "", matcherMaxWidth: Double? = null, override val processSelection: (String?) -> Unit) : Menu(menuText), MatchSelectionNode {

    @JvmOverloads
    constructor(candidates: List<String>, menuText: String = "", maxWidth: Double? = null, processSelection: Consumer<String?>) : this(candidates, menuText, maxWidth, processSelection::accept)

    final override var limit: Int? = null
    final override var cutoff: Int? = null
    final override var maxWidth: Double? = matcherMaxWidth
        set(value) {
            matcher.maxWidth = value ?: Region.USE_COMPUTED_SIZE
            field = value
        }

    private var forceupdateMatcher = 0

    private val matcherHash: Int
        get() = HashCodeBuilder().append(limit).append(cutoff).append(candidates).toHashCode()
    private val matcher by LazyForeignValue(this::matcherHash) { getMatcher(candidates) }

    init {
        matcher.maxWidth = maxWidth ?: Region.USE_COMPUTED_SIZE
        setOnShowing {
            InvokeOnJavaFXApplicationThread {
                matcher.requestFocus()
            }
        }
        if (candidates is ObservableList<String>) {
            candidates.addListener(ListChangeListener {
                forceupdateMatcher++
                matcher
            })
        }
    }

    override fun hide() {
        super.hide()
        parentPopup.hide()
    }
}
