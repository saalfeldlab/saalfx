package org.janelia.saalfeldlab.fx.actions

import javafx.event.EventType
import javafx.scene.input.MouseEvent
import org.apache.commons.lang.builder.ToStringBuilder
import org.apache.commons.lang.builder.ToStringStyle

class NamedMouseCombination(
    val name: String,
    val combination: MouseCombination,
    vararg target: EventType<MouseEvent>
) {
    val target = listOf(*target)

    override fun toString() = ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("name", name)
        .append("combination", combination)
        .append("target", target)
        .toString()

    val deepCopy: NamedMouseCombination
        get() = NamedMouseCombination(name, combination.deepCopy, *target.toTypedArray())


    class CombinationMap(vararg combinations: NamedMouseCombination) : MutableMap<String, NamedMouseCombination> by mutableMapOf() {

        init {
            combinations.forEach { this += it }
        }

        class NamedMouseCombinationAlreadyInserted(val keyCombination: NamedMouseCombination) :
            RuntimeException("Action with name ${keyCombination.name} already present but tried to insert: $keyCombination")

        @Throws(NamedMouseCombinationAlreadyInserted::class)
        fun addCombination(keyCombination: NamedMouseCombination) {
            if (containsKey(keyCombination.name))
                throw NamedMouseCombinationAlreadyInserted(keyCombination)
            this[keyCombination.name] = keyCombination
        }

        operator fun plusAssign(keyCombination: NamedMouseCombination) = addCombination(keyCombination)

        operator fun plus(keyCombination: NamedMouseCombination) = this.also { it.plusAssign(keyCombination) }

        operator fun contains(actionIdentifier: String) = this.containsKey(actionIdentifier)

        operator fun contains(keyCombination: NamedMouseCombination) = contains(keyCombination.name)

        val deepCopy: CombinationMap
            get() = values.map { it.deepCopy }.toTypedArray().let { CombinationMap(*it) }
    }
}
