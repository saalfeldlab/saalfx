package org.janelia.saalfeldlab.fx

import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.application.Platform
import javafx.event.Event
import javafx.scene.Scene
import javafx.scene.control.ListView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.stage.Stage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.janelia.saalfeldlab.fx.util.InvokeOnJavaFXApplicationThread
import org.junit.Assert
import org.testfx.framework.junit.ApplicationTest
import org.testfx.util.WaitForAsyncUtils
import java.io.PrintStream
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
			addEventFilter(Event.ANY) { LOG.trace { "Filtering event in scene: $it" } }
			addEventFilter(MouseEvent.ANY) { LOG.trace { "Filtering mouse event in scene: $it" } }
		}
		stage.show()
		// This is necessary to make sure that the stage grabs focus from OS and events are registered
		// https://stackoverflow.com/a/47685356/1725687
		Platform.runLater { stage.isIconified = true; stage.isIconified = false }
	}

	@BeforeTest
	fun clearList() = runBlocking {
		InvokeOnJavaFXApplicationThread {
			list.items.clear()
		}.join()
	}

	@Test
	fun `onSuccess runs when successful`() {
		val testText = "Single onSuccess Test"
		Tasks.invoke { testText }
			.onSuccess { list.items.add(it) }

		WaitForAsyncUtils.waitForFxEvents()

		val items = list.items
		Assert.assertArrayEquals(arrayOf(testText), items.toTypedArray())
	}


	@Test
	fun `onEnd and OnSuccess run when successful`() {
		val endOnSuccessText = "Single onEnd Test, expecting success"
		val task = Tasks.invoke { endOnSuccessText }
			.onSuccess { list.items.add(it) }
			.onEnd { result, _ -> list.items.add(result!!) }

		val result = runBlocking {
			task.await()
		}

		val items = list.items
		Assert.assertTrue(task.isCompleted)
		Assert.assertEquals(endOnSuccessText, result)
		Assert.assertArrayEquals(arrayOf(endOnSuccessText, endOnSuccessText), items.toTypedArray())
	}

	@Test
	fun `onEnd and onSuccess run after blocking when successful`() {
		val endOnSuccessText = "Single onEnd Test, expecting success"
		val result = Tasks.invoke { endOnSuccessText }
			.onSuccess { list.items.add(it) }
			.onEnd { result, _ -> list.items.add(result!!) }
			.get()

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
		val task = Tasks.invoke {
			/* waiting for the task to be canceled. If too long, we have failed. */
			while (!canceled) {
				delay(5000)
			}
			textWithoutCancel
		}
			.onSuccess { list.items.add(it) }
			.onEnd { _, _ -> list.items.add(textWithCancel) }

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
			task = Tasks.invoke { throw ExceptionTestException() }
				.onSuccess {
					@Suppress("KotlinUnreachableCode")
					list.items.add(it)
				}
				.onEnd { _, _ -> list.items.add(textWithFailure) }
				.onFailed { assertIs<ExceptionTestException>(it) }
				.wait()

		} finally {
			System.setOut(stdout)
			System.setErr(stderr)
		}

		WaitForAsyncUtils.waitForFxEvents()

		Assert.assertTrue(task.isCancelled)
		Assert.assertTrue(task.isCompleted)
		Assert.assertArrayEquals(arrayOf(textWithFailure), items.toTypedArray())
	}

	@Test
	fun `onEnd and custom onFailed run when failed`() {
		val items = list.items
		val textWithEnd = "Single onFailure Test, expecting end"
		val textWithFailure = "Single onFailure Test, expecting failure"
		/* Intentionally trigger failure, with custom onFailed */


		val task = Tasks.invoke { throw ExceptionTestException() }
			.onSuccess {
				@Suppress("KotlinUnreachableCode")
				list.items.add(it)
			}
			.onEnd { _, _ -> list.items.add(textWithEnd) }
			.onFailed { list.items.add(textWithFailure) }
			.onFailed { assertIs<ExceptionTestException>(it) }
			.wait()

		WaitForAsyncUtils.waitForFxEvents()

		Assert.assertTrue(task.isCancelled)
		Assert.assertTrue(task.isCompleted)
		Assert.assertArrayEquals(arrayOf(textWithEnd, textWithFailure), items.toTypedArray())
	}

	@Test
	fun `multiple callbacks run in order when successful`() {
		var success = 0
		var end = 0
		Tasks.invoke { "asdf" }
			.onSuccess { success += 1 }
			.onSuccess { success *= 3 }
			.onEnd { _, _ -> end += 1 }
			.onEnd { _, _ -> end *= 3 }
			.wait()

		WaitForAsyncUtils.waitForFxEvents()

		assertEquals(3, success)
		assertEquals(3, end)

		var cancelled = 0
		Tasks.invoke {
			"asdf"
			delay(1000)
		}
			.onSuccess { success += 1 }
			.onSuccess { success *= 3 }
			.onCancelled { cancelled += 1 }
			.onCancelled { cancelled *= 3 }
			.onEnd { _, _ -> end += 1 }
			.onEnd { _, _ -> end *= 3 }
			.also {
				it.cancel()
				it.wait()
			}

		WaitForAsyncUtils.waitForFxEvents()

		assertEquals(3, success)
		assertEquals(3, cancelled)
		assertEquals(12, end)

		var failed = 0
		Tasks.invoke {
			"asdf"
			throw ExceptionTestException()
		}
			.onSuccess { success += 1 }
			.onSuccess { success *= 3 }
			.onCancelled { cancelled += 1 }
			.onCancelled { cancelled *= 3 }
			.onEnd { _, _ -> end += 1 }
			.onEnd { _, _ -> end *= 3 }
			.onFailed { failed += 1 }
			.onFailed { failed *= 3 }
			.wait()

		WaitForAsyncUtils.waitForFxEvents()

		assertEquals(3, success)
		assertEquals(3, cancelled)
		assertEquals(3, failed)
		assertEquals(39, end)
	}

	companion object {
		private val LOG = KotlinLogging.logger { }

		const val SCENE_WIDTH = 800.0

		const val SCENE_HEIGHT = 600.0

	}

}
