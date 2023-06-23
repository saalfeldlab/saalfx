package org.janelia.saalfeldlab.fx.ui

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.ListView
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuButton
import javafx.scene.control.MenuItem
import javafx.scene.layout.HBox
import javafx.stage.Stage
import org.janelia.saalfeldlab.fx.util.InvokeOnJavaFXApplicationThread
import org.junit.Test
import org.testfx.framework.junit.ApplicationTest
import org.testfx.util.WaitForAsyncUtils
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals


class MatchSelectionNodeTest : ApplicationTest() {

    lateinit var root: HBox

    override fun start(stage: Stage) {
        root = HBox()
        stage.scene = Scene(root)
        stage.show()
    }

    @Test
    fun `input test`() {
        val choices = (0 until 30).map { "Number: $it" }.toList()
        val matcher = MatchSelectionMenuButton(choices, "limit top 10") {}
        matcher.limit = 10
        InvokeOnJavaFXApplicationThread {
            root.children += matcher
        }
        WaitForAsyncUtils.waitForFxEvents()
        val button = lookup("limit top 10").queryAs(MenuButton::class.java)
        InvokeOnJavaFXApplicationThread { button.fire() }
        WaitForAsyncUtils.waitForFxEvents()

        lookup<ListView<String>> { true }.query<ListView<String>>().also {
            assertEquals(30, it.items.size, "Start with 30 Items ")
        }

        WaitForAsyncUtils.waitForFxEvents()
        clickOn(".text-field").write("10")

        val itemsAt10 = mutableListOf<String>()
        lookup<ListView<String>> { true }.query<ListView<String>>().also {
            assertEquals(10, it.items.size, "Filtered with 10 Items (input: 10) ")
            itemsAt10.addAll(it.items)
        }

        WaitForAsyncUtils.waitForFxEvents()
        clickOn(".text-field").write("20")

        val itemsAt20 = mutableListOf<String>()
        lookup<ListView<String>> { true }.query<ListView<String>>().also {
            assertEquals(10, it.items.size, "Filtered with 10 Items (input: 10) ")
            itemsAt20.addAll(it.items)
        }

        assertNotEquals(itemsAt10, itemsAt20, "Filtered items at (10) and (20) should be different")

    }

    @Test
    fun `cuttof 50`() {
        val choices = (0..20).map { "Number: $it" }.toList()
        val matcher = MatchSelectionMenuButton(choices, "cuttof 50") {}
        matcher.cutoff = 50
        InvokeOnJavaFXApplicationThread {
            root.children += matcher
        }
        WaitForAsyncUtils.waitForFxEvents()
        val button = lookup("cuttof 50").queryAs(MenuButton::class.java)
        InvokeOnJavaFXApplicationThread { button.fire() }
        WaitForAsyncUtils.waitForFxEvents()

        WaitForAsyncUtils.waitForFxEvents()
        clickOn(".text-field").write("Test")

        lookup<ListView<String>> { true }.query<ListView<String>>().also {
            assertEquals(0, it.items.size, "Items should be empty")
        }

        WaitForAsyncUtils.waitForFxEvents()
        doubleClickOn(".text-field").write("13")

        lookup<ListView<String>> { true }.query<ListView<String>>().also {
            assertEquals(1, it.items.size, "Only 1 item should be present")
            assertEquals("Number: 13", it.items[0], "Item should equal Number: ${13}")
        }

        doubleClickOn(".text-field").write("1")
        WaitForAsyncUtils.waitForFxEvents()

        lookup<ListView<String>> { true }.query<ListView<String>>().also {
            assert(it.items.size > 1) { "12 items should be present" }
            it.items.forEach { assertContains(it, "1", false, "1 should be present in each item ") }
        }

    }

    @Test
    fun `button limit 10`() {
        val choices = (0 until 30).map { "Number: $it" }.toList()
        val matcher = MatchSelectionMenuButton(choices, "limit top 10") {}
        matcher.limit = 10
        InvokeOnJavaFXApplicationThread {
            root.children += matcher
        }
        WaitForAsyncUtils.waitForFxEvents()
        val button = lookup("limit top 10").queryAs(MenuButton::class.java)
        InvokeOnJavaFXApplicationThread { button.fire() }
        WaitForAsyncUtils.waitForFxEvents()

        lookup<ListView<String>> { true }.query<ListView<String>>().also {
            assert(it.items.size > 10) { " Greater than 10 items " }
        }

        WaitForAsyncUtils.waitForFxEvents()
        clickOn(".text-field").write("Test")

        WaitForAsyncUtils.waitForFxEvents()
        lookup(".list-view").queryAs(ListView::class.java).also {
            assertEquals(10, it.items.size, "Size Should be only 10")
        }
    }

