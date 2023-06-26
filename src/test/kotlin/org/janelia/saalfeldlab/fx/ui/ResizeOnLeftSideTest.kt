package org.janelia.saalfeldlab.fx.ui

import javafx.application.Platform
import javafx.event.Event
import javafx.geometry.Point2D
import javafx.scene.Scene
import javafx.scene.control.TextArea
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Region
import javafx.stage.Stage
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.slf4j.LoggerFactory
import org.testfx.framework.junit.ApplicationTest
import java.lang.invoke.MethodHandles
import java.util.function.DoublePredicate
import kotlin.math.abs

class ResizeOnLeftSideTest : ApplicationTest() {

	private lateinit var node: Region

	private lateinit var resizer: ResizeOnLeftSide

	override fun start(stage: Stage) {

		node = TextArea("Node!")
			.also { it.prefWidth = START_WIDTH }
			.also { it.addEventFilter(Event.ANY) { LOG.trace("Filtering event in node: {}", it) } }

		resizer = ResizeOnLeftSide(
			node,
			node.prefWidthProperty(),
			isWithinMarginOfBorder = IS_WITHIN_MARGIN_OF_BORDER
		)

		val center = TextArea("Center!")
			.also { it.addEventFilter(Event.ANY) { LOG.trace("Filtering event in center: {}", it) } }
		val pane = BorderPane(center)
			.also { it.right = node }
		resizer.install()
		stage.scene = Scene(pane, SCENE_WIDTH, SCENE_HEIGHT)
			.also { it.addEventFilter(Event.ANY) { LOG.trace("Filtering event in scene: {}", it) } }
			.also { it.addEventFilter(MouseEvent.ANY) { LOG.trace("Filtering mouse event in scene: {}", it) } }
		stage.show()
		// This is necessary to make sure that the stage grabs focus from OS and events are registered
		// https://stackoverflow.com/a/47685356/1725687
		Platform.runLater { stage.isIconified = true; stage.isIconified = false }
	}

	@Test
	fun testInitialState() {
		Assert.assertEquals(START_WIDTH, node.prefWidth, 0.0)
		Assert.assertEquals(node.boundsInParent.minX, SCENE_WIDTH - START_WIDTH, 0.0)
	}

	@Test
	@Ignore("This test moves the mouse pointer which is problematic on recent MacOS as it requires to allow this in the system's security settings.")
	fun testIsWithinMargin() {
		val offsetsAndResizable = arrayOf(
			Pair(-RESIZABLE_DISTANE - 1, false),
			Pair(-RESIZABLE_DISTANE, false), // IS_WITHIN_MARGIN_OF_BORDER requires strictly smaller
			Pair(-RESIZABLE_DISTANE / 2.0, true),
			Pair(0.0, true),
			Pair(RESIZABLE_DISTANE / 2.0, true),
			Pair(RESIZABLE_DISTANE, false), // IS_WITHIN_MARGIN_OF_BORDER requires strictly smaller
			Pair(RESIZABLE_DISTANE + 1, false)
		)
		offsetsAndResizable.forEach { (offset, resizable) ->
			LOG.debug("Moving mouse to offset=$offset relative to left border of right node")
			moveTo(node, Point2D(-node.width / 2.0 + offset, 0.0))
			LOG.debug("{} {} {} {}", offset, resizable, resizer.isCurrentlyWithinMarginOfBorder)
			Assert.assertEquals(resizable, resizer.isCurrentlyWithinMarginOfBorder)
		}
	}

	companion object {
		private val LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

		const val START_WIDTH = 100.0

		const val SCENE_WIDTH = 800.0

		const val SCENE_HEIGHT = 600.0

		const val RESIZABLE_DISTANE = 5.0

		val IS_WITHIN_MARGIN_OF_BORDER = DoublePredicate { abs(it) < RESIZABLE_DISTANE }

	}

}
