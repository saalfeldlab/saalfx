package org.janelia.saalfeldlab.fx.actions

import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.*
import org.janelia.saalfeldlab.fx.event.KeyTracker
import org.janelia.saalfeldlab.fx.extensions.nonnull
import org.janelia.saalfeldlab.fx.extensions.nonnullVal
import java.util.function.Consumer

open class DragActionSet @JvmOverloads constructor(name: String, keyTracker: KeyTracker? = null, filter: Boolean = true, apply: (DragActionSet.() -> Unit)? = null) : ActionSet(name, keyTracker) {

    val dragDetectedAction = DRAG_DETECTED {
        this.name = "${this@DragActionSet.name} (drag detected)"
        this.filter = filter
    }
    val dragAction = MOUSE_DRAGGED {
        this.name = "${this@DragActionSet.name} (drag)"
        this.filter = filter
        verify { isDragging }
    }
    val dragReleaseAction = MOUSE_RELEASED {
        this.name = "${this@DragActionSet.name} (drag released)"
        this.filter = filter
        verify { isDragging }
    }

    var startX = 0.0
    var startY = 0.0
    var updateXY = false

    private val readOnlyIsDraggingWrapper = ReadOnlyBooleanWrapper()
    val isDraggingProperty = readOnlyIsDraggingWrapper.readOnlyProperty
    private var _isDragging: Boolean by readOnlyIsDraggingWrapper.nonnull()
    val isDragging: Boolean by readOnlyIsDraggingWrapper.readOnlyProperty.nonnullVal()

    init {
        /* Initialize with empty lambda, so only drag state updates occur */
        onDragDetected { }
        onDrag { }
        onDragReleased { }
        apply?.let { it(this) }
    }


    fun onDragDetected(onDragDetected: Consumer<MouseEvent>) {
        onDragDetected { onDragDetected.accept(it) }
    }

    fun onDrag(onDrag: Consumer<MouseEvent>) {
        onDrag { onDrag.accept(it) }
    }

    fun onDragRelease(onDragRelease: Consumer<MouseEvent>) {
        onDragRelease { onDragRelease.accept(it) }
    }

    @JvmSynthetic
    fun onDragDetected(onDragDetected: (MouseEvent) -> Unit) {
        dragDetectedAction.apply {
            onAction {
                initDragState(it)
                onDragDetected(it)
            }
        }
    }

    @JvmSynthetic
    fun onDrag(onDrag: (MouseEvent) -> Unit) {
        dragAction.apply {
            onAction {
                onDrag(it)
                updateDragState(it)
            }
        }
    }

    @JvmSynthetic
    fun onDragReleased(onDragReleased: (MouseEvent) -> Unit) {
        dragReleaseAction.apply {
            onAction {
                endDragState()
                onDragReleased(it)
            }
        }
    }

    fun onDragDetected(onDragDetected: Consumer<MouseEvent>) {
        onDragDetected { onDragDetected.accept(it) }
    }

    fun onDrag(onDrag: Consumer<MouseEvent>) {
        onDrag { onDrag.accept(it) }
    }

    fun onDragReleased(onDragRelease: Consumer<MouseEvent>) {
        onDragReleased { onDragRelease.accept(it) }
    }

    private fun initDragState(it: MouseEvent) {
        startX = it.x
        startY = it.y
        _isDragging = true
    }

    private fun updateDragState(it: MouseEvent) {
        if (updateXY) {
            startX = it.x
            startY = it.y
        }
    }

    private fun endDragState() {
        _isDragging = false
    }

    fun verify(dragCheck: (MouseEvent) -> Boolean) {
        dragDetectedAction.verify(MouseEvent.DRAG_DETECTED, dragCheck)
        dragAction.verify(MouseEvent.MOUSE_DRAGGED, dragCheck)
    }
}
