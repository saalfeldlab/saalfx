package org.janelia.saalfeldlab.fx

import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.control.MenuItem

class Menus {

    companion object {

        fun menuItem(text: String, handler: (ActionEvent) -> Unit) = menuItem(text, EventHandler { handler(it) })

        @JvmStatic
        fun menuItem(
                text: String,
                handler: EventHandler<ActionEvent>?): MenuItem {
            val mi = MenuItem(text)
            handler.let { mi.onAction = it }
            return mi
        }

        @JvmStatic
        fun disabledItem(title: String): MenuItem {
            val item = MenuItem(title)
            item.isDisable = true
            return item
        }
    }

}
