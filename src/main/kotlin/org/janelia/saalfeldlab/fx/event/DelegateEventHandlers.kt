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
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.Optional
import java.util.function.Supplier

class DelegateEventHandlers {

    class SupplierDelegateEventHandler<E : Event>(private val currentEventHandler: Supplier<EventHandler<E>>) : EventHandler<E> {

        constructor(currentEventHandler: () -> EventHandler<E>) : this(Supplier { currentEventHandler() })

        override fun handle(event: E) {
            val handler = currentEventHandler.get()
            LOG.trace("Handling event {} with handler {}", event, handler)
            Optional.ofNullable(handler).ifPresent { h -> h.handle(event) }
        }

        companion object {
            private val LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        }
    }

    class ListDelegateEventHandler<E : Event> : EventHandler<E> {

        private val delegateHandlers = ArrayList<EventHandler<E>>()

        override fun handle(event: E) {
            for (handler in delegateHandlers) {
                LOG.trace("Handler {} handling event {}", handler, event)
                if (event.isConsumed)
                    break
                handler.handle(event)
            }
        }

        fun addHandler(handler: EventHandler<E>)= this.delegateHandlers.add(handler)

        fun removeHandler(handler: EventHandler<E>) = this.delegateHandlers.remove(handler)
    }

    class AnyHandler : EventHandler<Event> {

        // TODO fix generic bounds, very confusing
        private val handlerTypeMapping = HashMap<EventHandler<out Event>, EventType<out Event>>()

        override fun handle(event: Event) {

            val handlerTypeMappingCopy = handlerTypeMapping.toMap()
            for ((key, handlerEventType) in handlerTypeMappingCopy) {
                if (event.isConsumed)
                    break

                var eventType: EventType<*>? = event.eventType
                while (eventType != null) {
                    if (eventType == handlerEventType) {
                        LOG.trace("Handler for type {} handles type {}", handlerEventType, event.eventType)

                        (key as? EventHandler<in Event>)?.handle(event)
                        break
                    }
                    eventType = eventType.superType
                }
            }
        }

        fun addEventHandler(eventType: EventType<out Event>, eventHandler: EventHandler<out Event>) =
            handlerTypeMapping.put(eventHandler, eventType)

        fun removeEventHandler(handler: (Event) -> Unit) = removeEventHandler(EventHandler { handler(it) })
        fun removeEventHandler(eventHandler: EventHandler<Event>) = handlerTypeMapping.remove(eventHandler)

        fun addOnMousePressed(handler: (MouseEvent) -> Unit) = addOnMousePressed(EventHandler { handler(it) })
        fun addOnMousePressed(handler: EventHandler<MouseEvent>) = addEventHandler(MouseEvent.MOUSE_PRESSED, handler)

        fun addOnKeyPressed(handler: (KeyEvent) -> Unit) = addOnKeyPressed(EventHandler { handler(it) })
        fun addOnKeyPressed(handler: EventHandler<KeyEvent>) = addEventHandler(KeyEvent.KEY_PRESSED, handler)

        fun addOnKeyReleased(handler: (KeyEvent) -> Unit) = addOnKeyReleased(EventHandler { handler(it) })
        fun addOnKeyReleased(handler: EventHandler<KeyEvent>) = addEventHandler(KeyEvent.KEY_RELEASED, handler)

        fun addOnScroll(handler: (ScrollEvent) -> Unit) = addOnScroll(EventHandler { handler(it) })
        fun addOnScroll(handler: EventHandler<ScrollEvent>) = addEventHandler(ScrollEvent.SCROLL, handler)

        companion object {

            private val LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        }
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        fun <E : Event> fromSupplier(handler: () -> EventHandler<E>): EventHandler<E> = SupplierDelegateEventHandler(handler)

        @JvmStatic
        fun <E : Event> fromSupplier(handler: Supplier<EventHandler<E>>): EventHandler<E> = SupplierDelegateEventHandler(handler)

        @JvmStatic
        fun <E : Event> listHandler(): ListDelegateEventHandler<E> = ListDelegateEventHandler()

        @JvmStatic
        fun handleAny(): AnyHandler = AnyHandler()

    }

}
