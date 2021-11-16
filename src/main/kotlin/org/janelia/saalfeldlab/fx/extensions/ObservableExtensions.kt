package org.janelia.saalfeldlab.fx.extensions

import javafx.beans.Observable
import javafx.beans.binding.Bindings
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.DoubleProperty
import javafx.beans.property.FloatProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.LongProperty
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.scene.Node
import kotlin.reflect.KProperty

fun <Obj, Obs> Obs.createObjectBinding(obsToObj: (Obs) -> Obj): ObjectBinding<Obj> where Obs : Observable {
    return Bindings.createObjectBinding({ obsToObj.invoke(this) }, this)
}

inline operator fun <reified T : Node> T.invoke(apply: T.() -> Unit): T {
    apply(this)
    return this
}

fun <T> ObservableValue<T?>.nullableVal(): ObservableDelegate<T?> = ObservableDelegate(this) { value }
fun <T> ObservableValue<T>.nonnullVal(): ObservableDelegate<T> = ObservableDelegate(this) { value!! }

fun <T> WritableValue<T?>.nullable(): WritableDelegate<T?> = WritableDelegate(this) { value }
fun <T> WritableValue<T>.nonnull(): WritableDelegate<T> = WritableDelegate(this) { value!! }

fun IntegerProperty.nullable(): WritableSubclassDelegate<Number?, Int?> = WritableSubclassDelegate(this) { value }
fun IntegerProperty.nonnull(): WritableSubclassDelegate<Number, Int> = WritableSubclassDelegate(this) { value!! }

fun DoubleProperty.nullable(): WritableSubclassDelegate<Number?, Double?> = WritableSubclassDelegate(this) { value }
fun DoubleProperty.nonnull(): WritableSubclassDelegate<Number, Double> = WritableSubclassDelegate(this) { value!! }

fun FloatProperty.nullable(): WritableSubclassDelegate<Number?, Float?> = WritableSubclassDelegate(this) { value }
fun FloatProperty.nonnull(): WritableSubclassDelegate<Number, Float> = WritableSubclassDelegate(this) { value!! }

fun LongProperty.nullable(): WritableSubclassDelegate<Number?, Long?> = WritableSubclassDelegate(this) { value }
fun LongProperty.nonnull(): WritableSubclassDelegate<Number, Long> = WritableSubclassDelegate(this) { value!! }


class ObservableDelegate<T>(private val obs: ObservableValue<T>, private inline val getter: () -> T) {

    operator fun getValue(t: Any?, property: KProperty<*>): T {
        return getter()
    }
}

class WritableDelegate<T>(private val obs: WritableValue<T>, private inline val getter: () -> T) {

    operator fun getValue(t: Any?, property: KProperty<*>): T = getter()

    operator fun setValue(t: Any?, property: KProperty<*>, newVal: T) {
        obs.value = newVal
    }
}

class WritableSubclassDelegate<T, K : T>(private val obs: WritableValue<T?>, private inline val getter: () -> K) {

    operator fun getValue(t: Any?, property: KProperty<*>): K = getter()

    operator fun setValue(t: Any?, property: KProperty<*>, newVal: K) {
        obs.value = newVal
    }
}
