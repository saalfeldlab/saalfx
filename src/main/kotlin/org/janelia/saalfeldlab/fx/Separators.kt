package org.janelia.saalfeldlab.fx

import javafx.geometry.Orientation
import javafx.scene.control.Separator

class Separators {

    companion object {

        @JvmStatic
        fun vertical(): Separator {
            return withOrientation(Orientation.VERTICAL)
        }

        @JvmStatic
        fun horizontal(): Separator {
            return withOrientation(Orientation.HORIZONTAL)
        }

        @JvmStatic
        fun withOrientation(orientation: Orientation): Separator {
            val separator = Separator()
            separator.orientation = orientation
            return separator
        }
    }

}
