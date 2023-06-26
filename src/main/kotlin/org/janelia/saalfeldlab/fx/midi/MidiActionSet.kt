package org.janelia.saalfeldlab.fx.midi

import javafx.event.Event
import javafx.event.EventTarget
import javafx.event.EventType
import org.janelia.saalfeldlab.control.VPotControl
import org.janelia.saalfeldlab.control.mcu.*
import org.janelia.saalfeldlab.fx.actions.Action
import org.janelia.saalfeldlab.fx.actions.ActionSet
import org.janelia.saalfeldlab.fx.event.KeyTracker
import java.util.function.IntConsumer

open class MidiActionSet(name: String, private val device: MCUControlPanel, private val target: EventTarget, keyTracker: KeyTracker? = null, callback: MidiActionSet.() -> Unit = {}) : ActionSet(name, keyTracker) {
	init {
		callback()
	}

	override fun preInstallSetup() {
		actions
			.filterIsInstance<MidiAction<*>>()
			.forEach { it.registerEvent(target) }
	}

	override fun postRemoveCleanUp() {
		actions
			.filterIsInstance<MidiAction<*>>()
			.forEach { it.removeEvent() }
	}

	operator fun EventType<MidiPotentiometerEvent>.invoke(handle: Int, withAction: PotentiometerAction.() -> Unit): PotentiometerAction {
		return potentiometerAction(this, handle, withAction)
	}

	operator fun EventType<MidiButtonEvent>.invoke(handle: Int, withAction: ButtonAction.() -> Unit): ButtonAction {
		return buttonAction(this, handle, withAction)
	}

	operator fun EventType<MidiToggleEvent>.invoke(handle: Int, withAction: ToggleAction.() -> Unit): ToggleAction {
		return toggleAction(this, handle, withAction)
	}

	operator fun EventType<MidiFaderEvent>.invoke(handle: Int, withAction: FaderAction.() -> Unit): FaderAction {
		return faderAction(this, handle, withAction)
	}


	@JvmSynthetic
	fun potentiometerAction(eventType: EventType<MidiPotentiometerEvent>, handle: Int, withAction: PotentiometerAction.() -> Unit = {}): PotentiometerAction {
		return PotentiometerAction(eventType, device, handle, withAction).also { addAction(it) }
	}

	@JvmSynthetic
	fun toggleAction(eventType: EventType<MidiToggleEvent>, handle: Int, withAction: ToggleAction.() -> Unit = {}): ToggleAction {
		return ToggleAction(eventType, device, handle, withAction).also { addAction(it) }
	}

	@JvmSynthetic
	fun buttonAction(eventType: EventType<MidiButtonEvent>, handle: Int, withAction: ButtonAction.() -> Unit = {}): ButtonAction {
		return ButtonAction(eventType, device, handle, withAction).also { addAction(it) }
	}

	@JvmSynthetic
	fun faderAction(eventType: EventType<MidiFaderEvent>, handle: Int, withAction: FaderAction.() -> Unit = {}): FaderAction {
		return FaderAction(eventType, device, handle, withAction).also { addAction(it) }
	}

}

abstract class MidiAction<E : FxMidiEvent>(eventType: EventType<E>, val device: MCUControlPanel, val handle: Int, withAction: MidiAction<E>.() -> Unit = {}) : Action<E>(eventType) {
	abstract val control: MCUControl
	protected abstract var eventFiringListener: IntConsumer?
	var supressEvents = false

	init {
		ignoreKeys()
		apply(withAction)
	}

	var afterRegisterEvent: () -> Unit = {}
	var afterRemoveEvent: () -> Unit = {}

	fun updateControlSilently(value: Int) {
		val prevSuppressFlag = supressEvents
		supressEvents = true
		control.value = value
		supressEvents = prevSuppressFlag
	}

	abstract fun registerEvent(target: EventTarget?)
	open fun removeEvent() {
		eventFiringListener?.let {
			if (control.removeListener(eventFiringListener)) {
				eventFiringListener = null
			}
		}
		control.value = 0
		afterRemoveEvent()
	}
}

class PotentiometerAction(eventType: EventType<MidiPotentiometerEvent>, device: MCUControlPanel, handle: Int, withAction: PotentiometerAction.() -> Unit = {}) : MidiAction<MidiPotentiometerEvent>(eventType, device, handle) {

	override val control: MCUVPotControl = device.getVPotControl(handle)
	override var eventFiringListener: IntConsumer? = null

	var absolute: Boolean = control.isAbsolute
		set(value) {
			control.isAbsolute = value
			field = control.isAbsolute
		}

	/**
	 * Desired minimum value for the potentiometer event. Note, when the control is relative, it may override the specified min.
	 */
	var min: Int = control.min
		set(value) {
			control.min = value
			field = control.min
		}

	/**
	 * Desired maximum value for the potentiometer event. Note, when the control is relative, it may override the specified max.
	 */
	var max: Int = control.max
		set(value) {
			control.max = value
			field = control.max
		}

