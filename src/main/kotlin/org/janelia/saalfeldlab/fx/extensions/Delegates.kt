package org.janelia.saalfeldlab.fx.extensions

import kotlin.reflect.KProperty


/**
 * Lazy foreign cached value. Useful when you want a value to change depending on some other property.
 *  In that case, you can utilize this via property delegation to always provide the correct value for the
 *  property that the foreignKeyProvider grabs from.
 *
 *  Similar to `lazy`, it will cache the value after the first generation. However, this also has the ability to
 *  generate a new value if [foreignKeyProvider] returns a differnt key. In that case, it will replace the old
 *  cached value with the new one.
 *
 * @param K type of object foreign key
 * @param V type of value generated
 * @property foreignKeyProvider a function to provide a key to cache against
 * @property valueGenerator a function to generate a value for a given key
 * @constructor This is intended to by constructed via delegation e.g `val test by LazyForeignValue( this::key) { it.getValue }`
 */
class LazyForeignMap<K, V>(val foreignKeyProvider: () -> K, val valueGenerator: (K) -> V) : MutableMap<K, V> by HashMap() {

	private var mapStateHandler: ((MutableMap<K, V>) -> Unit)? = null

	/**
	 * Callback to allow for modification of the current backing map just before the new value is generated and added to it.
	 * Useful to clear the map if desireable, for example.
	 *
	 * @param handleMapState callback to run with the backing map just before a new key/value is added.
	 * @return this, so you can use this builder-style and still assign as a delegate
	 */
	fun beforeMapChange(handleMapState : (MutableMap<K, V>) -> Unit) : LazyForeignMap<K, V> {
		mapStateHandler = handleMapState
		return this
	}

	operator fun getValue(t: Any, property: KProperty<*>): V {
		return synchronized(this) {
			val foreignKey = foreignKeyProvider()
			getOrPut(foreignKey) { valueGenerator(foreignKey) }
		}
	}
}

/**
 * Lazy foreign cached value. Useful when you want a value to change depending on some other property.
 *  In that case, you can utilize this via property delegation to always provide the correct value for the
 *  property that the foreignKeyProvider grabs from.
 *
 *  Similar to `lazy`, it will cache the value after the first generation. However, this also has the ability to
 *  generate a new value if [foreignKeyProvider] returns a differnt key. In that case, it will replace the old
 *  cached value with the new one.
 *
 * @param K type of object foreign key
 * @param V type of value generated
 * @property foreignKeyProvider a function to provide a key to cache against
 * @property valueGenerator a function to generate a value for a given key
 * @constructor This is intended to by constructed via delegation e.g `val test by LazyForeignValue( this::key) { it.getValue }`
 */

class LazyForeignValue<K, V>(val foreignKeyProvider: () -> K, val valueGenerator: (K) -> V) {

	private var currentKey: K? = null
	private var currentValue: V? = null

	private var oldValueHandler : ((V?) -> Unit)? = null

	/**
	 * Callback to handle the old value just before the new value is generated.
	 *
	 * @param handleOldValue callback to run with the provided soon-to-be old value
	 * @return this, so you can use this builder-style and still assign as a delegate
	 */
	fun beforeValueChange(handleOldValue : (V?) -> Unit): LazyForeignValue<K, V> {
		oldValueHandler = handleOldValue
		return this
	}

	operator fun getValue(t: Any?, property: KProperty<*>): V {
		return synchronized(this) {
			updateKeyAndValue()
		}
	}

	private fun updateKeyAndValue(): V {
		val foreignKey = foreignKeyProvider()
		return if (foreignKey == currentKey) currentValue as V
		else {
			currentKey = foreignKey
			val oldValue = currentValue
			currentValue = valueGenerator(currentKey as K)
			oldValueHandler?.invoke(oldValue)
			currentValue as V
		}
	}

	operator fun getValue(t: Nothing?, property: KProperty<*>): V {
		return synchronized(this) {
			updateKeyAndValue()
		}
	}
}
