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
package org.janelia.saalfeldlab.fx

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.TitledPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import org.janelia.saalfeldlab.fx.ui.NamedNode
import java.util.Optional

class TitledPanes {

    companion object {

        @JvmStatic
        fun createCollapsed(title: String?, contents: Node?): TitledPane {
            val tp = TitledPane(title, contents)
            tp.isExpanded = false
            return tp
        }

        @JvmStatic
        fun graphicsOnly(
                pane: TitledPane,
                title: String?,
                right: Node?): TitledPane {
            return graphicsOnly(pane, Label(title), right)
        }

        @JvmStatic
        fun graphicsOnly(
                pane: TitledPane,
                left: Node?,
                right: Node?): TitledPane {
            val spacer = NamedNode.bufferNode()
            val graphicsContents = HBox(left, spacer, right).apply {
                alignment = Pos.CENTER
                padding = Insets(0.0, 35.0, 0.0, 0.0)
            }
            return graphicsOnly(pane, graphicsContents)
        }

        @JvmStatic
        fun graphicsOnly(
                pane: TitledPane,
                graphicsContentPane: Pane): TitledPane {
            graphicsContentPane.minWidthProperty().bind(pane.widthProperty())
            pane.graphic = graphicsContentPane
            pane.text = null
            return pane
        }

        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    class Builder constructor() {

        private var title: String? = null
        private var isExpanded: Boolean? = null
        private var graphic: Node? = null
        private var padding: Insets? = null
        private var content: Node? = null
        private var alignment: Pos? = null

        fun withTitle(title: String): Builder {
            this.title = title
            return this
        }

        fun setIsExpanded(isExpanded: Boolean?): Builder {
            this.isExpanded = isExpanded
            return this
        }

        fun collapsed(): Builder {
            return setIsExpanded(false)
        }

        fun withGraphic(graphic: Node): Builder {
            this.graphic = graphic
            return this
        }

        fun withPadding(padding: Insets): Builder {
            this.padding = padding
            return this
        }

        fun zeroPadding(): Builder {
            return withPadding(Insets.EMPTY)
        }

        fun withContent(content: Node): Builder {
            this.content = content
            return this
        }

        fun withAlignment(alignment: Pos): Builder {
            this.alignment = alignment
            return this
        }

        fun build(): TitledPane {
            val pane = TitledPane(title, content)
            Optional.ofNullable(isExpanded).ifPresent { pane.isExpanded = it }
            Optional.ofNullable(graphic).ifPresent { pane.graphic = it }
            Optional.ofNullable(padding).ifPresent { pane.padding = it }
            Optional.ofNullable(alignment).ifPresent { pane.alignment = it }
            return pane
        }

    }

}
