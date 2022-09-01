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
package org.janelia.saalfeldlab.fx.ortho

import com.sun.javafx.application.PlatformImpl
import com.sun.javafx.stage.WindowHelper
import javafx.application.Platform
import javafx.event.Event
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.SplitPane
import javafx.scene.layout.BorderPane
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.Window
import javafx.stage.WindowEvent

class DynamicCellPane(nodes: List<List<Node>>) : SplitPane() {

    init {
        nodes.forEach { cells ->
            items += SplitPane(*cells.toTypedArray())
        }
        orientation = Orientation.VERTICAL
        distributeAllDividers()
    }

    private var maximizedNodes: MutableMap<Node, Pair<Int, Int>>? = null
    val maximized: Boolean
        get() = maximizedNodes != null

    private var rowWhenNotMaximized: List<Node>? = null
    private val rowWhenMaximized by lazy { SplitPane() }


    fun addRow(row: Int, vararg nodes: Node) {
        val cells = cells()
        /* Can't have duplicate nodes*/
        for (node in nodes) {
            if (node in cells) {
                duplicateNodeError()
            }
        }
        items.add(row, SplitPane(*nodes))
        distributeAllDividers()
    }

    private fun duplicateNodeError() {
        error("Cannot add the same node to the pane multiple times. ")
    }

    fun remove(node: Node?): Boolean {

        for (rowIdx in items.indices) {
            val row = items[rowIdx]
            (row as? SplitPane)?.items?.let { rowItems ->
                if (rowItems.removeIf { it == node }) {
                    if (rowItems.isEmpty()) {
                        items.removeAt(rowIdx)
                    }
                    return true
                }
            }
        }
        return false
    }


    fun remove(row: Int, col: Int): Node? {
        return (items[row] as? SplitPane)?.items?.let { rowItems ->
            val removed = rowItems.removeAt(col)
            if (rowItems.isEmpty()) {
                items.removeAt(row)
            }
            removed
        }
    }

    fun add(node: Node, row: Int, col: Int) {
        if (items[row] == null) {
            items.add(row, SplitPane())
        }
        (items[row] as? SplitPane)?.items?.add(col, node)
    }

    fun replace(oldValue: Node, newValue: Node, row: Int, col: Int): Boolean {
        return if (this[row, col] == oldValue) {
            remove(row, col)
            add(newValue, row, col)
            true
        } else {
            false
        }
    }

    fun swap(row1: Int, col1: Int, row2: Int, col2: Int): Boolean {
        val node1 = this[row1, col1]
        val node2 = this[row2, col2]

        if (node1 != null && node2 != null) {
            this[row1, col1] = node2
            this[row2, col2] = node1
            return true
        }
        return false

    }

    fun add(row: Int, col: Int, node: Node) {
        if (node in cells()) {
            duplicateNodeError()
        }
        (items[row.coerceAtMost(items.size - 1)] as? SplitPane)?.items?.let { it.add(col.coerceAtMost(it.size - 1), node) }
    }

    operator fun set(row: Int, col: Int, node: Node?): Node? = (items[row] as? SplitPane)?.items?.set(col, node)

    operator fun get(row: Int, col: Int): Node? = (items[row] as? SplitPane)?.items?.get(col)

    operator fun get(node: Node?): Pair<Int, Int>? {
        node?.let {
            for (rowIdx in items.indices) {
                (items[rowIdx] as? SplitPane)?.items?.let {
                    for (colIdx in it.indices) {
                        if (it[colIdx] == node) return rowIdx to colIdx
                    }
                }
            }
        }
        return null
    }

    operator fun contains(node: Node?) = node in cells()

    fun indexOf(node: Node?): Pair<Int, Int>? {
        node?.let {
            for (row in items.indices) {
                (items[row] as? SplitPane)?.items?.let { cols ->
                    for (col in cols.indices) {
                        if (cols[col] == node) {
                            return row to col
                        }
                    }
                }
            }
        }
        return null
    }

    fun cells(): List<Node> {
        return items
            .flatMap { (it as? SplitPane)?.items ?: listOf() }
            .toList()

    }

