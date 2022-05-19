package org.janelia.saalfeldlab.fx

import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.scene.input.MouseEvent

class ObservablePosition(x: Double, y: Double) : Observable {

    var x: Double = x
        private set

    var y: Double = y
        private set


    private var listeners = mutableListOf<InvalidationListener>()

    fun set(event: MouseEvent) {
        set(event.x, event.y)
    }

    fun set(x: Double, y: Double) {
        this.x = x
        this.y = y
        notifyListeners()
    }

    fun setX(x: Double) {
        this.x = x
        notifyListeners()
    }

    fun setY(y: Double) {
        this.y = y
        notifyListeners()
    }

    private fun notifyListeners() {
        listeners.forEach { it.invalidated(this) }
    }


    override fun addListener(listener: InvalidationListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: InvalidationListener) {
        listeners.remove(listener)
    }
}
