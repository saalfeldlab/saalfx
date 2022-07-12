package org.janelia.saalfeldlab.fx

import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.scene.input.MouseEvent
import org.apache.commons.lang.builder.HashCodeBuilder

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
        if (x != this.x || y != this.y) {
            this.x = x
            this.y = y
            notifyListeners()
        }
    }

    fun setX(x: Double) {
        if (x != this.x) {
            this.x = x
            notifyListeners()
        }
    }

    fun setY(y: Double) {
        if (y != this.y) {
            this.y = y
            notifyListeners()
        }
    }

    override fun equals(other: Any?): Boolean {
        return (other as? ObservablePosition)?.let {
            x == it.x && y == it.y
        } ?: false
    }

    override fun toString() = "($x, $y)"

    override fun hashCode() = HashCodeBuilder().append(x).append(y).toHashCode()

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
