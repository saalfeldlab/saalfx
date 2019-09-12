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

import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.input.MouseEvent

import java.util.function.Consumer
import java.util.function.Predicate

class MouseClickFX(
        name: String,
        private val onPressConsumer: Consumer<MouseEvent>,
        private val onReleaseConsumer: Consumer<MouseEvent>,
        private val eventFilter: Predicate<MouseEvent>) : InstallAndRemove<Node> {

    private val onPress: EventFX<MouseEvent> = EventFX.MOUSE_PRESSED(name, Consumer { this.press(it) }, this.eventFilter)

    private val onRelease: EventFX<MouseEvent> = EventFX.MOUSE_RELEASED(name, Consumer { this.release(it) }, Predicate { isEvent })

    private var startX: Double = 0.0

    private var startY: Double = 0.0

    private var isEvent: Boolean = false

    private val tolerance = 1.0

    constructor(
            name: String,
            onReleaseConsumer: Consumer<MouseEvent>,
            eventFilter: Predicate<MouseEvent>) : this(name, Consumer {}, onReleaseConsumer, eventFilter)

    private fun testEvent(event: MouseEvent) = eventFilter.test(event)

    private fun press(event: MouseEvent) {
        if (testEvent(event)) {
            startX = event.x
            startY = event.y
            isEvent = true
            onPressConsumer.accept(event)
        }
    }

    private fun release(event: MouseEvent) {
        val x = event.x
        val y = event.y
        val dX = x - startX
        val dY = y - startY
        if (dX * dX + dY * dY <= tolerance * tolerance) {
            onReleaseConsumer.accept(event)
        }
        isEvent = false
    }

    override fun installInto(t: Node) {
        onPress.installInto(t)
        onRelease.installInto(t)
    }

    override fun removeFrom(t: Node) {
        onPress.removeFrom(t)
        onRelease.removeFrom(t)
    }

    val handler: EventHandler<MouseEvent>
        get() = EventHandler { event ->
            if (MouseEvent.MOUSE_PRESSED == event.getEventType())
                onPress.handle(event)
            else if (MouseEvent.MOUSE_RELEASED == event.getEventType())
                onRelease.handle(event)

        }

    @Deprecated("Use getter syntax instead", ReplaceWith("getHandler()"))
    fun handler() = handler

}
