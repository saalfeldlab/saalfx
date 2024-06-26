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
 * @param keyTracker to provide the key tracker to track the key state
 * @param filter to set drag actions as filters
 * @param consumeMouseClicked event generated when releasing a drag.
 *  If a drag begins and ends on the same node, it triggers a MOUSE_CLICKED event, regardless of how far the mouse
 *  travelled in between MOUSE_PRESSED and MOUSE_RELEASED. If [consumeMouseClicked] is set to true, then a
 *  MOUSE_CLICKED action listener with consume the resulting MOUSE_CLICKED as a filter prior.
 * @param apply configuration callback for the created [DragActionSet]
 * 	var consumeMouseClicked = consumeMouseClicked
 */
open class DragActionSet @JvmOverloads constructor(
	name: String,
	keyTracker: () -> KeyTracker? = { null },
	filter: Boolean = true,
	consumeMouseClicked: Boolean = false,
	apply: (DragActionSet.() -> Unit)? = null) : ActionSet(name, keyTracker) {

	/**
	 * [DRAG_DETECTED] [Action]. Can be access for further configuration
	 */
	val dragDetectedAction = DRAG_DETECTED {
		this.name = "${this@DragActionSet.name}.drag detected"
		this.filter = filter
		verifyEventNotNull()
	}

	/**
	 * [MOUSE_DRAGGED] [Action]. Can be access for further configuration
	 */
	val dragAction = MOUSE_DRAGGED {
		this.name = "${this@DragActionSet.name}.drag"
		this.filter = filter
		verifyEventNotNull()
		verify { isDragging }
	}

	/**
	 * [MOUSE_RELEASED] [Action]. Can be access for further configuration
	 */
	val dragReleaseAction = MOUSE_RELEASED {
		this.name = "${this@DragActionSet.name}.drag released"
		this.filter = filter
		verifyEventNotNull()
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
	 * if true, [startX],[startY] will be updated when [MOUSE_DRAGGED].
	 * if false, [startX],[startY] are always the position of the MOUSE_PRESSED event that triggered the drag event.
	 */
	var relative = false

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

	private var nextClickFromDragRelease = false

	init {
		MOUSE_PRESSED {
			this.name = "${this@DragActionSet.name}.start position for drag threshold"
			consume = false
			verifyEventNotNull()
			verify("start position updated only until the drag is detected") {!isDragging }
			onAction {
				if (!relative) {
					startX = it!!.x
					startY = it.y
				}
			}
		}
		/* Initialize with empty lambda, so only drag state updates occur */
		onDragDetected { }
		onDrag { }
		onDragReleased { }
		if (consumeMouseClicked) {
			MOUSE_CLICKED {
				this.name = "${this@DragActionSet.name}.DragActionSet Mouse Release Consumer"
				this.filter = true
				onAction {
					consume = nextClickFromDragRelease
					nextClickFromDragRelease = false
				}
			}
		}
		apply?.let { it() }
	}


	/**
	 * Provide a callback to configure the [dragDetectedAction] [MouseAction]
	 *
	 * @param onDragDetected the callback
	 */
	@JvmSynthetic
	fun onDragDetected(onDragDetected: (MouseEvent) -> Unit) {
		dragDetectedAction.apply {
			verifyEventNotNull()
			onAction {
				initDragState(it!!)
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
			verifyEventNotNull()
			onAction {
				onDrag(it!!)
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
			verifyEventNotNull()
			onAction {
				endDragState()
				onDragReleased(it!!)
				nextClickFromDragRelease = true
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
		_isDragging = true
		updateDragState(it)
	}

	private fun updateDragState(it: MouseEvent) {
		if (relative) {
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
	 * NOTE: Because drag event inherently depend on the MoueEvent, we require the non-nullable type here, and cast it.
	 *
	 * @param dragCheck check against the [MouseEvent] before triggering a drag action
	 */
	@JvmOverloads
	fun verify(description: String? = null, dragCheck: (MouseEvent) -> Boolean) {
		dragDetectedAction.verify(description) { dragCheck(it!!) }
		dragAction.verify(description) { dragCheck(it!!) }
	}
}
