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

import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.EventType
import javafx.scene.Node
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import java.util.function.Consumer
import java.util.function.Predicate

abstract class EventFX<E : Event>(
        val name: String,
        val eventType: EventType<E>,
        private val eventFilter: Predicate<E>,
) : EventHandler<E>, InstallAndRemove<Node> {

    abstract fun actOn(event: E)

    override fun installInto(t: Node) = t.addEventHandler(eventType, this)

    override fun removeFrom(t: Node) = t.removeEventHandler(eventType, this)

    override fun handle(e: E) = e.takeIf { eventFilter.test(it) }?.let { actOn(it) } ?: Unit

    class EventFXWithConsumer<E : Event> @JvmOverloads constructor(
            name: String,
            eventType: EventType<E>,
            private val eventHandler: Consumer<E>,
            eventFilter: Predicate<E>,
            private val consume: Boolean = false,
    ) : EventFX<E>(name, eventType, eventFilter) {

        override fun actOn(event: E) {
            if (consume)
                event.consume()
            eventHandler.accept(event)
        }

    }

    companion object {

        @JvmStatic
        @JvmOverloads
        fun KEY_PRESSED(name: String, eventHandler: Consumer<KeyEvent>, eventFilter: Predicate<KeyEvent> = Predicate<KeyEvent> { true }): EventFX<KeyEvent> =
            EventFXWithConsumer(name, KeyEvent.KEY_PRESSED, eventHandler, eventFilter)

        @JvmStatic
        @JvmOverloads
        fun KEY_RELEASED(name: String, eventHandler: Consumer<KeyEvent>, eventFilter: Predicate<KeyEvent> = Predicate<KeyEvent> { true }, consume: Boolean = true): EventFX<KeyEvent> =
            EventFXWithConsumer(name, KeyEvent.KEY_RELEASED, eventHandler, eventFilter, consume)

        @JvmStatic
        @JvmOverloads
        fun KEY_TYPED(name: String, eventHandler: Consumer<KeyEvent>, eventFilter: Predicate<KeyEvent> = Predicate<KeyEvent> { true }): EventFX<KeyEvent> =
            EventFXWithConsumer(name, KeyEvent.KEY_TYPED, eventHandler, eventFilter)

        @JvmStatic
        @JvmOverloads
        fun MOUSE_CLICKED(name: String, eventHandler: Consumer<MouseEvent>, eventFilter: Predicate<MouseEvent> = Predicate<MouseEvent> { true }): EventFX<MouseEvent> =
            EventFXWithConsumer(name, MouseEvent.MOUSE_CLICKED, eventHandler, eventFilter)

        @JvmStatic
        @JvmOverloads
        fun MOUSE_PRESSED(name: String, eventHandler: Consumer<MouseEvent>, eventFilter: Predicate<MouseEvent> = Predicate<MouseEvent> { true }): EventFX<MouseEvent> =
            EventFXWithConsumer(name, MouseEvent.MOUSE_PRESSED, eventHandler, eventFilter)

        @JvmStatic
        @JvmOverloads
        fun MOUSE_RELEASED(name: String, eventHandler: Consumer<MouseEvent>, eventFilter: Predicate<MouseEvent> = Predicate<MouseEvent> { true }): EventFX<MouseEvent> =
            EventFXWithConsumer(name, MouseEvent.MOUSE_RELEASED, eventHandler, eventFilter)

        @JvmStatic
        @JvmOverloads
        fun MOUSE_DRAGGED(name: String, eventHandler: Consumer<MouseEvent>, eventFilter: Predicate<MouseEvent> = Predicate<MouseEvent> { true }): EventFX<MouseEvent> =
            EventFXWithConsumer(name, MouseEvent.MOUSE_DRAGGED, eventHandler, eventFilter)

        @JvmStatic
        @JvmOverloads
        fun MOUSE_MOVED(name: String, eventHandler: Consumer<MouseEvent>, eventFilter: Predicate<MouseEvent> = Predicate<MouseEvent> { true }): EventFX<MouseEvent> =
            EventFXWithConsumer(name, MouseEvent.MOUSE_MOVED, eventHandler, eventFilter)

        @JvmStatic
        @JvmOverloads
        fun SCROLL(name: String, eventHandler: Consumer<ScrollEvent>, eventFilter: Predicate<ScrollEvent> = Predicate<ScrollEvent> { true }): EventFX<ScrollEvent> =
            EventFXWithConsumer(name, ScrollEvent.SCROLL, eventHandler, eventFilter)
    }

}
