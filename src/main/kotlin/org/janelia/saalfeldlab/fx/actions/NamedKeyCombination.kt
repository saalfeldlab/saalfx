package org.janelia.saalfeldlab.fx.actions

import com.sun.javafx.tk.Toolkit
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.input.*
import org.apache.commons.lang.builder.ToStringBuilder
import org.apache.commons.lang.builder.ToStringStyle
import org.janelia.saalfeldlab.fx.extensions.nonnull
import kotlin.collections.set

private val KeyCombination.modifierCodes: Set<KeyCode>
	get() = setOfNotNull(
		if (shift == KeyCombination.ModifierValue.DOWN) KeyCode.SHIFT else null,
		if (control == KeyCombination.ModifierValue.DOWN) KeyCode.CONTROL else null,
		if (alt == KeyCombination.ModifierValue.DOWN) KeyCode.ALT else null,
		if (meta == KeyCombination.ModifierValue.DOWN) KeyCode.META else null,
		if (shortcut == KeyCombination.ModifierValue.DOWN) Toolkit.getToolkit().platformShortcutKey else null
	)

private val KeyEvent.modifierCodes: Set<KeyCode>
	get() = setOfNotNull(
		if (isShiftDown) KeyCode.SHIFT else null,
		if (isControlDown) KeyCode.CONTROL else null,
		if (isAltDown) KeyCode.ALT else null,
		if (isMetaDown) KeyCode.META else null,
		if (isShortcutDown) Toolkit.getToolkit().platformShortcutKey else null
	)

class NamedKeyCombination(val name: String, primaryCombination: KeyCombination) {

	private val primaryCombinationProperty = SimpleObjectProperty(primaryCombination)
	var primaryCombination: KeyCombination by primaryCombinationProperty.nonnull()

	fun primaryCombinationProperty() = primaryCombinationProperty

	fun matches(event: KeyEvent) = primaryCombination.match(event)

	val keyCodes: Set<KeyCode>
		get() {
			val codes = mutableSetOf<KeyCode>()
			(primaryCombination as? KeyCodeCombination)?.code?.also { codes += it }
			codes += primaryCombination.modifierCodes
			return codes.toSet()
		}

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

		fun matches(name: String, event: KeyEvent, keysExclusive : Boolean = true) : Boolean {
			val namedCombo = get(name)!!
			return if (keysExclusive) {
				namedCombo.matches(event)
			} else {
				val combo = namedCombo.primaryCombination
				val codesMatchIfCodeCombo = (combo as? KeyCodeCombination)?.code?.let { it == event.code } ?: true
				codesMatchIfCodeCombo && event.modifierCodes.containsAll(combo.modifierCodes)
			}
		}

		operator fun plusAssign(keyCombination: NamedKeyCombination) = addCombination(keyCombination)

		operator fun plus(keyCombination: NamedKeyCombination) = this.also { it.plusAssign(keyCombination) }

		operator fun contains(actionIdentifier: String) = this.containsKey(actionIdentifier)

		operator fun contains(keyCombination: NamedKeyCombination) = contains(keyCombination.name)

		val deepCopy: CombinationMap
			get() = values.map { it.deepCopy }.toTypedArray().let { CombinationMap(*it) }
	}

	class OnlyModifierKeyCombination(vararg modifier: Modifier) : KeyCombination(*modifier) {
		override fun match(event: KeyEvent?): Boolean {
			return super.match(event)
		}
	}

}
