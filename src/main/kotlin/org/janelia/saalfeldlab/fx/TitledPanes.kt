package org.janelia.saalfeldlab.fx

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.TitledPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import java.util.*
import java.util.function.Consumer

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
            val spacer = Region()
                    .also { it.maxWidth = java.lang.Double.POSITIVE_INFINITY }
                    .also { it.minWidth = 0.0 }
                    .also { HBox.setHgrow(it, Priority.ALWAYS) }
            val graphicsContents = HBox(left, spacer, right)
                    .also { it.alignment = Pos.CENTER }
                    .also { it.padding = Insets(0.0, 35.0, 0.0, 0.0) }
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
            Optional.ofNullable(isExpanded).ifPresent(Consumer<Boolean> { pane.isExpanded = it })
            Optional.ofNullable(graphic).ifPresent(Consumer<Node> { pane.graphic = it })
            Optional.ofNullable(padding).ifPresent(Consumer<Insets> { pane.padding = it })
            Optional.ofNullable(alignment).ifPresent(Consumer<Pos> { pane.alignment = it })
            return pane
        }

    }

}
