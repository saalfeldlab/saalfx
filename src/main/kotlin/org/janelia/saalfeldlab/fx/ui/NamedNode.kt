package org.janelia.saalfeldlab.fx.ui

import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region

class NamedNode private constructor(
        name: String,
        nameWidth: Double,
        growNodes: Boolean,
        vararg nodes: Node) {

    private val name = Label(name)
            .also { it.prefWidth = nameWidth }
            .also { it.minWidth = nameWidth }
            .also { it.maxWidth = nameWidth }

    private val contents = HBox(this.name).also { it.children.addAll(nodes) }

    init {
        if (growNodes)
            nodes.forEach { n -> HBox.setHgrow(n, Priority.ALWAYS) }
    }

    fun addNameToolTip(tooltip: Tooltip) {
        this.name.tooltip = tooltip
    }

    companion object {

        @JvmStatic
        fun nameIt(name: String, nameWidth: Double, growNodes: Boolean, vararg nodes: Node): Node {
            return NamedNode(name, nameWidth, growNodes, *nodes).contents
        }

        @JvmStatic
        fun bufferNode(n: Node = Region().also { it.minWidth = 0.0 }) = n.also { HBox.setHgrow(it, Priority.ALWAYS) }
    }
}
