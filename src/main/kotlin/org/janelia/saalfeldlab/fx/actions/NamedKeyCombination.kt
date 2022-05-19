package org.janelia.saalfeldlab.fx.actions

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import org.apache.commons.lang.builder.ToStringBuilder
import org.apache.commons.lang.builder.ToStringStyle
import org.janelia.saalfeldlab.fx.extensions.nonnull
import kotlin.collections.set

class NamedKeyCombination(val name: String, primaryCombination: KeyCombination) {

    constructor(name: String, keyCode: KeyCode, vararg modifiers: KeyCombination.Modifier) : this(name, KeyCodeCombination(keyCode, *modifiers))

    private val primaryCombinationProperty = SimpleObjectProperty(primaryCombination)
    var primaryCombination: KeyCombination by primaryCombinationProperty.nonnull()

    fun primaryCombinationProperty() = primaryCombinationProperty

    fun matches(event: KeyEvent) = primaryCombination.match(event)

    val deepCopy: NamedKeyCombination
        get() = NamedKeyCombination(name, primaryCombination)

    override fun equals(other: Any?): Boolean {
        if (other is NamedKeyCombination)
            return other.name === name
        return false
    }

    override fun toString() = ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("name", name)
        .append("primaryCombination", primaryCombination)
        .toString()

    override fun hashCode() = name.hashCode()

    class CombinationMap(vararg combinations: NamedKeyCombination) : MutableMap<String, NamedKeyCombination> by mutableMapOf() {

        init {
            combinations.forEach { this += it }
        }

        class KeyCombinationAlreadyInserted(val keyCombination: NamedKeyCombination) :
            RuntimeException("Action with name ${keyCombination.name} already present but tried to insert: $keyCombination")

        @Throws(KeyCombinationAlreadyInserted::class)
        fun addCombination(keyCombination: NamedKeyCombination) {
            if (containsKey(keyCombination.name))
                throw KeyCombinationAlreadyInserted(keyCombination)
            this[keyCombination.name] = keyCombination
        }

        fun matches(name: String, event: KeyEvent) = get(name)!!.matches(event)

        operator fun plusAssign(keyCombination: NamedKeyCombination) = addCombination(keyCombination)

        operator fun plus(keyCombination: NamedKeyCombination) = this.also { it.plusAssign(keyCombination) }

        operator fun contains(actionIdentifier: String) = this.containsKey(actionIdentifier)

        operator fun contains(keyCombination: NamedKeyCombination) = contains(keyCombination.name)

        val deepCopy: CombinationMap
            get() = values.map { it.deepCopy }.toTypedArray().let { CombinationMap(*it) }
    }

}
