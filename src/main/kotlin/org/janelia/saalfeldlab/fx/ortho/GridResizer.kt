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

import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.event.EventHandler
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.util.Duration
import org.janelia.saalfeldlab.fx.event.InstallAndRemove
import org.janelia.saalfeldlab.fx.event.KeyTracker
import kotlin.math.abs

class GridResizer(private val manager: GridConstraintsManager, private val tolerance: Double, private val grid: Pane, private val keyTracker: KeyTracker) : InstallAndRemove<Node> {

    private var mouseWithinResizableRangeX = false

    private var mouseWithinResizableRangeY = false

    var isDraggingPanel = false
        private set

    private var isOnMargin: Boolean = false

    private val mouseMoved = MouseChanged()

    private val mosuePressed = MousePressed()

    private val mouseDragged = MouseDragged()

    private val mouseDoublClicked = MouseDoubleClicked()

    private val mouseReleased = MouseReleased()

    private inner class MouseChanged : EventHandler<MouseEvent> {
        override fun handle(event: MouseEvent) {
            if (!keyTracker.noKeysActive()) {
                return
            }
            synchronized(manager) {
                synchronized(grid) {
                    val x = event.x
                    val y = event.y
                    val gridBorderX = manager.firstColumnWidthProperty().get() / 100 * grid
                            .widthProperty().get()
                    val gridBorderY = manager.firstRowHeightProperty().get() / 100 * grid
                            .heightProperty().get()
                    val mouseWithinResizableRangeX = Math.abs(x - gridBorderX) < tolerance
                    val mouseWithinResizableRangeY = Math.abs(y - gridBorderY) < tolerance

                    val scene = grid.sceneProperty().get()

                    if (mouseWithinResizableRangeX && mouseWithinResizableRangeY) {
                        // TODO replace compareTo with operator comparison
                        scene.cursor =
                                if ((x - gridBorderX).compareTo(0.0) < 0 && (y - gridBorderY).compareTo(0.0) < 0) Cursor.SE_RESIZE
                                else if ((x - gridBorderX).compareTo(0.0) > 0 && (y - gridBorderY).compareTo(0.0) < 0) Cursor.SW_RESIZE
                                else if ((x - gridBorderX).compareTo(0.0) < 0 && (y - gridBorderY).compareTo(0.0) > 0) Cursor.NE_RESIZE
                                else Cursor.NW_RESIZE
                        isOnMargin = true
                    } else if (mouseWithinResizableRangeX) {
                        scene.cursor = Cursor.H_RESIZE
                        isOnMargin = true
                    } else if (mouseWithinResizableRangeY) {
                        scene.cursor = Cursor.V_RESIZE
                        isOnMargin = true
                    } else if (isOnMargin) {
                        scene.cursor = Cursor.DEFAULT
                        isOnMargin = false
                    }
                }
            }
        }
    }

    private inner class MousePressed : EventHandler<MouseEvent> {

        override fun handle(event: MouseEvent) {
            val x = event.x
            val y = event.y
            val gridBorderX = manager.firstColumnWidthProperty().get() / 100 * grid.widthProperty().get()
            val gridBorderY = manager.firstRowHeightProperty().get() / 100 * grid.heightProperty().get()

            mouseWithinResizableRangeX = abs(x - gridBorderX) < tolerance
            mouseWithinResizableRangeY = abs(y - gridBorderY) < tolerance

            isDraggingPanel = mouseWithinResizableRangeX || mouseWithinResizableRangeY
            if (isDraggingPanel)
                event.consume()
        }
    }

    private inner class MouseReleased : EventHandler<MouseEvent> {

        override fun handle(event: MouseEvent) {
            isDraggingPanel = false
            grid.sceneProperty().get().cursor = Cursor.DEFAULT
        }

    }

