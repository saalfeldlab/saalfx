package org.janelia.saalfeldlab.fx

import javafx.application.Platform
import javafx.event.Event
import javafx.scene.Scene
import javafx.scene.control.ListView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.stage.Stage
import org.janelia.saalfeldlab.fx.util.InvokeOnJavaFXApplicationThread
import org.junit.Assert
import org.slf4j.LoggerFactory
import org.testfx.framework.junit.ApplicationTest
import org.testfx.util.WaitForAsyncUtils
import java.io.File
import java.io.PrintStream
import java.lang.invoke.MethodHandles
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TasksTest : ApplicationTest() {

	private lateinit var list: ListView<String>

	override fun start(stage: Stage) {
		list = ListView()
		val pane = Pane(list)
		stage.scene = Scene(pane, SCENE_WIDTH, SCENE_HEIGHT).apply {
			addEventFilter(Event.ANY) { LOG.trace("Filtering event in scene: {}", it) }
			addEventFilter(MouseEvent.ANY) { LOG.trace("Filtering mouse event in scene: {}", it) }
		}
		stage.show()
		// This is necessary to make sure that the stage grabs focus from OS and events are registered
		// https://stackoverflow.com/a/47685356/1725687
		Platform.runLater { stage.isIconified = true; stage.isIconified = false }
	}

	@BeforeTest
	fun clearList() {
		InvokeOnJavaFXApplicationThread.invokeAndWait {
			list.items.clear()
		}
	}

	@Test
	fun `onSuccess runs when successful`() {
		val testText = "Single onSuccess Test"
		Tasks.createTask { testText }
			.onSuccess { _, t -> list.items.add(t.value) }
			.submit()

		WaitForAsyncUtils.waitForFxEvents()

		val items = list.items
		Assert.assertArrayEquals(arrayOf(testText), items.toTypedArray())
	}


	@Test
	fun `onEnd and OnSuccess run when successful`() {
		val endOnSuccessText = "Single onEnd Test, expecting success"
		val task = Tasks.createTask { endOnSuccessText }
			.onSuccess { _, t -> list.items.add(t.value) }
			.onEnd { t -> list.items.add(t.value) }
			.submit()

		WaitForAsyncUtils.waitForFxEvents()

		val items = list.items
		Assert.assertTrue(task.isDone)
		Assert.assertEquals(endOnSuccessText, task.get())
		Assert.assertArrayEquals(arrayOf(endOnSuccessText, endOnSuccessText), items.toTypedArray())
	}

	@Test
	fun `onEnd and onSuccess run after blocking when successful`() {
		val endOnSuccessText = "Single onEnd Test, expecting success"
		val result = Tasks.createTask { endOnSuccessText }
			.onSuccess { _, t -> list.items.add(t.value) }
			.onEnd { t -> list.items.add(t.value) }
			.submitAndWait()

		WaitForAsyncUtils.waitForFxEvents()

		val items = list.items
		Assert.assertEquals(endOnSuccessText, result)
		Assert.assertArrayEquals(arrayOf(endOnSuccessText, endOnSuccessText), items.toTypedArray())
	}

	@Test
	fun `onEnd runs when cancelled`() {
		val items = list.items
		val textWithoutCancel = "Single onEnd Test, expecting to never see this"
		val textWithCancel = "Single onEnd Test, expecting cancel"
		var canceled = false
		val maxTime = LocalDateTime.now().plus(5, ChronoUnit.SECONDS)
		val task = Tasks.createTask {
			/* waiting for the task to be canceled. If too long, we have failed. */
			while (!canceled || LocalDateTime.now().isBefore(maxTime)) {
				sleep(20)
			}
			textWithoutCancel
		}
			.onSuccess { _, t -> list.items.add(t.get()) }
			.onEnd { list.items.add(textWithCancel) }
			.submit()

		task.cancel()
		canceled = true

		WaitForAsyncUtils.waitForFxEvents()

		Assert.assertTrue(task.isCancelled)
		Assert.assertThrows(CancellationException::class.java) { task.get() }
		Assert.assertArrayEquals(arrayOf(textWithCancel), items.toTypedArray())
	}

	private class ExceptionTestException : RuntimeException("Intentional Exception Test!")

	@Test
	fun `onEnd and default onFailed run when failed`() {

		val items = list.items
		val textWithFailure = "Single onEnd Test, expecting failure"


		val stdout = System.out
		val stderr = System.err
		val devnull = object : PrintStream(nullOutputStream()) {
			override fun write(b: Int) = Unit
		}
		System.setOut(devnull)
		System.setErr(devnull)


		/* Intentionally trigger failed, ensure `onEnd` is still triggered */
		val task: UtilityTask<*>
		try {
			task = Tasks.createTask { throw ExceptionTestException() }
				.onSuccess { _, t -> list.items.add(t.get()) }
				.onEnd { list.items.add(textWithFailure) }
				.submit()
			WaitForAsyncUtils.waitForFxEvents()
		} finally {
			System.setOut(stdout)
			System.setErr(stderr)
		}



		Assert.assertFalse(task.isCancelled)
		Assert.assertTrue(task.isDone)
		Assert.assertArrayEquals(arrayOf(textWithFailure), items.toTypedArray())

		InvokeOnJavaFXApplicationThread.invokeAndWait {
			assertIs<ExceptionTestException>(task.exception)
		}
	}

	@Test
	fun `onEnd and custom onFailed run when failed`() {
		val items = list.items
		val textWithEnd = "Single onFailure Test, expecting end"
		val textWithFailure = "Single onFailure Test, expecting failure"
		/* Intentionally trigger failure, with custom onFailed */
		val task = Tasks.createTask { throw ExceptionTestException() }
			.onSuccess { _, t -> list.items.add(t.get()) }
			.onEnd { list.items.add(textWithEnd) }
			.onFailed { _, _ -> list.items.add(textWithFailure) }
			.submit()

		WaitForAsyncUtils.waitForFxEvents()

		Assert.assertFalse(task.isCancelled)
		Assert.assertTrue(task.isDone)
		Assert.assertArrayEquals(arrayOf(textWithEnd, textWithFailure), items.toTypedArray())

		InvokeOnJavaFXApplicationThread.invokeAndWait {
			assertIs<ExceptionTestException>(task.exception)
		}
	}

	@Test
	fun `appending callbacks run in order when successful`() {
		var success = 0
		var end = 0
		Tasks.createTask { "asdf" }
			.onSuccess { _, _ -> success += 1 }
			.onSuccess(true) { _, _ -> success *= 3 }
			.onEnd { end += 1 }
			.onEnd(true) { end *= 3 }
			.submitAndWait()

		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(3, success)
		assertEquals(3, end)

		var cancelled = 0
		Tasks.createTask {
			"asdf"
			Thread.sleep(100)
		}
			.onSuccess { _, _ -> success += 1 }
			.onSuccess(true) { _, _ -> success *= 3 }
			.onCancelled { _, _ -> cancelled += 1 }
			.onCancelled(true) { _, _ -> cancelled *= 3 }
			.onEnd { end += 1 }
			.onEnd(true) { end *= 3 }
			.submit().also { it.cancel() }

		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(3, success)
		assertEquals(3, cancelled)
		assertEquals(12, end)

		var failed = 0
		Tasks.createTask {
			"asdf"
			throw ExceptionTestException()
		}
			.onSuccess { _, _ -> success += 1 }
			.onSuccess(true) { _, _ -> success *= 3 }
			.onCancelled { _, _ -> cancelled += 1 }
			.onCancelled(true) { _, _ -> cancelled *= 3 }
			.onEnd { end += 1 }
			.onEnd(true) { end *= 3 }
			.onFailed { _, _ -> failed += 1}
			.onFailed(true) { _, _ -> failed *= 3}
			.submit()

		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(3, success)
		assertEquals(3, cancelled)
		assertEquals(3, failed)
		assertEquals(39, end)
	}

	@Test
	fun `append callbacks works as expected`() {
		Assert.assertThrows(RuntimeException::class.java) {
			Tasks.createTask { "asdf" }
				.onSuccess { _, _ -> }
				.onSuccess { _, _ -> }
		}
		Assert.assertThrows(RuntimeException::class.java) {
			Tasks.createTask { "asdf" }
				.onEnd { }
				.onEnd { }
		}
		Assert.assertThrows(RuntimeException::class.java) {
			Tasks.createTask { "asdf" }
				.onFailed{ _, _ -> }
				.onFailed{ _, _ -> }
		}
		Assert.assertThrows(RuntimeException::class.java) {
			Tasks.createTask { "asdf" }
				.onCancelled { _, _ -> }
				.onCancelled { _, _ -> }
		}
	}

	companion object {
		private val LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

		const val SCENE_WIDTH = 800.0

		const val SCENE_HEIGHT = 600.0

	}

}
