package org.janelia.saalfeldlab.fx.ui

import java.util.Optional
import java.util.function.DoublePredicate

import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.event.EventHandler
import javafx.geometry.Bounds
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import org.janelia.saalfeldlab.fx.event.MouseDragFX
import kotlin.math.abs

class ResizeOnLeftSide @JvmOverloads constructor(
        private val node: Node,
        private val width: DoubleProperty,
        // TODO minWidth/maxWidth should probably be suppliers!
        private val minWidth: Double = 50.0,
        private val maxWidth: Double = 500.0,
        private val isWithinMarginOfBorder: DoublePredicate = DoublePredicate { abs(it) < 5 }) {

    private val isCurrentlyWithinMarginOfBorder = SimpleBooleanProperty(false)

    private val mouseMoved = MouseMoved()

    private val mouseDragged: MouseDragFX

    init {

        this.mouseDragged = object : MouseDragFX(
                "resize",
                { isCurrentlyWithinMarginOfBorder.get() },
                true,
                this,
                false) {

            override fun initDrag(event: MouseEvent) = node.scene.setCursor(Cursor.W_RESIZE)


            override fun drag(event: MouseEvent) {
                val bounds = node.localToScene(node.boundsInLocal)
                val dx = event.sceneX - bounds.minX
                this@ResizeOnLeftSide.width.set(Math.min(
                        Math.max(width.get() - dx, this@ResizeOnLeftSide.minWidth),
                        this@ResizeOnLeftSide.maxWidth))
            }

            override fun endDrag(event: MouseEvent) {
                node.scene.cursor = Cursor.DEFAULT
            }
        }

        isCurrentlyWithinMarginOfBorder.addListener { obs, oldv, newv ->
            if (!mouseDragged.isDraggingProperty().get()) {
                Optional.ofNullable(node.scene).ifPresent { s ->
                    s.cursor = if (newv)
                        Cursor.W_RESIZE
                    else
                        Cursor.DEFAULT
                }
            }
        }
    }

    fun install() {
        node.parent.addEventFilter(MouseEvent.MOUSE_MOVED, mouseMoved)
        this.mouseDragged.installIntoAsFilter(node.parent)
    }

    fun remove() {
        node.parent.removeEventFilter(MouseEvent.MOUSE_MOVED, mouseMoved)
        this.mouseDragged.removeFromAsFilter(node.parent)
        this.isCurrentlyWithinMarginOfBorder.set(false)
        this.mouseDragged.abortDrag()
    }

    private inner class MouseMoved : EventHandler<MouseEvent> {

        override fun handle(event: MouseEvent) {
            val bounds = node.boundsInParent
            val y = event.y
            isCurrentlyWithinMarginOfBorder.set(y >= bounds.minY && y <= bounds.maxY && isWithinMarginOfBorder.test(event.x - bounds.minX))
        }
    }

}
