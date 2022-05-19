package org.janelia.saalfeldlab.fx.actions

import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.*
import org.janelia.saalfeldlab.fx.event.KeyTracker
import org.janelia.saalfeldlab.fx.extensions.nonnull
import org.janelia.saalfeldlab.fx.extensions.nonnullVal
import java.util.function.Consumer

/**
 * [DragActionSet] handles drag actions, containing 3 provided [MouseAction]s
 *
 * 1. [MouseEvent.DRAG_DETECTED]
 * 2. [MouseEvent.MOUSE_DRAGGED]
 * 3. [MouseEvent.MOUSE_RELEASED]
 *
 * @constructor create the [DragActionSet], and initialize the contained [Action]s
 *
 * @param name of the [DragActionSet];
 * @param keyTracker to track the key state
 * @param filter to apply before triggering a drag event (Only [DRAG_DETECTED] and [MOUSE_DRAGGED] are drag events)
 * @param apply configuration callback for the created [DragActionSet]
 */
open class DragActionSet @JvmOverloads constructor(name: String, keyTracker: KeyTracker? = null, filter: Boolean = true, apply: (DragActionSet.() -> Unit)? = null) : ActionSet(name, keyTracker) {

    /**
     * [DRAG_DETECTED] [Action]. Can be access for further configuration
     */
    val dragDetectedAction = DRAG_DETECTED {
        this.name = "${this@DragActionSet.name} (drag detected)"
        this.filter = filter
    }

    /**
     * [MOUSE_DRAGGED] [Action]. Can be access for further configuration
     */
    val dragAction = MOUSE_DRAGGED {
        this.name = "${this@DragActionSet.name} (drag)"
        this.filter = filter
        verify { isDragging }
    }

    /**
     * [MOUSE_RELEASED] [Action]. Can be access for further configuration
     */
    val dragReleaseAction = MOUSE_RELEASED {
        this.name = "${this@DragActionSet.name} (drag released)"
        this.filter = filter
        verify { isDragging }
    }

    /**
     * X position when [DRAG_DETECTED]
     */
    var startX = 0.0

    /**
     * Y position when [DRAG_DETECTED]
     */
    var startY = 0.0

    /**
     * if true, [startX],[startY] will be updated when [MOUSE_DRAGGED]. false by default
     */
    var updateXY = false

    private val readOnlyIsDraggingWrapper = ReadOnlyBooleanWrapper()

    /**
     * Read Only Property to determine whether this action is currently handling a drag event.
     */
    val isDraggingProperty: ReadOnlyBooleanProperty = readOnlyIsDraggingWrapper.readOnlyProperty
    private var _isDragging: Boolean by readOnlyIsDraggingWrapper.nonnull()

    /**
     * Is this [DragActionSet] currently handling a drag event
     */
    val isDragging: Boolean by readOnlyIsDraggingWrapper.readOnlyProperty.nonnullVal()

    init {
        /* Initialize with empty lambda, so only drag state updates occur */
        onDragDetected { }
        onDrag { }
        onDragReleased { }
        apply?.let { it(this) }
    }


    /**
     * Provide a callback to configure the [dragDetectedAction] [MouseAction]
     *
     * @param onDragDetected the callback
     */
    @JvmSynthetic
    fun onDragDetected(onDragDetected: (MouseEvent) -> Unit) {
        dragDetectedAction.apply {
            onAction {
                initDragState(it)
                onDragDetected(it)
            }
        }
    }

    /**
     * Provide a callback to configure the [dragAction] [MouseAction]
     *
     * @param onDrag the callback
     */
    @JvmSynthetic
    fun onDrag(onDrag: (MouseEvent) -> Unit) {
        dragAction.apply {
            onAction {
                onDrag(it)
                updateDragState(it)
            }
        }
    }

    /**
     * Provide a callback to configure the [dragReleaseAction] [MouseAction]
     *
     * @param onDragReleased the callback
     */
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

    /**
     * Add a check to verify before triggering a drag action (only [dragDetectedAction] and [dragAction] )
     *
     * @param dragCheck check against the [MouseEvent] before triggering a drag action
     */
    fun verify(dragCheck: (MouseEvent) -> Boolean) {
        dragDetectedAction.verify { dragCheck(it) }
        dragAction.verify { dragCheck(it) }
    }
}
