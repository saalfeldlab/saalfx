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

import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.event.EventHandler
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import org.janelia.saalfeldlab.fx.actions.ActionSet.Companion.installActionSet
import org.janelia.saalfeldlab.fx.actions.ActionSet.Companion.removeActionSet
import org.janelia.saalfeldlab.fx.actions.DragActionSet
import org.janelia.saalfeldlab.fx.extensions.nonnullVal
import java.util.function.DoublePredicate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ResizeOnLeftSide @JvmOverloads constructor(
	private val node: Node,
	private val width: DoubleProperty,
	// TODO minWidth/maxWidth should probably be suppliers!
	private val minWidth: Double = 50.0,
	private val maxWidth: Double = 500.0,
	private val isWithinMarginOfBorder: DoublePredicate = DoublePredicate { abs(it) < 5 }
) {

	private val isCurrentlyWithinMarginOfBorderProperty = SimpleBooleanProperty(false)
	val isCurrentlyWithinMarginOfBorder: Boolean by isCurrentlyWithinMarginOfBorderProperty.nonnullVal()

	private val mouseMoved = EventHandler<MouseEvent> {
		val bounds = node.boundsInParent
		val y = it.y
		isCurrentlyWithinMarginOfBorderProperty.set(y >= bounds.minY && y <= bounds.maxY && isWithinMarginOfBorder.test(it.x - bounds.minX))
	}

	private val mouseDragged = DragActionSet("resize") {
		verify { isCurrentlyWithinMarginOfBorder }

		updateXY = false

		dragDetectedAction.filter = true
		dragAction.filter = true
		dragReleaseAction.filter = true

		onDragDetected { node.scene.cursor = Cursor.W_RESIZE }
		onDrag {
			val bounds = node.localToScene(node.boundsInLocal)
			val dx = it.sceneX - bounds.minX
			this@ResizeOnLeftSide.width.set(min(max(width.get() - dx, this@ResizeOnLeftSide.minWidth), this@ResizeOnLeftSide.maxWidth))
		}
		onDragReleased { node.scene.cursor = Cursor.DEFAULT }
	}

	init {

		isCurrentlyWithinMarginOfBorderProperty.addListener { _, _, newv ->
			if (!mouseDragged.isDragging) {
				node.scene?.run { cursor = if (newv) Cursor.W_RESIZE else Cursor.DEFAULT }
			}
		}
	}

	fun install() {
		node.parent.addEventFilter(MouseEvent.MOUSE_MOVED, mouseMoved)
		node.parent.installActionSet(mouseDragged)
	}

	fun remove() {
		node.parent.removeEventFilter(MouseEvent.MOUSE_MOVED, mouseMoved)
		node.parent.removeActionSet(mouseDragged)
		this.isCurrentlyWithinMarginOfBorderProperty.set(false)
	}

}
