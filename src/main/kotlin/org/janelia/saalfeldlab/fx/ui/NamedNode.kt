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
