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

    operator fun getValue(t: Any, property: KProperty<*>): V {
        val foreignKey = foreignKeyProvider()
        return getOrPut(foreignKey) { valueGenerator(foreignKey) }
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

class LazyForeignValue<K, V>(val foreignKeyProvider: () -> K, val valueGenerator: (K) -> V) : MutableMap<K, V> by HashMap() {

    operator fun getValue(t: Any, property: KProperty<*>): V {
        val foreignKey = foreignKeyProvider()
        return getOrPut(foreignKey) {
            /* We only want a single value, so clear before we add this new one */
            clear()
            valueGenerator(foreignKey)
        }
    }
}
