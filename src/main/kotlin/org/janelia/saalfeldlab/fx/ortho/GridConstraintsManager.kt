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

import javafx.beans.binding.Bindings
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.RowConstraints
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class GridConstraintsManager {

    private var previousFirstRowHeight: Double = 0.0

    private var previousFirstColumnWidth: Double = 0.0

    @get:Synchronized
    var isFullScreen = false
        private set

    private val firstRowHeight = SimpleDoubleProperty()

    private val firstColumnWidth = SimpleDoubleProperty()

    val maximizedColumn: MaximizedColumn
        get() = fromFirstColumnWidth(this.firstColumnWidth.value)

    val maximizedRow: MaximizedRow
        get() = fromFirstRowHeight(this.firstRowHeight.value)

    enum class MaximizedRow constructor(val index: Int) {
        TOP(0),
        BOTTOM(1),
        NONE(-1);

        companion object {

            @JvmStatic
            fun fromIndex(index: Int): MaximizedRow? {
                return when (index) {
                    0 -> TOP
                    1 -> BOTTOM
                    -1 -> NONE
                    else -> null
                }
            }
        }
    }

    enum class MaximizedColumn constructor(val index: Int) {
        LEFT(0),
        RIGHT(1),
        NONE(-1);

        companion object {

            @JvmStatic
            fun fromIndex(index: Int): MaximizedColumn? {
                return when (index) {
                    0 -> LEFT
                    1 -> RIGHT
                    -1 -> NONE
                    else -> null
                }
            }
        }
    }

    init {
        resetToDefault()
        storeCurrent()
    }

    @Synchronized
    private fun resetToDefault() {
        firstColumnWidth.set(DEFAULT_COLUMN_WIDTH1)
        firstRowHeight.set(DEFAULT_ROW_HEIGHT1)

        isFullScreen = false
    }

    fun resetToLast() {
        LOG.debug("Reset to last {} {}", previousFirstColumnWidth, previousFirstRowHeight)
        firstColumnWidth.set(previousFirstColumnWidth)
        firstRowHeight.set(previousFirstRowHeight)

        isFullScreen = false
    }

    @Synchronized
    private fun storeCurrent() {
        this.previousFirstRowHeight = firstRowHeight.get()
        this.previousFirstColumnWidth = firstColumnWidth.get()
    }

    @Synchronized
    fun maximize(r: MaximizedRow?, c: MaximizedColumn?, steps: Int) {
        LOG.debug("Maximizing cell ({}, {}). Is already maximized? {}", r, c, isFullScreen)
        if (isFullScreen) {
            resetToLast()
            return
        }

        if (r == null || r == MaximizedRow.NONE || c == null || c == MaximizedColumn.NONE) {
            LOG.debug("Arguments null or NONE: {} {}", r, c)
            return
        }

        storeCurrent()
        val isLeft = c == MaximizedColumn.LEFT
        val isTop = r == MaximizedRow.TOP
        val columnStep = (if (isLeft) 100 - firstColumnWidth.get() else firstColumnWidth.get() - 0) / steps
        val rowStep = (if (isTop) 100 - firstRowHeight.get() else firstRowHeight.get() - 0) / steps

        for (i in 0 until steps) {
            firstColumnWidth.set(firstColumnWidth.get() + columnStep)
            firstRowHeight.set(firstRowHeight.get() + rowStep)
        }

        firstColumnWidth.set((if (isLeft) 100 else 0).toDouble())
        firstRowHeight.set((if (isTop) 100 else 0).toDouble())

        LOG.debug("Maximized first column={} first row={}", firstColumnWidth.value, firstRowHeight.value)

        isFullScreen = true
    }

    @Synchronized
    fun maximize(row: MaximizedRow?, steps: Int) {
        LOG.debug("Maximizing row {}. Is already maximized? {}", row, isFullScreen)
        if (isFullScreen) {
            resetToLast()
            return
        }

        if (row == null || row == MaximizedRow.NONE) {
            LOG.debug("Argument null or NONE: {}", row)
            return
        }

        LOG.debug("Maximizing row {}", row)

        storeCurrent()
        val isTop = row == MaximizedRow.TOP
        val rowStep = (if (isTop) 100 - firstRowHeight.get() else firstRowHeight.get() - 0) / steps

        for (i in 0 until steps) {
            firstRowHeight.set(firstRowHeight.get() + rowStep)
        }

        firstRowHeight.set((if (isTop) 100 else 0).toDouble())

        isFullScreen = true
    }

    fun manageGrid(grid: GridPane) = attachToGrid(grid)

    private fun attachToGrid(grid: GridPane) {

        val column1 = ColumnConstraints()
        val column2 = ColumnConstraints()
        column1.percentWidthProperty().bind(this.firstColumnWidth)
        column2.percentWidthProperty().bind(Bindings.subtract(100, this.firstColumnWidth))
        grid.columnConstraints.setAll(column1, column2)

        val row1 = RowConstraints()
        val row2 = RowConstraints()
        row1.percentHeightProperty().bind(this.firstRowHeight)
        row2.percentHeightProperty().bind(Bindings.subtract(100, this.firstRowHeight))
        grid.rowConstraints.setAll(row1, row2)

        column1.percentWidthProperty().addListener { _, _, _ -> updateChildrenVisibilities(grid) }
        column2.percentWidthProperty().addListener { _, _, _ -> updateChildrenVisibilities(grid) }

        // TODO row visibility overrides columnVisibility
        row1.percentHeightProperty().addListener { _, _, _ -> updateChildrenVisibilities(grid) }
        row2.percentHeightProperty().addListener { _, _, _ -> updateChildrenVisibilities(grid) }
    }

    fun firstRowHeightProperty(): DoubleProperty = this.firstRowHeight

    fun firstColumnWidthProperty(): DoubleProperty = this.firstColumnWidth

    fun set(that: GridConstraintsManager) {
        if (this === that) {
            return
        }
        this.isFullScreen = that.isFullScreen
        this.firstColumnWidth.set(that.firstColumnWidth.get())
        this.firstRowHeight.set(that.firstRowHeight.get())
        this.previousFirstColumnWidth = that.previousFirstColumnWidth
        this.previousFirstRowHeight = that.previousFirstRowHeight
    }

    override fun toString(): String {
        return "{${this.javaClass.simpleName}: $previousFirstRowHeight, $previousFirstColumnWidth, ${firstRowHeight.get()}, ${firstColumnWidth.get()}, $isFullScreen}"
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        private val DEFAULT_COLUMN_WIDTH1 = 50.0

        private val DEFAULT_ROW_HEIGHT1 = 50.0

        private fun updateChildrenVisibilities(grid: GridPane) {
            val colConstraints = grid.columnConstraints
            val rowConstraints = grid.rowConstraints
            for (node in grid.children) {
                val r = GridPane.getRowIndex(node)!!
                val c = GridPane.getColumnIndex(node)!!
                node.isVisible = colConstraints[c].percentWidth > 0 && rowConstraints[r].percentHeight > 0
            }
        }

        private fun fromFirstColumnWidth(width: Double): MaximizedColumn {
            return when (width) {
                0.0 -> MaximizedColumn.RIGHT
                100.0 -> MaximizedColumn.LEFT
                else -> MaximizedColumn.NONE
            }
        }

        private fun fromFirstRowHeight(height: Double): MaximizedRow {
            return when (height) {
                0.0 -> MaximizedRow.BOTTOM
                100.0 -> MaximizedRow.TOP
                else -> MaximizedRow.NONE
            }
        }
    }

}
