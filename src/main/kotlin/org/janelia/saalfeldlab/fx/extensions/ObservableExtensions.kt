package org.janelia.saalfeldlab.fx.extensions

import javafx.beans.Observable
import javafx.beans.binding.Bindings
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.DoubleProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.LongProperty
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import kotlin.reflect.KProperty

fun <Obj, Obs> Obs.createObjectBinding(obsToObj: (Obs) -> Obj): ObjectBinding<Obj> where Obs : Observable {
        return Bindings.createObjectBinding({ obsToObj.invoke(this) }, this)
}

operator fun <T> WritableValue<T>.setValue(ref: Any, property: KProperty<*>, value: T?) = setValue(value)
operator fun <T> ObservableValue<T>.getValue(ref: Any, property: KProperty<*>): T = value

operator fun IntegerProperty.setValue(ref: Any, property: KProperty<*>, value: Int?) = setValue(value)
operator fun IntegerProperty.getValue(ref: Any, property: KProperty<*>): Int = value

operator fun DoubleProperty.setValue(ref: Any, property: KProperty<*>, value: Double?) = setValue(value)
operator fun DoubleProperty.getValue(ref: Any, property: KProperty<*>): Double = value

operator fun LongProperty.setValue(ref: Any, property: KProperty<*>, value: Long?) = setValue(value)
operator fun LongProperty.getValue(ref: Any, property: KProperty<*>): Long = value
