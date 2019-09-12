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
import java.util.function.Predicate
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
                Predicate { isCurrentlyWithinMarginOfBorder.get() },
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
            if (!mouseDragged.isDraggingProperty.get()) {
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
