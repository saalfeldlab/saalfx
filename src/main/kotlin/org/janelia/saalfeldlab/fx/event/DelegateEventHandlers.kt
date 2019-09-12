package org.janelia.saalfeldlab.fx.event

import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.EventType
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.invoke.MethodHandles
import java.util.ArrayList
import java.util.HashMap
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
            for ((key, value) in handlerTypeMappingCopy) {
                if (event.isConsumed)
                    break
                val handlerEventType = value ?: continue

                var eventType: EventType<*>? = event.eventType
                while (eventType != null) {
                    if (eventType == handlerEventType) {
                        LOG.trace("Handler for type {} handles type {}", handlerEventType, event.eventType)

                        (key as EventHandler<in Event>).handle(event)
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
