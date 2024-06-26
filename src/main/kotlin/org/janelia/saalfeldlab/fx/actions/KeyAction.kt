package org.janelia.saalfeldlab.fx.actions

import javafx.event.EventType
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import java.util.function.Consumer

/**
 * [Action] subclass that has convenience methods for handle [KeyEvent]s.
 * Simplifies the process of verifying an event which was triggerd by a key being release.
 *
 * @constructor creates a [KeyAction]
 *
 * @param eventType that the [KeyAction] is triggering
 */
class KeyAction(eventType: EventType<KeyEvent>) : Action<KeyEvent>(eventType) {

	private var keysReleased: List<KeyCode>? = null


	/**
	 * Provide a [NamedKeyCombination.CombinationMap] and a [keyName] to use to verify the valid key combination.
	 *
	 * @param keyBindings the map to query the key combinations for the given [keyName]
	 * @param keyName key used to find the desired key combination in the [keyBindings]
	 */
	@JvmOverloads
	fun keyMatchesBinding(keyBindings: NamedKeyCombination.CombinationMap, keyName: String, keysExclusive: Boolean = true) {
		val namedKeyCombo = keyBindings[keyName]!!
		keyMatchesBinding(namedKeyCombo, keysExclusive)
	}

	/**
	 * Provide a [NamedKeyCombination.CombinationMap] and a [NamedKeyCombination] to use to verify the valid key combination.
	 * NOTE: the [NamedKeyCombination.keyCodes] are not directly used. This is purely a convenience function to get the [NamedKeyCombination.keyBindingName]
	 *
	 * @param keyBindings the map to query the key combinations for the given [NamedKeyCombination.keyBindingName]
	 * @param namedKeyCombination which provided the key name used to find the desired key combination in the [keyBindings].
	 */
	@JvmOverloads
	fun keyMatchesBinding(namedKeyBinding: NamedKeyBinding, keysExclusive: Boolean = true) {


		if (name == null) name = namedKeyBinding.keyBindingName
		ignoreKeys()
		if (eventType == KeyEvent.KEY_RELEASED)
			keysReleased(*namedKeyBinding.keyCodes.toTypedArray())
		else
			verify { event ->
				/* always valid here if the event is null; it indicates we are triggering the action programmatically, not via an Event */
				event ?: return@verify true

				namedKeyBinding.matches(event, keyTracker(), keysExclusive).also { match ->
					if (!match) logger.trace { "key did not match bindings" }
				}
			}
	}

	/**
	 * Specify any number of keys that would make this [KeyAction] valid if ANY of them where release durign a [KeyEvent.KEY_RELEASED] event.
	 *
	 * [keyCodes] can contain multiple [KeyCode]s. When the [verifyKeys] function is called from this [KeyAction]
	 * to validate the key state of the event that triggered this action, if any of the keys indicated by [keyCodes] where released,
	 * then the keyState for this [KeyAction] will be considered valid.
	 *
	 * @param keyCodes all of the [KeyCode], of which any could cause the key state of this [KeyAction] to be valid
	 */
	fun keysReleased(vararg keyCodes: KeyCode) {
		ignoreKeys()
		verify {
			val keyTracker = keyTracker()
			val otherKeys = keyTracker?.getActiveKeyCodes(true)
			val otherKeysAreDown = otherKeys?.let { keyTracker.areKeysDown(*otherKeys.toTypedArray()) } ?: false
			val onlyOtherKeys = keyTracker?.activeKeyCount() == otherKeys?.size && (otherKeys?.size ?: -1) > 0
			if (keysExclusive && otherKeysAreDown && onlyOtherKeys) {
				true
			} else otherKeysAreDown
		}
		keysReleased = listOf(*keyCodes)
	}

	override fun verifyKeys(event: KeyEvent): Boolean {
		val keysValid = super.verifyKeys(event) && keysReleased?.let {
			// If we are checking a key on a release, we can't use the keyTracker
			val keyReleased = (event as? KeyEvent)?.code!!
			val otherKeys = it - keyReleased
			eventType == KeyEvent.KEY_RELEASED && /*ensure we are a KEY_RELEASED event */
					(it.isEmpty() || keyReleased in it) && /* ensure the key that was released was a trigger key*/
					keyTracker()?.areKeysDown(*otherKeys.toTypedArray()) ?: false /* ensure all OTHER trigger keys are down. */
		} ?: true
		return keysValid.also {
			if (!it) logger.trace("keys invalid")
		}
	}

	companion object {

		/**
		 * Create a [KeyAction] from an [EventType] reciever.
		 *
		 * @param T the event type to create the [KeyAction] for
		 * @param keyBindings used to validate the key state
		 * @param keys name of the entry in the [keyBindings] map for the target key combination
		 * @param action callback in the scope of the [KeyAction] created.
		 * @receiver the [EventType] to trigger the [KeyAction] on
		 */
		@JvmSynthetic
		fun <T : EventType<KeyEvent>> T.action(keyBindings: NamedKeyCombination.CombinationMap, keys: String, keysExclusive: Boolean = true, action: Action<KeyEvent>.() -> Unit) = KeyAction(this).also {
			it.keyMatchesBinding(keyBindings, keys, keysExclusive)
			it.action()
		}

		/**
		 * Create a [KeyAction] from an [EventType] reciever with no configuration, other than [keyMatchesBinding] on [keyBindings] and [keys]. Triggers [onAction] when valid.
		 *
		 * @param T the event type to create the [KeyAction] for
		 * @param keyBindings used to validate the key state
		 * @param keys name of the entry in the [keyBindings] map for the target key combination
		 * @param onAction callback when the [KeyAction] is valid.
		 * @receiver the [EventType] to trigger the [KeyAction] on
		 */
		@JvmSynthetic
		fun <T : EventType<KeyEvent>> T.onAction(keyBindings: NamedKeyCombination.CombinationMap, keys: String, keysExclusive: Boolean = true, onAction: (KeyEvent?) -> Unit) = KeyAction(this).also { action ->
			action.keyMatchesBinding(keyBindings, keys, keysExclusive)
			action.onAction { onAction(it) }
		}

		@JvmStatic
		@JvmOverloads
		fun <T : EventType<KeyEvent>> T.onAction(keyBindings: NamedKeyCombination.CombinationMap, keys: String, keysExclusive: Boolean = true, onAction: Consumer<KeyEvent?>) =
			onAction(keyBindings, keys, keysExclusive) { onAction.accept(it) }
	}
}
