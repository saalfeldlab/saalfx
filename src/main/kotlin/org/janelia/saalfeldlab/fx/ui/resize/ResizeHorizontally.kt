package org.janelia.saalfeldlab.fx.ui.resize

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.property.SimpleDoubleProperty
import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.Window
import org.janelia.saalfeldlab.fx.actions.ActionSet.Companion.installActionSet
import org.janelia.saalfeldlab.fx.actions.ActionSet.Companion.removeActionSet
import org.janelia.saalfeldlab.fx.actions.DragActionSet
import org.janelia.saalfeldlab.fx.extensions.createNonNullValueBinding
import org.janelia.saalfeldlab.fx.extensions.nonnull
import org.janelia.saalfeldlab.fx.extensions.nonnullVal
import org.janelia.saalfeldlab.fx.util.InvokeOnJavaFXApplicationThread
import java.util.function.DoublePredicate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ResizeHorizontally @JvmOverloads constructor(
	private val initialPosition: Double? = null,
	private val isWithinMarginOfBorder: DoublePredicate = DoublePredicate { abs(it) < 5 },
) {

	constructor(
		initialPosition: Double? = null,
		isWithinMarginOfBorder: (Double) -> Boolean = { abs(it) < 5 },
	) :
			this(
				initialPosition,
				DoublePredicate(isWithinMarginOfBorder)
			)

	val minProperty = SimpleDoubleProperty(Double.NEGATIVE_INFINITY).also { it.addListener { _ -> update() } }
	val min by minProperty.nonnull()

	val maxProperty = SimpleDoubleProperty(Double.POSITIVE_INFINITY).also { it.addListener { _ -> update() } }
	val max by maxProperty.nonnull()

	val currentPositionProperty: DoubleProperty = SimpleDoubleProperty().apply {
		addListener { _, _, new -> value = limitPosition(new.toDouble()) }
		value = initialPosition ?: 0.0
	}
	var currentPosition by currentPositionProperty.nonnull()

	private val _canResize = ReadOnlyBooleanWrapper(false)

	val canResizeProperty: ReadOnlyBooleanProperty = _canResize.readOnlyProperty

	var canResize: Boolean
		get() = _canResize.value
		private set(canResize) = _canResize.setValue(canResize)

	// handlers
	private val mouseMoved: EventHandler<MouseEvent> = EventHandler { ev ->
		canResize = isWithinMarginOfBorder.test(ev.x - currentPosition)
	}

	private val mouseDragged = DragActionSet("resize horizontally") {
		relative = false
		verify { canResize }
		onDrag {
			val dx = it.x - currentPosition
			currentPosition += dx
		}
	}

	val isDraggingProperty: ReadOnlyBooleanProperty = mouseDragged.isDraggingProperty
	val isDragging: Boolean by isDraggingProperty.nonnullVal()

	fun installInto(node: Node) {
		node.addEventFilter(MouseEvent.MOUSE_MOVED, mouseMoved)
		node.installActionSet(mouseDragged)
	}

	fun removeFrom(node: Node) {
		node.removeEventFilter(MouseEvent.MOUSE_MOVED, mouseMoved)
		node.removeActionSet(mouseDragged)
		_canResize.value = false
	}

	private fun update() {
		currentPosition = limitPosition(currentPosition)
	}

	private fun limitPosition(position: Double): Double {
		val min = this.min
		val max = this.max
		require(max >= min) { "max < min: $max < $min" }
		return min(max(position, min), max)
	}

	companion object {

		private val Event.scene: Scene?
			get() = this.target?.scene

		private val EventTarget.scene: Scene?
			get() = when (this) {
				is Scene -> this
				is Node -> this.scene
				is Window -> this.scene
				else -> null
			}

		@JvmStatic
		fun main(args: Array<String>) {
			Platform.startup {}
			InvokeOnJavaFXApplicationThread {
				val left = VBox(Label("Left")).apply {
					background = Background(BackgroundFill(Color.LIGHTCYAN, CornerRadii.EMPTY, Insets.EMPTY))
					minWidth = 0.0
				}
				val center = VBox(Label("Center")).apply {
					background = Background(BackgroundFill(Color.MAGENTA, CornerRadii.EMPTY, Insets.EMPTY))
					minWidth = 50.0
				}
				val right = VBox(Label("Right")).apply {
					background = Background(BackgroundFill(Color.LIGHTCYAN, CornerRadii.EMPTY, Insets.EMPTY))
					minWidth = 0.0
				}
				val root = BorderPane(center).also {
					it.left = left
					it.right = right
					it.minWidth = 35.0
				}
				val margin = 5.0
				val leftMinWidth = Bindings.createDoubleBinding({ max(left.minWidth, 0.0) }, left.minWidthProperty())
				val rightMinWidth = Bindings.createDoubleBinding({ max(right.minWidth, 0.0) }, right.minWidthProperty())
				val centerMinWidth = Bindings.createDoubleBinding({ max(center.minWidth, 0.0) }, center.minWidthProperty())
				val leftMaxWidth = Bindings.createDoubleBinding(
					{ max(root.width - max(max(right.width, rightMinWidth.value), 0.0) - centerMinWidth.value, leftMinWidth.value) },
					root.widthProperty(),
					leftMinWidth,
					right.widthProperty(),
					rightMinWidth,
					centerMinWidth
				)
				val rightMaxWidth = Bindings.createDoubleBinding({ max(root.width - rightMinWidth.value, 0.0) }, root.widthProperty())
				val rightMinOffset = Bindings
					.createDoubleBinding({ max(leftMinWidth.value, left.width) }, leftMinWidth, left.widthProperty())
					.add(centerMinWidth)
				val boundedRightMinOffset = Bindings.createDoubleBinding({ min(rightMaxWidth.value, rightMinOffset.value) }, rightMaxWidth, rightMinOffset)
				val resizeLeft = ResizeHorizontally(2.0 * max(left.minWidth, 20.0)) { abs(it) < margin }.apply {
					minProperty.bind(leftMinWidth)
					maxProperty.bind(leftMaxWidth)
				}
				val resizeRight = ResizeHorizontally(root.width - 2.0 * max(left.minWidth, 20.0)) { abs(it) < margin }.apply {
					minProperty.bind(boundedRightMinOffset)
					maxProperty.bind(rightMaxWidth)
				}
				resizeLeft.installInto(root)
				resizeRight.installInto(root)
				resizeLeft.currentPositionProperty.addListener { _, _, new -> left.prefWidth = new.toDouble() }
				resizeRight.currentPositionProperty.addListener { _, _, new -> right.prefWidth = root.width - new.toDouble() }
				left.widthProperty().addListener { _, _, new -> resizeLeft.currentPosition = new.toDouble() }
				right.widthProperty().addListener { _, _, new -> resizeRight.currentPosition = root.width - new.toDouble() }

				val canResize = resizeLeft.canResizeProperty.or(resizeRight.canResizeProperty)
				val isDragging = resizeLeft.isDraggingProperty.or(resizeRight.isDraggingProperty)
				val needsCursor = canResize.or(isDragging)
				val cursor = needsCursor.createNonNullValueBinding { if (it) Cursor.H_RESIZE else Cursor.DEFAULT }

				Stage().also {
					it.scene = Scene(root, 400.0, 300.0)
					cursor.addListener { _, _, new -> it.scene.cursor = new }
				}.show()
				InvokeOnJavaFXApplicationThread {
					resizeRight.currentPosition = root.width - left.width
					resizeLeft.currentPosition = left.width
				}
			}
		}
	}
}
