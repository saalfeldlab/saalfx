package org.janelia.saalfeldlab.fx

import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.input.MouseEvent
import org.apache.commons.lang.builder.HashCodeBuilder
import org.janelia.saalfeldlab.fx.extensions.nonnullVal

class ObservablePosition(x: Double, y: Double) : Observable {


	val xProperty: SimpleDoubleProperty = SimpleDoubleProperty(x)
	val x: Double by xProperty.nonnullVal()

	val yProperty: SimpleDoubleProperty = SimpleDoubleProperty(y)
	val y: Double by yProperty.nonnullVal()


	private var listeners = mutableListOf<InvalidationListener>()

	fun set(event: MouseEvent) {
		set(event.x, event.y)
	}

	fun set(x: Double, y: Double) {
		if (x != this.x || y != this.y) {
			this.xProperty.set(x)
			this.yProperty.set(y)
			notifyListeners()
		}
	}

	fun setX(x: Double) {
		if (x != this.x) {
			this.xProperty.set(x)
			notifyListeners()
		}
	}

	fun setY(y: Double) {
		if (y != this.y) {
			this.yProperty.set(y)
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
