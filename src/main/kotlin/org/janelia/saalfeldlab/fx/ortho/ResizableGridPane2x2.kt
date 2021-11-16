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
import org.janelia.saalfeldlab.fx.extensions.nonnull

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
        bottomRight: BR,
) : GridPane() {

    init {
        hgap = 1.0
        vgap = 1.0
    }

    val topLeftProperty: ObjectProperty<TL> = SimpleObjectProperty<TL>().apply {
        addListener { _, oldv, newv -> replace(oldv, newv, 0, 0) }
        value = topLeft
    }
    var topLeft by topLeftProperty.nonnull()

    val topRightProperty: ObjectProperty<TR> = SimpleObjectProperty<TR>().apply {
        addListener { _, oldv, newv -> replace(oldv, newv, 1, 0) }
        value = topRight
    }
    var topRight by topRightProperty.nonnull()

    val bottomLeftProperty: ObjectProperty<BL> = SimpleObjectProperty<BL>().apply {
        addListener { _, oldv, newv -> replace(oldv, newv, 0, 1) }
        value = bottomLeft
    }
    var bottomLeft by bottomLeftProperty.nonnull()

    val bottomRightProperty: ObjectProperty<BR> = SimpleObjectProperty<BR>().apply {
        addListener { _, oldv, newv -> replace(oldv, newv, 1, 1) }
        value = bottomRight
    }
    var bottomRight by bottomRightProperty.nonnull()

    /**
     * Manage the underlying [GridPane] with a [GridConstraintsManager].
     *
     * @param manager controls grid cell proportions
     */
    fun manage(manager: GridConstraintsManager) = manager.manageGrid(this)

    fun getNodeAt(col: Int, row: Int): Node? = children.firstOrNull { it.columnIndex == col && it.rowIndex == row }

    private fun replace(oldValue: Node?, newValue: Node?, col: Int, row: Int) {
        children.remove(oldValue)
        add(newValue, col, row)
    }

    companion object {
        private val Node.columnIndex: Int
            get() = getColumnIndex(this)

        private val Node.rowIndex: Int
            get() = getRowIndex(this)

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
