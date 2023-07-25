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

import javafx.application.Platform
import javafx.event.Event
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.SplitPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.Window
import javafx.stage.WindowEvent

class DynamicCellPane @JvmOverloads constructor(vararg nodes: List<Node> = arrayOf()) : SplitPane() {

	init {
		nodes.forEach { cells ->
			items += SplitPane(*cells.toTypedArray())
		}
		orientation = Orientation.VERTICAL
		distributeAllDividers()
	}

	private var maximizedNodes: MutableMap<Node, Pair<Int, Int>>? = null
	private var detachedNodes: MutableList<Node> = mutableListOf()
	val maximized: Boolean
		get() = maximizedNodes != null

	private var rowWhenNotMaximized: List<Node>? = null
	private val rowWhenMaximized by lazy { SplitPane() }


	/**
	 * Add a new row at index [idx] containing [nodes].
	 * If a row already exists at [idx], insert this row before it, and shift all remaining rows down.
	 * If [idx] is greater than current number of rows, then this row will be created at the end.
	 *
	 * @param idx to add the row to.
	 * @param nodes to add to the new row
	 */
	@JvmOverloads
	fun addRow(idx: Int? = null, vararg nodes: Node) {
		val cells = cells()
		/* Can't have duplicate nodes*/
		for (node in nodes) {
			if (node in cells) {
				duplicateNodeError()
			}
		}
		if (idx == null || idx > items.size - 1) {
			items.add(SplitPane(*nodes))
		} else {
			items.add(idx, SplitPane(*nodes))
		}
		distributeAllDividers()
	}

	private fun duplicateNodeError() {
		error("Cannot add the same node to the pane multiple times. ")
	}

	/**
	 * Remove all occurences of [node].
	 *
	 * @param node to remove
	 * @return true if removed, else false
	 */
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


	/**
	 * Remove the node at ([row], [col])
	 *
	 * @param row index
	 * @param col index
	 * @return the node if one was removed, or else null
	 */
	fun remove(row: Int, col: Int): Node? {
		return (items[row] as? SplitPane)?.items?.let { rowItems ->
			val removed = rowItems.removeAt(col)
			if (rowItems.isEmpty()) {
				items.removeAt(row)
			}
			removed
		}
	}

	/**
	 * Remove all nodes at row [idx].
	 *
	 * @param idx of the row to remove
	 * @return the nodes that where removed, or null if no corresponding row at [idx]
	 */
	fun removeRow(idx: Int): List<Node>? {
		return if (idx < items.size) {
			(items.removeAt(idx) as? SplitPane)?.items
		} else {
			null
		}

	}

	/**
	 * Remove all nodes at columns [idx].
	 *
	 * @param idx of the column to remove
	 * @return the nodes that where removed, or null if no corresponding  col at [idx]
	 */
	fun removeColumn(idx: Int): List<Node>? {
		var removed: MutableList<Node>? = null
		items.mapNotNull { it as? SplitPane }.forEach {
			if (idx < it.items.size) {
				removed = removed ?: mutableListOf()
				removed?.add(it.items.removeAt(idx))
			}
		}
		return removed?.toList()
	}

	/**
	 * If [currentNode] is the current node at ([row], [col]), remove it and replace it with [newNode]
	 *
	 * @param currentNode
	 * @param newNode
	 * @param row
	 * @param col
	 * @return
	 */
	fun replace(currentNode: Node, newNode: Node, row: Int, col: Int): Boolean {
		return if (this[row, col] == currentNode) {
			remove(row, col)
			add(row, col, newNode)
			true
		} else {
			false
		}
	}

	/**
	 * Swap the node at ([row1], [col1]) with the node at ([row2], [col2]).
	 * If either coordinate does not contain as node, the swap does not occur
	 *
	 * @param row1 row index of the first node
	 * @param col1 column index of the first node
	 * @param row2 row index of the second node
	 * @param col2 coluimn index of the second node
	 * @return true if the swap occured, otherwise false
	 */
	fun swap(row1: Int, col1: Int, row2: Int, col2: Int): Boolean {
		this[row1, col1]?.let { node1 ->
			this[row2, col2]?.let { node2 ->
				this[row1, col1] = node2
				this[row2, col2] = node1
				return true
			}
		}
		return false

	}

	/**
	 * Add [node] to the pane at ([row], [col]).
	 * If [row] is larger than the current number of rows, see [addRow]
	 * If [col] is larger than the current number of items in the row at [row], add [node] to the end.
	 *
	 * @param row
	 * @param col
	 * @param node
	 */
	fun add(row: Int, col: Int, node: Node) {
		if (node in cells()) {
			duplicateNodeError()
		}
		if (row > items.size - 1) {
			addRow(row, node)
		} else {
			(items[row] as? SplitPane)?.let {
				if (col > it.items.size - 1) {
					it.items.add(node)
				} else {
					it.items.add(col, node)
				}
			}
		}
	}

	operator fun set(row: Int, col: Int, node: Node?): Node? = (items[row] as? SplitPane)?.items?.set(col, node)

	operator fun get(row: Int, col: Int): Node? = (items[row] as? SplitPane)?.items?.getOrNull(col)

