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
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.input.MouseEvent.MOUSE_MOVED
import org.janelia.saalfeldlab.fx.actions.DragActionSet
import org.janelia.saalfeldlab.fx.extensions.nonnull
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
) : DragActionSet("resize-on-left") {

	private val isCurrentlyWithinMarginOfBorderProperty = SimpleBooleanProperty(false)
	internal var isCurrentlyWithinMarginOfBorder: Boolean by isCurrentlyWithinMarginOfBorderProperty.nonnull()
		private set

	init {
		verify { isCurrentlyWithinMarginOfBorder }

		relative = false

		dragDetectedAction.filter = true
		dragAction.filter = true
		dragReleaseAction.filter = true


		onDragDetected { node.scene.cursor = Cursor.W_RESIZE }
		onDrag {
			val bounds = node.localToScene(node.boundsInLocal)
			val dx = it.sceneX - bounds.minX
			width.set(min(max(width.get() - dx, minWidth), maxWidth))
		}
		onDragReleased { node.scene.cursor = Cursor.DEFAULT }

		val withinMarginOfBorderAction = MOUSE_MOVED {
			filter = true
			consume = false
			onAction { event ->
				event ?: return@onAction

				val bounds = node.boundsInParent
				val inHeightBounds = event.y >= bounds.minY && event.y <= bounds.maxY
				val nearBorder = isWithinMarginOfBorder.test(event.x - bounds.minX)
				isCurrentlyWithinMarginOfBorder = inHeightBounds && nearBorder
			}
		}

		isCurrentlyWithinMarginOfBorderProperty.subscribe { withinMarginOfBorder ->
			if (!withinMarginOfBorderAction.isValid(null)) {
				node.scene?.run { cursor = Cursor.DEFAULT }
				return@subscribe
			}
			if (isDragging) return@subscribe

			node.scene?.run { cursor = if (withinMarginOfBorder) Cursor.W_RESIZE else Cursor.DEFAULT }
		}
	}

	fun install() {
		node.parent.installActionSet(this)
	}

	fun remove() {
		node.parent.removeActionSet(this)
		isCurrentlyWithinMarginOfBorder = false
	}
}
