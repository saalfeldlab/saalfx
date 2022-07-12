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
import javafx.scene.Cursor
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.*
import javafx.scene.layout.Pane
import javafx.util.Duration
import org.janelia.saalfeldlab.fx.actions.ActionSet
import org.janelia.saalfeldlab.fx.event.KeyTracker
import kotlin.math.abs

class GridResizer(private val manager: GridConstraintsManager, private val tolerance: Double, private val grid: Pane, keyTracker: KeyTracker) : ActionSet("Grid Resizer", keyTracker) {

    private var trackedMouseRange = MouseInsideRange(xInRange = false, yInRange = false)
    private var onResideStartMouseRange = MouseInsideRange(xInRange = false, yInRange = false)

    var isDraggingPanel = false
        private set

    private var isOnMargin: Boolean = false

    init {
        /* keep track of where the mouse is with respect to the resize locations */
        MOUSE_MOVED {
            filter = true
            verifyNoKeysDown()
            verifyEventNotNull()
            verify { event ->
                mouseInRange(event!!)
                    //Not sure how I feel about changing state in the `verify`.
                    // Alternatively, could have a second MOVED action, but then you have to gaurantee ordering
                    // ( I think that would be fine though... )
                    .also { trackedMouseRange = it }
                    .run { xInRange || yInRange || isOnMargin }
            }
            onAction { event ->
                event!!
                synchronized(manager) {
                    synchronized(grid) {
                        val gridBorderX = manager.firstColumnWidthProperty().get() / 100 * grid.widthProperty().get()
                        val gridBorderY = manager.firstRowHeightProperty().get() / 100 * grid.heightProperty().get()
                        val (mouseWithinResizableRangeX, mouseWithinResizableRangeY) = trackedMouseRange

                        val scene = grid.sceneProperty().get()

                        if (mouseWithinResizableRangeX && mouseWithinResizableRangeY) {
                            // TODO replace compareTo with operator comparison
                            scene.cursor =
                                if ((event.x - gridBorderX).compareTo(0.0) < 0 && (event.y - gridBorderY).compareTo(0.0) < 0) Cursor.SE_RESIZE
                                else if ((event.x - gridBorderX).compareTo(0.0) > 0 && (event.y - gridBorderY).compareTo(0.0) < 0) Cursor.SW_RESIZE
                                else if ((event.x - gridBorderX).compareTo(0.0) < 0 && (event.y - gridBorderY).compareTo(0.0) > 0) Cursor.NE_RESIZE
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
        /* On Primary Press, */
        MOUSE_PRESSED(MouseButton.PRIMARY) {
            filter = true
            verify { trackedMouseRange.run { xInRange || yInRange } }
            onAction {
                trackedMouseRange.run {
                    onResideStartMouseRange = MouseInsideRange(xInRange, yInRange)
                }
                isDraggingPanel = true
            }
        }

        MOUSE_RELEASED(MouseButton.PRIMARY, released = true) {
            filter = true
            verify { isDraggingPanel }
            onAction {
                isDraggingPanel = false
                grid.scene.cursor = Cursor.DEFAULT
            }
        }

        MOUSE_DRAGGED {
            filter = true
            verify { isDraggingPanel }
            verifyEventNotNull()
            onAction { event ->
                event!!
                val width = grid.widthProperty().get()
                val height = grid.heightProperty().get()
                val stopX = event.x
                val stopY = event.y

                if (onResideStartMouseRange.xInRange) {
                    val percentWidth = (stopX * 100.0 / width).coerceIn(20.0, 80.0)
                    manager.firstColumnWidthProperty().set(percentWidth)
                }

                if (onResideStartMouseRange.yInRange) {
                    val percentHeight = (stopY * 100.0 / height).coerceIn(20.0, 80.0)
                    manager.firstRowHeightProperty().set(percentHeight)
                }
            }
        }
        MOUSE_CLICKED {
            filter = true
            verifyEventNotNull()
            verify { it!!.clickCount == 2 }
            verify { trackedMouseRange.run { xInRange || yInRange } }
            onAction { event ->
                trackedMouseRange.run {

                    val time = 300
                    event!!.consume()
                    val timeline = Timeline()

                    if (xInRange && yInRange) {
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
                    } else if (xInRange) {
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
                    } else if (yInRange) {
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

    private data class MouseInsideRange(val xInRange: Boolean, val yInRange: Boolean)


    private fun mouseInRange(event: MouseEvent): MouseInsideRange {
        val x = event.x
        val y = event.y
        val gridBorderX = manager.firstColumnWidthProperty().get() / 100 * grid.widthProperty().get()
        val gridBorderY = manager.firstRowHeightProperty().get() / 100 * grid.heightProperty().get()
        val mouseWithinResizableRangeX = abs(x - gridBorderX) < tolerance
        val mouseWithinResizableRangeY = abs(y - gridBorderY) < tolerance
        return MouseInsideRange(mouseWithinResizableRangeX, mouseWithinResizableRangeY)
    }
}