	/**
	 * See [indexOf].
	 */
	operator fun get(node: Node?): Pair<Int, Int>? = indexOf(node)

	/**
	 * Determine if the this [DynamicCellPane] contains [node].
	 *
	 * @param node to check for.
	 * @return true if the [node] exists any cell.
	 */
	operator fun contains(node: Node?) = node in cells()


	/**
	 * Get the (row,col) coordinates of [node].
	 *
	 * @param node to get the coordinates for
	 * @return the (row, col) coordinates if [node] is present, else null
	 */
	fun indexOf(node: Node?): Pair<Int, Int>? {
		node?.let {
			for (rowIdx in items.indices) {
				(items[rowIdx] as? SplitPane)?.items?.let {
					for (colIdx in it.indices) {
						if (it[colIdx] == node) {
							return rowIdx to colIdx
						}
					}
				}
			}
		}
		return null
	}

	/**
	 * A flat list of all the nodes, in row-major order.
	 *
	 * @return list of nodes
	 */
	fun cells(): List<Node> {
		return items
			.flatMap { (it as? SplitPane)?.items ?: listOf() }
			.toList()

	}

	/**
	 * Toggle maximized state of [nodes].
	 *
	 * If there are no current maximized nodes, then [nodes] are maximized such that there is a single row, containing all [nodes] in the order specified.
	 * If some nodes are maximized, then [nodes] is ignored, and the state of the cells is returned to how it was before the prior call to [toggleMaximize].
	 *
	 * @param nodes to maximize
	 */
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

	/**
	 * Resize all cells within a rwo to have the same width.
	 */
	fun distributeAllDividers() {
		items.forEach { row -> (row as? SplitPane)?.distributeDividers() }
		distributeDividers()
	}

	/**
	 * Detach a node, such that it is removed from the current [Stage] containing this [DynamicCellPane], and added to its own [Stage].
	 * Handlers are attached to the new [Stage] such that when it is closed, it attemps to re-attach to the original stage.
	 * The root of the new [Stage] is a [StackPane] which contains a [BorderPane], which in turn places [node] at its [BorderPane.setCenter].
	 * [uiCallback] provides the [StackPane] and [BorderPane] for additional modification.
	 *
	 *
	 *
	 * @param node to detach
	 * @param title of the new stage
	 * @param onClose when the stage is closed, this is called.
	 * @param beforeShow prior to showing the new stage, this is called
	 * @param reAttachIndices if porivded, [node] will be added back to this [DynamicCellPane] at these indices, instead of the starting indices
	 * @param uiCallback callback which provides the stackpane and border pane that are created for this detached window.
	 */
	fun toggleNodeDetach(
		node: Node,
		title: String? = null,
		onClose: (Stage) -> Unit = {},
		beforeShow: (Stage) -> Unit = {},
		reAttachIndices: Pair<Int, Int>? = null,
		uiCallback: ((StackPane, BorderPane) -> Unit)? = null,
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

				detachedNodes += node

				val borderPane = BorderPane(node)
				val stackPaneRoot = StackPane(borderPane)

				borderPane.centerProperty().addListener { _, _, new ->
					if (new == null) {
						closeNodeIfDetached(stackPaneRoot)
					}
				}

				stage.scene = Scene(stackPaneRoot, scene.width, scene.height)
				uiCallback?.invoke(stackPaneRoot, borderPane)
				stage.scene.stylesheets.setAll(scene.stylesheets)

				stage.initModality(Modality.NONE)
				beforeShow(stage)


				val cleanUp = {
					if (node in detachedNodes) {
						onClose(stage)
						detachedNodes.remove(node)
						/* if the DynamicCellPane is maximized, unmaximize it first */
						if (maximized) toggleMaximize()

						/* if the cell was already removed, do nothing */
						if (borderPane.center != null) {
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
				}
				stage.onCloseRequest = EventHandler { cleanUp() }
				stage.show()
			}
		}
	}

	/**
	 * remove all nodes from this [DynamicCellPane]
	 *
	 */
	fun removeAll() {
		if (maximized) {
			toggleMaximize()
		}
		detachedNodes.toList().forEach {
			toggleNodeDetach(it)
		}
		while (items.size != 0) {
			removeRow(0)
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
			if (nodeIsDetached) closeDetachedWindow(root.scene.window)
		}
	}

	private fun closeDetachedWindow(window: Window) {
		/* Note; There is a difference in close behavior when you close programatically
		*	(as below) compare to a system close (e.g. clicking X or Alt+F4),
		*	whereby the node doesn't unfocus. To resolve, manually request focus for the main scene first.
		* */
		scene.root.requestFocus()
		window.hide()
		Event.fireEvent(window, WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST))
	}
}

fun main() {
	Platform.startup { }
	Platform.setImplicitExit(true)
	Platform.runLater {
		val cellPane = DynamicCellPane(
			listOf(Label("Test"), Label("Test")),
			listOf(Label("Test"), Label("Test"))
		)
		val scene = Scene(cellPane)
		val stage = Stage()
		stage.scene = scene
		stage.width = 800.0
		stage.height = 600.0
		stage.show()
	}
}