    private inner class MouseDragged : EventHandler<MouseEvent> {

        override fun handle(event: MouseEvent) {
            if (isDraggingPanel) {
                val width = grid.widthProperty().get()
                val height = grid.heightProperty().get()
                val stopX = event.x
                val stopY = event.y

                if (mouseWithinResizableRangeX) {
                    val percentWidth = Math.min(Math.max(stopX * 100.0 / width, 20.0), 80.0)
                    manager.firstColumnWidthProperty().set(percentWidth)
                }

                if (mouseWithinResizableRangeY) {
                    val percentHeight = Math.min(Math.max(stopY * 100.0 / height, 20.0), 80.0)
                    manager.firstRowHeightProperty().set(percentHeight)
                }

                event.consume()
            }

        }

    }

    private inner class MouseDoubleClicked : EventHandler<MouseEvent> {
        override fun handle(event: MouseEvent) {
            if (event.clickCount == 2) {
                val x = event.x
                val y = event.y
                val gridBorderX = manager.firstColumnWidthProperty().get() / 100 * grid
                        .widthProperty().get()
                val gridBorderY = manager.firstRowHeightProperty().get() / 100 * grid
                        .heightProperty().get()
                val mouseWithinResizableRangeX = Math.abs(x - gridBorderX) < tolerance
                val mouseWithinResizableRangeY = Math.abs(y - gridBorderY) < tolerance

                if (mouseWithinResizableRangeX || mouseWithinResizableRangeY) {
                    val time = 300
                    event.consume()
                    val timeline = Timeline()

                    if (mouseWithinResizableRangeX && mouseWithinResizableRangeY) {
                        timeline.keyFrames.addAll(
                                KeyFrame(
                                        Duration.ZERO,
                                        KeyValue(
                                                manager.firstColumnWidthProperty(),
                                                manager.firstColumnWidthProperty().get()
                                        ),
                                        KeyValue(
                                                manager.firstRowHeightProperty(),
                                                manager.firstRowHeightProperty().get()
                                        )
                                ),
                                KeyFrame(
                                        Duration(time.toDouble()),
                                        KeyValue(manager.firstColumnWidthProperty(), 50),
                                        KeyValue(manager.firstRowHeightProperty(), 50)
                                )
                        )
                    } else if (mouseWithinResizableRangeX) {
                        timeline.keyFrames.addAll(
                                KeyFrame(
                                        Duration.ZERO,
                                        KeyValue(
                                                manager.firstColumnWidthProperty(),
                                                manager.firstColumnWidthProperty().get()
                                        )
                                ),
                                KeyFrame(
                                        Duration(time.toDouble()),
                                        KeyValue(manager.firstColumnWidthProperty(), 50)
                                )
                        )
                    } else if (mouseWithinResizableRangeY) {
                        timeline.keyFrames.addAll(
                                KeyFrame(
                                        Duration.ZERO,
                                        KeyValue(
                                                manager.firstRowHeightProperty(),
                                                manager.firstRowHeightProperty().get()
                                        )
                                ),
                                KeyFrame(
                                        Duration(time.toDouble()),
                                        KeyValue(manager.firstRowHeightProperty(), 50)
                                )
                        )
                    }
                    timeline.play()
                }
            }
        }
    }

    override fun installInto(t: Node) {
        t.addEventFilter(MouseEvent.MOUSE_MOVED, mouseMoved)
        t.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseDoublClicked)
        t.addEventFilter(MouseEvent.MOUSE_DRAGGED, mouseDragged)
        t.addEventFilter(MouseEvent.MOUSE_PRESSED, mosuePressed)
        t.addEventFilter(MouseEvent.MOUSE_RELEASED, mouseReleased)
    }

    override fun removeFrom(t: Node) {
        t.removeEventFilter(MouseEvent.MOUSE_MOVED, mouseMoved)
        t.removeEventFilter(MouseEvent.MOUSE_CLICKED, mouseDoublClicked)
        t.removeEventFilter(MouseEvent.MOUSE_DRAGGED, mouseDragged)
        t.removeEventFilter(MouseEvent.MOUSE_PRESSED, mosuePressed)
        t.removeEventFilter(MouseEvent.MOUSE_RELEASED, mouseReleased)
    }
}
