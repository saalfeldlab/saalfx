package org.janelia.saalfeldlab.fx.extensions

import java.util.Optional

class UtilityExtensions {

    companion object {
        val <T> Optional<T>.nullable: T?
            get() = orElse(null)
    }
}
