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

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Node
import javafx.scene.layout.GridPane

/**
 * A wrapper around [GridPane] that holds for children organized in a 2x2 grid. The underlying
 * [GridPane] is exposed through [.pane] and can be managed with a [GridConstraintsManager]
 * that is passed to [.manage].
 *
 * @param <TL> type of top left child
 * @param <TR> type of top right child
 * @param <BL> type of bottom left child
 * @param <BR></BR> type of bottom right child
</BL></TR></TL> */
class ResizableGridPane2x2<TL : Node?, TR : Node?, BL : Node?, BR : Node?>
/**
 *
 * @param topLeft top left child
 * @param topRight top right child
 * @param bottomLeft bottom left child
 * @param bottomRight bottom right child
 */
(
        topLeft: TL,
        topRight: TR,
        bottomLeft: BL,
        bottomRight: BR) {

    val grid = GridPane()
            .also { it.hgap = 1.0 }
            .also { it.vgap = 1.0 }

    /**
     *
     * @return underlying [GridPane]
     */
    @Deprecated("Use getter syntax instead", ReplaceWith("getGrid()"))
    fun pane(): GridPane = grid

    private val _topLeft: ObjectProperty<TL> = SimpleObjectProperty<TL>()
            .also{ it.addListener { _, oldv, newv -> replace(grid, oldv, newv, 0, 0) } }
            .also { it.value = topLeft }
    fun topLeftProperty(): ObjectProperty<TL> = _topLeft
    var topLeft: TL
        get() = _topLeft.value
        set(topLeft) = _topLeft.set(topLeft)

    private val _topRight: ObjectProperty<TR> = SimpleObjectProperty<TR>()
            .also{ it.addListener { _, oldv, newv -> replace(grid, oldv, newv, 1, 0) } }
            .also { it.value = topRight }
    fun topRightProperty(): ObjectProperty<TR> = _topRight
    var topRight: TR
        get() = _topRight.value
        set(topRight) = _topRight.set(topRight)

    private val _bottomLeft: ObjectProperty<BL> = SimpleObjectProperty<BL>()
            .also{ it.addListener { _, oldv, newv -> replace(grid, oldv, newv, 0, 1) } }
            .also { it.value = bottomLeft }
    fun bottomLeftProperty(): ObjectProperty<BL> = _bottomLeft
    var bottomLeft: BL
        get() = _bottomLeft.value
        set(bottomLeft) = _bottomLeft.set(bottomLeft)

    private val _bottomRight: ObjectProperty<BR> = SimpleObjectProperty<BR>()
            .also{ it.addListener { _, oldv, newv -> replace(grid, oldv, newv, 1, 1) } }
            .also { it.value = bottomRight }
    fun bottomRightProperty(): ObjectProperty<BR> = _bottomRight
    var bottomRight: BR
        get() = _bottomRight.value
        set(bottomRight) = _bottomRight.set(bottomRight)

    /**
     * Manage the underlying [GridPane] with a [GridConstraintsManager].
     *
     * @param manager controls grid cell proportions
     */
    fun manage(manager: GridConstraintsManager) = manager.manageGrid(this.grid)

    fun getNodeAt(col: Int, row: Int): Node? = grid.children.firstOrNull { it.columnIndex == col && it.rowIndex == row }

    private fun replace(grid: GridPane, oldValue: Node?, newValue: Node?, col: Int, row: Int) {
        grid.children.remove(oldValue)
        grid.add(newValue, col, row)
    }

    companion object {
        private val Node.columnIndex: Int
            get() = GridPane.getColumnIndex(this)

        private val Node.rowIndex: Int
            get() = GridPane.getRowIndex(this)
    }
}
