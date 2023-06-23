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
package org.janelia.saalfeldlab.fx.event

import javafx.beans.property.SimpleDoubleProperty
import javafx.event.EventHandler
import javafx.scene.input.MouseEvent
import org.janelia.saalfeldlab.fx.extensions.nonnull
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class MouseTracker : EventHandler<MouseEvent> {

	var isDragging: Boolean = false
		private set

	val xProperty = SimpleDoubleProperty(0.0)
	val yProperty = SimpleDoubleProperty(0.0)

	var x by xProperty.nonnull()
		private set
	var y by yProperty.nonnull()
		private set

	override fun handle(event: MouseEvent) {
		if (event.eventType == MouseEvent.MOUSE_PRESSED)
			this.isDragging = false
		else if (event.eventType == MouseEvent.DRAG_DETECTED)
			this.isDragging = true
		LOG.trace("Updated x {}->{} and y {}->{}", x, event.x, y, event.y)
		x = event.x
		y = event.y
	}

	companion object {
		private val LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
	}

}
