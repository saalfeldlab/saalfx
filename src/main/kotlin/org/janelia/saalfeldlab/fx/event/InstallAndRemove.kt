package org.janelia.saalfeldlab.fx.event

interface InstallAndRemove<T> {

    fun installInto(t: T)

    fun removeFrom(t: T)

}
