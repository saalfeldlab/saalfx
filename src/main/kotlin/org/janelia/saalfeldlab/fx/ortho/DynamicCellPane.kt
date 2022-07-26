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
import javafx.application.Platform
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.SplitPane
import javafx.stage.Stage
import org.janelia.saalfeldlab.fx.ui.HVBox

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
        items.add(row, SplitPane(*nodes))
        distributeAllDividers()
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

    companion object {
        @JvmStatic
        fun getCellIndex(col: Int, row: Int): Int {
            assert(col in 0..1)
            assert(row in 0..1)
            return if (row == 0) {
                if (col == 0) 0 else 1
            } else {
                if (col == 0) 2 else 3
            }
        }
    }
}

fun main() {
    PlatformImpl.startup { }
    PlatformImpl.setImplicitExit(true)
    Platform.runLater {
        val cellPane = DynamicCellPane(
            listOf(
                listOf(HVBox(Label("Test")), HVBox(Label("Test"))),
                listOf(HVBox(Label("Test")), HVBox(Label("Test")))
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
