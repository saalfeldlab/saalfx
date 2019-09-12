package org.janelia.saalfeldlab.fx.event

import javafx.event.EventHandler
import javafx.scene.input.MouseEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.invoke.MethodHandles

class MouseTracker : EventHandler<MouseEvent> {

    var isDragging: Boolean = false
        private set

    var x: Double = 0.0
        private set

    var y: Double = 0.0
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
