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
import java.lang.invoke.MethodHandles
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.BeforeTest
import kotlin.test.Test

class TasksTest : ApplicationTest() {

    private lateinit var list: ListView<String>

    override fun start(stage: Stage) {
        list = ListView()
        val pane = Pane(list)
        stage.scene = Scene(pane, SCENE_WIDTH, SCENE_HEIGHT)
            .also { it.addEventFilter(Event.ANY) { LOG.trace("Filtering event in scene: {}", it) } }
            .also { it.addEventFilter(MouseEvent.ANY) { LOG.trace("Filtering mouse event in scene: {}", it) } }
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
        WaitForAsyncUtils.waitForFxEvents()
    }

    @Test
    fun testOnSucess() {
        val testText = "Single onSuccess Test"
        Tasks.createTask<String> { testText }
            .onSuccess { _, t -> list.items.add(t.value) }
            .submit()

        WaitForAsyncUtils.waitForFxEvents()

        val items = list.items
        Assert.assertArrayEquals(arrayOf(testText), items.toTypedArray())
    }


    @Test
    fun testOnEndWithSuccess() {
        val endOnSuccessText = "Single onEnd Test, expecting success"
        val task = Tasks.createTask<String> { endOnSuccessText }
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
    fun testOnEndWithSuccessBlocking() {
        val endOnSuccessText = "Single onEnd Test, expecting success"
        val result = Tasks.createTask<String> { endOnSuccessText }
            .onSuccess { _, t -> list.items.add(t.value) }
            .onEnd { t -> list.items.add(t.value) }
            .submitAndWait()

        WaitForAsyncUtils.waitForFxEvents()

        val items = list.items
        Assert.assertEquals(endOnSuccessText, result)
        Assert.assertArrayEquals(arrayOf(endOnSuccessText, endOnSuccessText), items.toTypedArray())
    }

    @Test
    fun testOnEndWithCancel() {
        val items = list.items
        val textWithoutCancel = "Single onEnd Test, expecting to never see this"
        val textWithCancel = "Single onEnd Test, expecting cancel"
        var canceled = false
        val maxTime = LocalDateTime.now().plus(5, ChronoUnit.SECONDS)
        val task = Tasks.createTask<String> {
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

    @Test
    fun testOnEndWithFailure() {
        val items = list.items
        val textWithFailure = "Single onEnd Test, expecting failure"
        /* Intentionally trigger failed*/
        val task = Tasks.createTask<String> { throw RuntimeException("Forced failure!") }
            .onSuccess { _, t -> list.items.add(t.get()) }
            .onEnd { list.items.add(textWithFailure) }
            .submit()

        WaitForAsyncUtils.waitForFxEvents()

        Assert.assertFalse(task.isCancelled)
        Assert.assertTrue(task.isDone)
        Assert.assertArrayEquals(arrayOf(textWithFailure), items.toTypedArray())
    }

    @Test
    fun testOnEndOnFailed() {
        val items = list.items
        val textWithEnd = "Single onFailure Test, expecting end"
        val textWithFailure = "Single onFailure Test, expecting failure"
        /* Intentionally trigger failure, with custom onFailed */
        val task = Tasks.createTask<String> { throw RuntimeException("Forced failure!") }
            .onSuccess { _, t -> list.items.add(t.get()) }
            .onEnd { list.items.add(textWithEnd) }
            .onFailed { _, _ -> list.items.add(textWithFailure) }
            .submit()

        WaitForAsyncUtils.waitForFxEvents()

        Assert.assertFalse(task.isCancelled)
        Assert.assertTrue(task.isDone)
        Assert.assertArrayEquals(arrayOf(textWithEnd, textWithFailure), items.toTypedArray())
    }


    @Test
    fun testOnFailedDefaultExceptionHandler() {

        class IntentionalTestException(msg: String) : Throwable(msg)

        val items = list.items
        val textWithEnd = "Single onFailure Test, expecting end"
        val textWithFailure = "Single onFailure Test, expecting failure"
        /* Intentionally trigger failure, with custom onFailed. onEnd should also trigger*/
        val task = Tasks.createTask<String> { throw IntentionalTestException("Forced failure!") }
            .onSuccess { _, t -> list.items.add(t.get()) }
            .onEnd { list.items.add(textWithEnd) }
            .onFailed { _, _ -> list.items.add(textWithFailure) }
            .submit()

        WaitForAsyncUtils.waitForFxEvents()

        var thrownException: Throwable? = null
        InvokeOnJavaFXApplicationThread {
            thrownException = task.exception
        }

        WaitForAsyncUtils.waitForFxEvents()

        Assert.assertFalse(task.isCancelled)
        Assert.assertTrue(task.isDone)
        Assert.assertNotNull(thrownException)

        @Suppress("AssertBetweenInconvertibleTypes") /*Intentional, to trigger the failure case */
        Assert.assertEquals(IntentionalTestException::class.java, thrownException!!::class.java)
        Assert.assertArrayEquals(arrayOf(textWithEnd, textWithFailure), items.toTypedArray())
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        const val SCENE_WIDTH = 800.0

        const val SCENE_HEIGHT = 600.0

    }

}