    fun toggleMaximize(vararg nodes: Node) {
        if (maximizedNodes?.size == 0) maximizedNodes = null
        maximizedNodes?.let {
            rowWhenMaximized.items.clear()
            this.items.remove(rowWhenMaximized)
            rowWhenNotMaximized?.let { this.items.setAll(rowWhenNotMaximized) }
            rowWhenNotMaximized = null
            it.forEach { node, (row, col) ->
                this[row, col] = node
                maximizedNodes = null
            }
            cells().forEach { node -> node.isVisible = true }
            distributeAllDividers()
        } ?: let {
            cells().forEach { node -> node.isVisible = node in nodes }
            maximizedNodes = mutableMapOf<Node, Pair<Int, Int>>().also { map ->
                for (node in nodes) {
                    this[node]?.let {
                        map[node] = it
                    }
                }
            }
            rowWhenNotMaximized = items.toList()
            items.clear()
            rowWhenMaximized.items.setAll(maximizedNodes!!.keys)
            items.add(rowWhenMaximized)
            rowWhenMaximized.distributeDividers()
            maximize(items.indexOf(rowWhenMaximized))
        }
    }

    private fun SplitPane.maximize(cellIdx: Int) {

        for (divIdx in dividers.indices) {
            if (divIdx < cellIdx) setDividerPosition(divIdx, 0.0)
            else setDividerPosition(divIdx, 1.0)
        }
    }

    private fun SplitPane.distributeDividers() {
        setDividerPositions(*(1 until items.size).map { it.toDouble() / items.size }.toDoubleArray())
    }

    fun distributeAllDividers() {
        items.forEach { row -> (row as? SplitPane)?.distributeDividers() }
        distributeDividers()
    }

    fun toggleNodeDetach(
        node: Node,
        title: String? = null,
        topProvider: ((BorderPane) -> Node)? = null,
        bottomProvider: ((BorderPane) -> Node)? = null,
        onClose: (Stage) -> Unit = {},
        beforeShow: (Stage) -> Unit = {},
        reAttachIndices: Pair<Int, Int>? = null
    ) {
        /* close the cell if it is detached, otherwise, detach the cell */
        if (closeNodeIfDetached(node)) {
            return
        }
        /* rows before */
        val rowsBefore = items.size
        /* indices of node for re-attaching */
        val (row, col) = indexOf(node) ?: return
        if (remove(node)) {
            distributeAllDividers()
            /* get a new window */
            Stage().let { stage ->
                title?.let { stage.title = it }

                val newRoot = BorderPane(node)
                newRoot.centerProperty().addListener { _, _, new ->
                    if (new == null) {
                        closeNodeIfDetached(newRoot)
                    }
                }

                topProvider?.let { newRoot.top = it(newRoot) }
                bottomProvider?.let { newRoot.bottom = it(newRoot) }

                stage.scene = Scene(newRoot, scene.width, scene.height)
                stage.scene.stylesheets.setAll(scene.stylesheets)

                stage.initModality(Modality.NONE)
                beforeShow(stage)


                val cleanUp = {
                    onClose(stage)
                    /* if the DynamicCellPane is maximized, unmaximize it first */
                    if (maximized) toggleMaximize()

                    /* if the cell was already removed, do nothing */
                    if (newRoot.center != null) {
                        /* grab the original cell location */
                        val (reattachRow, reattachCol) = reAttachIndices ?: (row to col)

                        /* Node cannot already be in the cellpane, remove in case */
                        remove(node)
                        /* recombine in original window */
                        if (rowsBefore > items.size) {
                            addRow(reattachRow, node)
                        } else {
                            add(reattachRow, reattachCol, node)
                        }
                        distributeAllDividers()
                    }
                }
                stage.onCloseRequest = EventHandler { cleanUp() }
                stage.show()
            }
        }
    }

    private fun closeNodeIfDetached(node: Node): Boolean {
        var root = node
        /* get the top-most node */
        while (root.scene == null && root.parent != null) {
            root = root.parent
        }
        /* If we have a scene that is different from the main scene, then it means we
        * are already detached. In that case, we want to close the window, which
        * will trigger the detached viewer to re-attach to the pain scene */
        return (root.scene != null && root.scene != scene).also { nodeIsDetached ->
            if (nodeIsDetached) closeWindow(root.scene.window)
        }
    }

    private fun closeWindow(window: Window) {
        /* Something to note; I didn't think them manual unfocus would be necessary, but seemingly there is a difference in close behavior
		*   when you close programatically (as below) compare to a system close (e.g. clicking X or Alt+F4), whereby the node doesn't unfocus.
		*   To resolve, we do it manually first. */
        WindowHelper.setFocused(window, false)
        window.hide()
        Event.fireEvent(window, WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST))
    }
}

fun main() {
    PlatformImpl.startup { }
    PlatformImpl.setImplicitExit(true)
    Platform.runLater {
        val cellPane = DynamicCellPane(
            listOf(
                listOf(Label("Test"), Label("Test")),
                listOf(Label("Test"), Label("Test"))
            )
        )
        val scene = Scene(cellPane)
        val stage = Stage()
        stage.scene = scene
        stage.width = 800.0
        stage.height = 600.0
        stage.show()
    }
}
