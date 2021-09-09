package org.janelia.saalfeldlab.fx.ui.resize

import com.sun.javafx.application.PlatformImpl
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
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.BorderPane
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.Window
import org.janelia.saalfeldlab.fx.event.MouseDragFX
import org.janelia.saalfeldlab.fx.extensions.getValue
import org.janelia.saalfeldlab.fx.extensions.setValue
import org.janelia.saalfeldlab.fx.util.InvokeOnJavaFXApplicationThread
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.function.DoublePredicate
import java.util.function.Predicate
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
                    DoublePredicate(isWithinMarginOfBorder))

    val minProperty = SimpleDoubleProperty(Double.NEGATIVE_INFINITY).also { it.addListener { _ -> update() } }
    val min by minProperty

    val maxProperty = SimpleDoubleProperty(Double.POSITIVE_INFINITY).also { it.addListener { _ -> update() } }
    val max by maxProperty

    val currentPositionProperty: DoubleProperty = SimpleDoubleProperty()
            .also { cp -> cp.addListener { _, _, new -> cp.value = limitPosition(new.toDouble()) } }
            .also { it.value = initialPosition ?: 0.0 }
    var currentPosition by currentPositionProperty

    private val _canResize = ReadOnlyBooleanWrapper(false)

    val canResizeProperty: ReadOnlyBooleanProperty = _canResize.readOnlyProperty

    var canResize: Boolean
        get() = _canResize.value
        private set(canResize) = _canResize.setValue(canResize)

    // handlers
    private val mouseMoved: EventHandler<MouseEvent> = EventHandler { ev ->
        canResize = isWithinMarginOfBorder.test(ev.x - currentPosition)
    }

    private val mouseDragged: MouseDragFX = object : MouseDragFX(
            "resize horizontally",
            Predicate { canResize },
            true,
            this,
            false) {
        override fun initDrag(event: MouseEvent) {
//            event.scene?.cursor = Cursor.W_RESIZE
        }

        override fun drag(event: MouseEvent) {
            val dx = event.x - currentPosition
            currentPosition += dx
        }

    }

    val draggingProperty = mouseDragged.isDraggingProperty

    val isDragging: Boolean
        get() = mouseDragged.isDragging

    fun installInto(node: Node) {
        node.addEventFilter(MouseEvent.MOUSE_MOVED, mouseMoved)
        mouseDragged.installInto(node)
    }

    fun removeFrom(node: Node) {
        node.removeEventFilter(MouseEvent.MOUSE_MOVED, mouseMoved)
        mouseDragged.removeFrom(node)
        _canResize.value = false
        this.mouseDragged.abortDrag()
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

        private val LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

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
            PlatformImpl.startup {}
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
                        centerMinWidth)
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
                val isDragging = resizeLeft.draggingProperty.or(resizeRight.draggingProperty)
                val needsCursor = canResize.or(isDragging)
                val cursor = Bindings.createObjectBinding({ if (needsCursor.get()) Cursor.H_RESIZE else Cursor.DEFAULT }, needsCursor)

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
