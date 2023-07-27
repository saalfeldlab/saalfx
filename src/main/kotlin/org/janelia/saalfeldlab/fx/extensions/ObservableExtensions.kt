package org.janelia.saalfeldlab.fx.extensions

import javafx.beans.Observable
import javafx.beans.binding.*
import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.collections.ObservableSet
import javafx.scene.Node
import kotlin.reflect.KProperty

fun <Obj, Obs> Obs.createObservableBinding(vararg observables: Observable, obsToObj: (Obs) -> Obj): ObjectBinding<Obj> where Obs : Observable {
	return Bindings.createObjectBinding({ obsToObj.invoke(this) }, this, *observables)
}

inline fun <reified T, Obj, Obs> Obs.createNullableValueBinding(vararg observables: Observable, crossinline obsValToObj: (T?) -> Obj): ObjectBinding<Obj> where Obs : ObservableValue<T> {
	return Bindings.createObjectBinding({ obsValToObj(value) }, this, *observables)
}

inline fun <reified T, Obj, Obs> Obs.createNonNullValueBinding(vararg observables: Observable, crossinline obsValToObj: (T) -> Obj): ObjectBinding<Obj> where Obs : ObservableValue<T> {
	return Bindings.createObjectBinding({ obsValToObj(value) }, this, *observables)
}

inline fun <reified T, Obj, Obs> Obs.createNonNullProperty(vararg observables: Observable, crossinline obsValToObj: (T) -> Obj): Property<Obj> where Obs : Property<T> {
	val mappingBinding = createNonNullValueBinding(*observables) { obsValToObj(it) }
	val property = SimpleObjectProperty<Obj>()
	property.bind(mappingBinding)
	return property
}

inline fun <reified T, Obj, Obs> Obs.createNullableProperty(vararg observables: Observable, crossinline obsValToObj: (T?) -> Obj): Property<Obj?> where Obs : Property<T> {
	val mappingBinding = createNonNullValueBinding(*observables) { obsValToObj(it) }
	val property = SimpleObjectProperty<Obj>()
	property.bind(mappingBinding)
	return property
}

inline operator fun <reified T : Node> T.invoke(apply: T.() -> Unit): T {
	apply(this)
	return this
}

operator fun BooleanExpression.invoke() = get()
operator fun DoubleExpression.invoke() = get()
operator fun LongExpression.invoke() = get()
operator fun FloatExpression.invoke() = get()
operator fun IntegerExpression.invoke() = get()
operator fun StringExpression.invoke(): String? = get()
operator fun <E> ListExpression<E>.invoke(): ObservableList<E>? = get()
operator fun <K, V> MapExpression<K, V>.invoke(): ObservableMap<K, V>? = get()
operator fun <E> SetExpression<E>.invoke(): ObservableSet<E>? = get()
operator fun <T> ObjectExpression<T>.invoke(): T? = get()


fun <T> ObservableValue<T?>.nullableVal(): ObservableDelegate<T?> = ObservableDelegate(this) { value }
fun <T> ObservableValue<T>.nonnullVal(): ObservableDelegate<T> = ObservableDelegate(this) { value!! }

fun ReadOnlyIntegerProperty.nullableVal(): ObservableSubclassDelegate<Number?, Int?> = ObservableSubclassDelegate(this) { value }
fun ReadOnlyIntegerProperty.nonnullVal(): ObservableSubclassDelegate<Number, Int> = ObservableSubclassDelegate(this) { value!! }

fun ReadOnlyDoubleProperty.nullableVal(): ObservableSubclassDelegate<Number?, Double?> = ObservableSubclassDelegate(this) { value }
fun ReadOnlyDoubleProperty.nonnullVal(): ObservableSubclassDelegate<Number, Double> = ObservableSubclassDelegate(this) { value!! }

fun ReadOnlyFloatProperty.nullableVal(): ObservableSubclassDelegate<Number?, Float?> = ObservableSubclassDelegate(this) { value }
fun ReadOnlyFloatProperty.nonnullVal(): ObservableSubclassDelegate<Number, Float> = ObservableSubclassDelegate(this) { value!! }

fun ReadOnlyLongProperty.nullableVal(): ObservableSubclassDelegate<Number?, Long?> = ObservableSubclassDelegate(this) { value }
fun ReadOnlyLongProperty.nonnullVal(): ObservableSubclassDelegate<Number, Long> = ObservableSubclassDelegate(this) { value!! }

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

class ObservableSubclassDelegate<T, K : T>(private val obs: ObservableValue<T?>, private inline val getter: () -> K) {

	operator fun getValue(t: Any?, property: KProperty<*>): K = getter()
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

fun <T> ObservableValue<T>.addTriggeredListener(triggerWith : T = value, listener : (ObservableValue<out T>?, T, T) -> Unit) {
	val changeListener = ChangeListener<T> { observable, oldValue, newValue -> listener(observable, oldValue, newValue) }
	addListener(changeListener)
	listener(this, value, triggerWith)
}