	init {
		when (eventType) {
			MidiPotentiometerEvent.POTENTIOMETER_RELATIVE -> absolute = false
			MidiPotentiometerEvent.POTENTIOMETER_ABSOLUTE -> absolute = true
		}
		verify("Correct Handle") { it?.handle == handle }
		verify("Control State Has Not Changed") { control.min == min && control.max == max && control.isAbsolute == absolute }
		apply(withAction)
		if (!absolute) {
			min = min.coerceIn(-MCUVPotControl.MAX_STEP, MCUVPotControl.MAX_STEP)
			max = max.coerceIn(-MCUVPotControl.MAX_STEP, MCUVPotControl.MAX_STEP)
		}
	}

	override fun registerEvent(target: EventTarget?) {
		eventFiringListener = IntConsumer {
			if (supressEvents) return@IntConsumer
			Event.fireEvent(target, MidiPotentiometerEvent(handle, it, eventType))
		}
		control.addListener(eventFiringListener)
		afterRegisterEvent()
	}

	fun setDisplayType(displayType: VPotControl.DisplayType) {
		control.setDisplayType(displayType)
	}
}

class ButtonAction(eventType: EventType<MidiButtonEvent>, device: MCUControlPanel, handle: Int, withAction: ButtonAction.() -> Unit = {}) : MidiAction<MidiButtonEvent>(eventType, device, handle) {

	override val control: MCUButtonControl = device.getButtonControl(handle)
	override var eventFiringListener: IntConsumer? = null

	init {
		verify("Correct Handle") { it?.handle == handle }
		verify("Control is not toggle") { !control.isToggle }
		apply(withAction)
	}

	override fun registerEvent(target: EventTarget?) {

		eventFiringListener = listener(target)
		control.addListener(eventFiringListener)
		afterRegisterEvent()
	}

	private fun listener(target: EventTarget?): IntConsumer = IntConsumer {
		if (supressEvents) return@IntConsumer

		if (eventType == MidiButtonEvent.BUTTON_PRESED && it != 0) {
			Event.fireEvent(target, MidiButtonEvent(handle, it, eventType))
		} else if (eventType == MidiButtonEvent.BUTTON_RELEASED && it == 0) {
			Event.fireEvent(target, MidiButtonEvent(handle, it, eventType))
		} else if (eventType == MidiButtonEvent.BUTTON) {
			Event.fireEvent(target, MidiButtonEvent(handle, it, eventType))
		}
	}
}

class ToggleAction(eventType: EventType<MidiToggleEvent>, device: MCUControlPanel, handle: Int, withAction: ToggleAction.() -> Unit = {}) : MidiAction<MidiToggleEvent>(eventType, device, handle) {

	override val control: MCUButtonControl = device.getButtonControl(handle)
	override var eventFiringListener: IntConsumer? = null

	init {
		control.isToggle = true
		verify("Correct Handle") { it?.handle == handle }
		verify("Control is toggle") { control.isToggle }
		apply(withAction)

	}


	override fun registerEvent(target: EventTarget?) {
		eventFiringListener = IntConsumer {
			if (supressEvents) return@IntConsumer
			Event.fireEvent(target, MidiToggleEvent(handle, it, eventType))
		}
		control.addListener(eventFiringListener)
		afterRegisterEvent()
	}

	override fun removeEvent() {
		super.removeEvent()
		/* Just to clean up the device, so it doesn't treat the button as a toggle anymore */
		control.value = MCUButtonControl.TOGGLE_OFF
		control.isToggle = false
	}

}

class FaderAction(eventType: EventType<MidiFaderEvent>, device: MCUControlPanel, handle: Int, withAction: FaderAction.() -> Unit = {}) : MidiAction<MidiFaderEvent>(eventType, device, handle) {

	override val control: MCUFaderControl = device.getFaderControl(handle)
	override var eventFiringListener: IntConsumer? = null

	/**
	 * Desired minimum value for the fader event.
	 */
	var min: Int = control.min

	/**
	 * Desired maximum value for the fader event.
	 */
	var max: Int = control.max


	val value: Int = control.value
	val step: Int
		get() = control.value

	/**
	 * Mapping from the fader's internal steps [0-127] to the target step.
	 */
	var stepToValueConverter: ((Int) -> Int) = {
		((it.toDouble() / (FADER_MAX - FADER_MIN)) * (max - min) + min).toInt()
	}

	init {
		verify("Correct Handle") { it?.handle == handle }
		apply(withAction)
	}

	override fun registerEvent(target: EventTarget?) {
		eventFiringListener = IntConsumer {
			if (supressEvents) return@IntConsumer
			val converter = stepToValueConverter
			Event.fireEvent(target, MidiFaderEvent(handle, converter(it), eventType))
		}
		control.addListener(eventFiringListener)
		afterRegisterEvent()
	}

	companion object {
		const val FADER_MAX = 127
		const val FADER_MIN = 0
	}
}
