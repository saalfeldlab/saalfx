package org.janelia.saalfeldlab.fx

import javafx.event.ActionEvent
import javafx.scene.control.ContextMenu
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import javafx.util.Pair
import org.slf4j.LoggerFactory

import java.lang.invoke.MethodHandles
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet
import java.util.Stack
import java.util.function.Consumer

class MenuFromHandlers @JvmOverloads constructor(entries: Collection<Pair<String, Consumer<ActionEvent>>> = listOf()) {

    private val entries = mutableListOf<Pair<String, Consumer<ActionEvent>>>()

    init {
        this.entries.addAll(entries)
    }

    private fun subMenus(): Set<MenuPath> {
        val subMenus = HashSet<MenuPath>()
        for (entry in entries) {
            val elements = entry.key.split(MENU_SPLIT.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            elements
                    .takeIf { it.isNotEmpty() }
                    ?.let { subMenus += MenuPath(*it.sliceArray(0 until it.size - 1)) }
        }
        return subMenus
    }

    fun asMenu(menuText: String): Menu {
        val menu = Menu(menuText)

        val parentElements = HashMap<MenuPath, Menu>()

        for (entry in entries) {
            val elementPath = MenuPath(*entry.key.split(MENU_SPLIT.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
            val parentPath = elementPath.parent()
            LOG.debug("Adding element {} with parents {} ({})", elementPath, parentPath, entry.key)
            val mi = MenuItem(elementPath.elementsCopy[elementPath.elementsCopy.size - 1])
                    .also { it.setOnAction { e -> entry.value.accept(e) } }
            LOG.debug("Menu item is mnemonic enabled: {}", mi.isMnemonicParsing)
            if (parentPath.elementsCopy.isEmpty()) {
                menu.items += mi
            } else {
                val toCreate = Stack<MenuPath>()
                run {
                    var p = elementPath.parent()
                    while (p.elementsCopy.isNotEmpty()) {
                        toCreate.add(p)
                        p = p.parent()
                    }
                }
                while (!toCreate.empty()) {
                    val p = toCreate.pop()
                    val path = parentElements[p]
                    if (path == null) {
                        val m = Menu(p.elementsCopy[p.elementsCopy.size - 1])
                        parentElements[p] = m
                        if (p.elementsCopy.size == 1)
                            menu.items.add(m)
                        else
                            parentElements[p.parent()]!!.items += m
                    }
                }
                parentElements[parentPath]!!.items += mi
            }

        }

        return menu
    }

    fun asContextMenu(menuText: String?): ContextMenu {
        val menu = ContextMenu()
        if (menuText != null)
            menu.items.addAll(Menus.disabledItem(menuText), SeparatorMenuItem())

        val parentElements = HashMap<MenuPath, Menu>()

        for (entry in entries) {
            val elementPath = MenuPath(*entry.key.split(MENU_SPLIT_REGEX).dropLastWhile { it.isEmpty() }.toTypedArray())
            val parentPath = elementPath.parent()
            LOG.debug("Adding element {} with parents {} ({})", elementPath, parentPath, entry.key)
            val mi = MenuItem(elementPath.elementsCopy[elementPath.elementsCopy.size - 1])
                    .also { it.setOnAction { e -> entry.value.accept(e) } }
            LOG.debug("Menu item is mnemonic enabled: {}", mi.isMnemonicParsing)
            if (parentPath.elementsCopy.isEmpty()) {
                menu.items += mi
            } else {
                val toCreate = Stack<MenuPath>()
                run {
                    var p = elementPath.parent()
                    while (p.elementsCopy.isNotEmpty()) {
                        toCreate.add(p)
                        p = p.parent()
                    }
                }
                while (!toCreate.empty()) {
                    val p = toCreate.pop()
                    val path = parentElements[p]
                    if (path == null) {
                        val m = Menu(p.elementsCopy[p.elementsCopy.size - 1])
                        parentElements[p] = m
                        if (p.elementsCopy.size == 1)
                            menu.items.add(m)
                        else
                            parentElements[p.parent()]!!.items += m
                    }
                }
                parentElements[parentPath]!!.items += mi
            }

        }

        return menu
    }

    class MenuEntryConflict(message: String) : Exception(message)

    private class MenuPath constructor(vararg elements: String) {

        private val elements = arrayOf(*elements)

        private val _hashCode = Arrays.hashCode(this.elements)

        override fun equals(other: Any?) = other is MenuPath && Arrays.equals(other.elements, elements)

        override fun hashCode() = _hashCode

        fun parent() = parent

        val parent: MenuPath
            get() = MenuPath(*elements.sliceArray(0 until elements.size - 1))

        val elementsCopy: Array<String>
            get() = elements.copyOf()



    }

    companion object {

        // mnemonics might not work without alt modifier...
        // https://bugs.openjdk.java.net/browse/JDK-8090026

        private val LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        private const val MENU_SPLIT = ">"

        private val MENU_SPLIT_REGEX = MENU_SPLIT.toRegex()
    }
}
