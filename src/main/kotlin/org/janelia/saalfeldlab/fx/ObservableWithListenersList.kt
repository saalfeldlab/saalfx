package org.janelia.saalfeldlab.fx

import javafx.beans.InvalidationListener
import javafx.beans.Observable

open class ObservableWithListenersList : Observable {
    private val listeners = mutableListOf<InvalidationListener>()

    @Synchronized
    override fun addListener(listener: InvalidationListener) {
        this.listeners.add(listener)
        listener.invalidated(this)
    }

    @Synchronized
    override fun removeListener(listener: InvalidationListener) {
        this.listeners.remove(listener)
    }

    protected fun stateChanged() {
        // TODO should this be synchronized?
        for (i in listeners.indices) {
            listeners[i].invalidated(this)
        }
    }

}
