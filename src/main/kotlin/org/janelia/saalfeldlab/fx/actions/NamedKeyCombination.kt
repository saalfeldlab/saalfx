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

interface NamedKeyBinding {
	val keyBindingName : String
	val primaryCombinationProperty: SimpleObjectProperty<KeyCombination>
	var primaryCombination : KeyCombination

	val keyCodes: Set<KeyCode>
		get() {
			val codes = mutableSetOf<KeyCode>()
			(primaryCombinationProperty.get() as? KeyCodeCombination)?.code?.also { codes += it }
			codes += primaryCombinationProperty.get().modifierCodes
			return codes.toSet()
		}

	fun matches(event : KeyEvent, keysExclusive: Boolean = true) : Boolean {
		return if (keysExclusive) {
			primaryCombinationProperty.get().match(event)
		} else {
			val codesMatchIfCodeCombo = (primaryCombinationProperty.get() as? KeyCodeCombination)?.code?.let { it == event.code } ?: true
			codesMatchIfCodeCombo && event.modifierCodes.containsAll(primaryCombinationProperty.get().modifierCodes)
		}
	}

	val deepCopy: NamedKeyBinding
}

open class NamedKeyCombination(override val keyBindingName: String, primaryCombination: KeyCombination) : NamedKeyBinding {

	override val primaryCombinationProperty = SimpleObjectProperty(primaryCombination)
	override var primaryCombination: KeyCombination by primaryCombinationProperty.nonnull()

	override val deepCopy: NamedKeyCombination
		get() = NamedKeyCombination(keyBindingName, primaryCombination)

	override fun equals(other: Any?): Boolean {
		if (other is NamedKeyCombination)
			return other.keyBindingName === keyBindingName
		return false
	}

	override fun toString() = ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
		.append("name", keyBindingName)
		.append("primaryCombination", primaryCombination)
		.toString()

	override fun hashCode() = keyBindingName.hashCode()

	class CombinationMap(vararg combinations: NamedKeyBinding) : MutableMap<String, NamedKeyBinding> by mutableMapOf() {

		init {
			combinations.forEach { this += it }
		}

		class KeyCombinationAlreadyInserted(val keyCombination: NamedKeyBinding) :
			RuntimeException("Action with name ${keyCombination.keyBindingName} already present but tried to insert: $keyCombination")

		@Throws(KeyCombinationAlreadyInserted::class)
		fun addCombination(keyCombination: NamedKeyBinding) {
			if (containsKey(keyCombination.keyBindingName))
				throw KeyCombinationAlreadyInserted(keyCombination)
			this[keyCombination.keyBindingName] = keyCombination
		}

		fun matches(name: String, event: KeyEvent, keysExclusive : Boolean = true) : Boolean {
			return get(name)!!.matches(event, keysExclusive)
		}

		operator fun plusAssign(keyCombination: NamedKeyBinding) = addCombination(keyCombination)

		operator fun plus(keyCombination: NamedKeyBinding) = this.also { it.plusAssign(keyCombination) }

		operator fun contains(actionIdentifier: String) = this.containsKey(actionIdentifier)

		operator fun contains(keyCombination: NamedKeyBinding) = contains(keyCombination.keyBindingName)

		val deepCopy: CombinationMap
			get() = values.map { it.deepCopy }.toTypedArray().let { CombinationMap(*it) }
	}

	class OnlyModifierKeyCombination(vararg modifier: Modifier) : KeyCombination(*modifier) {
		override fun match(event: KeyEvent?): Boolean {
			return super.match(event)
		}
	}

}