    @Test
    fun `button no limit`() {
        val choices = (0 until 30).map { "Number: $it" }.toList()
        val matcher = MatchSelectionMenuButton(choices, "no limit") {}
        InvokeOnJavaFXApplicationThread {
            root.children += matcher
        }
        WaitForAsyncUtils.waitForFxEvents()
        /* NOTE: for some reason, `clickOn` doesn't work for MenuButtons, when headless. Instead, just manually trigger `MenuButte#fire`.
        * If you want, you can verify when headed, that `clickOn` works properly. But the button firing isn't part of the
        * MatchSelectionMenuButton implementation, so I'm comfortable with this workaround. */
        val button = lookup("no limit").queryAs(MenuButton::class.java)
        InvokeOnJavaFXApplicationThread { button.fire() }
        WaitForAsyncUtils.waitForFxEvents()

        lookup<ListView<String>> { true }.query<ListView<String>>().also {
            assertEquals(30, it.items.size, "Size Should be 30 items ")
        }

        WaitForAsyncUtils.waitForFxEvents()
        clickOn(".text-field").write("Test")

        WaitForAsyncUtils.waitForFxEvents()
        lookup(".list-view").queryAs(ListView::class.java).also {
            assertEquals(30, it.items.size, "Size Should be 30")
        }
    }

    @Test
    fun `menu no limit`() {

        val choices = (0 until 30).map { "Number: $it" }.toList()
        val matcher = MatchSelectionMenu(choices, "menu no limit", null, {})
        val menuButton = MenuButton("Test Menu Button", null, matcher)
        InvokeOnJavaFXApplicationThread {
            root.children += menuButton
        }
        WaitForAsyncUtils.waitForFxEvents()
        /* NOTE: for some reason, `clickOn` doesn't work for MenuButtons, when headless. Instead, just manually trigger `MenuButte#fire`.
        * If you want, you can verify when headed, that `clickOn` works properly. But the button firing isn't part of the
        * MatchSelectionMenuButton implementation, so I'm comfortable with this workaround. */
        val button = lookup("Test Menu Button").queryAs(MenuButton::class.java)
        InvokeOnJavaFXApplicationThread { button.fire() }
        WaitForAsyncUtils.waitForFxEvents()
        clickOn("menu no limit")

        lookup<ListView<String>> { true }.query<ListView<String>>().also {
            assert(it.items.size > 10) { " Greater than 10 items " }
        }

        WaitForAsyncUtils.waitForFxEvents()
        clickOn(".text-field").write("Test")

        WaitForAsyncUtils.waitForFxEvents()
        lookup(".list-view").queryAs(ListView::class.java).also {
            assertEquals(30, it.items.size, "Size Should be 30")
        }
    }
}

class TestApp : Application() {

    override fun init() {

    }

    override fun start(primaryStage: Stage) {
        fun newMenu() : Menu {
            val matchSelectionMenu = MatchSelectionMenu(
                listOf("0qw1erq2wer2", "1qw2erq3wer3", "2qw3erq4wer4", "3qw4erq5wer5", "4qw5erq6wer6", "5qw6erq7wer7"),
                "test"
            ) { println(it)}
            val matchSelectionMenu2 = MatchSelectionMenu(
                listOf("0qw1erq2wer2", "1qw2erq3wer3", "2qw3erq4wer4", "3qw4erq5wer5", "4qw5erq6wer6", "5qw6erq7wer7"),
                "test"
            ) { println(it)}
            val matchSelectionMenu3 = MatchSelectionMenu(
                listOf("0qw1erq2wer2", "1qw2erq3wer3", "2qw3erq4wer4", "3qw4erq5wer5", "4qw5erq6wer6", "5qw6erq7wer7"),
                "test"
            ) { println(it)}
            val menu  = Menu("Menu")
            menu.items += MenuItem("0 - menu test")
            menu.items += matchSelectionMenu
            menu.items += matchSelectionMenu2
            menu.items += MenuItem("1 - menu test")
            menu.items += MenuItem("2 - menu test")
            menu.items += matchSelectionMenu3
            menu.items += MenuItem("3 - menu test")
            menu.items += MenuItem("4 - menu test")
            menu.items += MenuItem("5 - menu test")
            menu.items += MenuItem("6 - menu test")
            return menu
        }
        val menuBar = MenuBar(newMenu(), newMenu())
        val scene = Scene(menuBar)
        primaryStage.scene = scene
        primaryStage.show()
    }
}

fun main() {
    Application.launch(TestApp::class.java)
}
