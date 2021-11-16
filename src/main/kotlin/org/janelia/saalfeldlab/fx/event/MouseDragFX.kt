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

import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import org.janelia.saalfeldlab.fx.extensions.nonnullVal
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Predicate

abstract class MouseDragFX(
        val name: String,
        private val eventFilter: Predicate<MouseEvent>,
        protected val consume: Boolean,
        protected val transformLock: Any,
        protected val updateXY: Boolean,
) : InstallAndRemove<Node> {

    protected var startX = 0.0

    protected var startY = 0.0

    private val _isDragging = ReadOnlyBooleanWrapper()

    private val detect = DragDetect()

    private val drag = Drag()

    private val release = DragRelease()

    val isDraggingProperty: ReadOnlyBooleanProperty = _isDragging.readOnlyProperty

    val isDragging: Boolean by isDraggingProperty.nonnullVal()

    constructor(
            name: String,
            eventFilter: Predicate<MouseEvent>,
            transformLock: Any,
            updateXY: Boolean,
    ) : this(name, eventFilter, false, transformLock, updateXY) {
    }

    abstract fun initDrag(event: MouseEvent)

    abstract fun drag(event: MouseEvent)

    open fun endDrag(event: MouseEvent) {}

    @Deprecated("Use getter syntax instead", ReplaceWith("getName()"))
    fun name() = name

    override fun installInto(t: Node) {
        t.addEventHandler(MouseEvent.DRAG_DETECTED, detect)
        t.addEventHandler(MouseEvent.MOUSE_DRAGGED, drag)
        t.addEventHandler(MouseEvent.MOUSE_RELEASED, release)
    }

    override fun removeFrom(t: Node) {
        t.removeEventHandler(MouseEvent.DRAG_DETECTED, detect)
        t.removeEventHandler(MouseEvent.MOUSE_DRAGGED, drag)
        t.removeEventHandler(MouseEvent.MOUSE_RELEASED, release)
    }

    fun installIntoAsFilter(t: Node) {
        t.addEventFilter(MouseEvent.DRAG_DETECTED, detect)
        t.addEventFilter(MouseEvent.MOUSE_DRAGGED, drag)
        t.addEventFilter(MouseEvent.MOUSE_RELEASED, release)
    }

    fun removeFromAsFilter(t: Node) {
        t.removeEventFilter(MouseEvent.DRAG_DETECTED, detect)
        t.removeEventFilter(MouseEvent.MOUSE_DRAGGED, drag)
        t.removeEventFilter(MouseEvent.MOUSE_RELEASED, release)
    }

    private inner class DragDetect : EventHandler<MouseEvent> {

        override fun handle(event: MouseEvent) {
            if (eventFilter.test(event)) {
                startX = event.x
                startY = event.y
                _isDragging.set(true)
                initDrag(event)
                if (consume) {
                    LOG.debug("Consuming Drag Detect event")
                    event.consume()
                }
            }
        }
    }

    private inner class Drag : EventHandler<MouseEvent> {

        override fun handle(event: MouseEvent) {
            if (_isDragging.get()) {
                drag(event)
                if (consume) {
                    LOG.debug("Consuming Drag event")
                    event.consume()
                }
                if (updateXY) {
                    startX = event.x
                    startY = event.y
                }
            }

        }
    }

    private inner class DragRelease : EventHandler<MouseEvent> {

        override fun handle(event: MouseEvent) {
            val wasDragging = _isDragging.get()
            _isDragging.set(false)
            if (wasDragging) {
                endDrag(event)
                if (consume) {
                    LOG.debug("Consuming DragRelease event")
                    event.consume()
                }
            }
        }

    }

    fun abortDrag() = _isDragging.set(false)

    companion object {

        private val LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        @JvmStatic
        fun createDrag(
                name: String,
                eventFilter: Predicate<MouseEvent>,
                consume: Boolean,
                transformLock: Any,
                initDrag: Consumer<MouseEvent>,
                drag: BiConsumer<Double, Double>,
                updateXY: Boolean,
        ): MouseDragFX {
            return object : MouseDragFX(name, eventFilter, consume, transformLock, updateXY) {

                override fun initDrag(event: MouseEvent) {
                    initDrag.accept(event)
                }

                override fun drag(event: MouseEvent) {
                    drag.accept(event.x - startX, event.y - startY)
                }
            }
        }
    }

}
