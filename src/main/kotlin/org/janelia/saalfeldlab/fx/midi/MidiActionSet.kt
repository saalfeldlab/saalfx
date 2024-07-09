package org.janelia.saalfeldlab.fx.midi

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.beans.property.SimpleBooleanProperty
import javafx.event.Event
import javafx.event.EventTarget
import javafx.event.EventType
import org.janelia.saalfeldlab.control.VPotControl.DisplayType
import org.janelia.saalfeldlab.control.mcu.*
import org.janelia.saalfeldlab.control.mcu.MCUButtonControl.TOGGLE_OFF
import org.janelia.saalfeldlab.control.mcu.MCUButtonControl.TOGGLE_ON
import org.janelia.saalfeldlab.fx.actions.Action
import org.janelia.saalfeldlab.fx.actions.ActionSet
import org.janelia.saalfeldlab.fx.event.KeyTracker
import org.janelia.saalfeldlab.fx.util.InvokeOnJavaFXApplicationThread
import java.util.function.IntConsumer
import kotlin.math.absoluteValue

open class MidiActionSet(name: String, private val device: MCUControlPanel, private val target: EventTarget, keyTracker: () -> KeyTracker? = { null }, callback: MidiActionSet.() -> Unit = {}) : ActionSet(name, keyTracker) {
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
		return PotentiometerAction(eventType, device, handle, name, withAction).also { addAction(it) }
	}

	@JvmSynthetic
	fun toggleAction(eventType: EventType<MidiToggleEvent>, handle: Int, withAction: ToggleAction.() -> Unit = {}): ToggleAction {
		return ToggleAction(eventType, device, handle, name, withAction).also { addAction(it) }
	}

	@JvmSynthetic
	fun buttonAction(eventType: EventType<MidiButtonEvent>, handle: Int, withAction: ButtonAction.() -> Unit = {}): ButtonAction {
		return ButtonAction(eventType, device, handle, name, withAction).also { addAction(it) }
	}

	@JvmSynthetic
	fun faderAction(eventType: EventType<MidiFaderEvent>, handle: Int, withAction: FaderAction.() -> Unit = {}): FaderAction {
		return FaderAction(eventType, device, handle, name, withAction).also { addAction(it) }
	}

}

abstract class MidiAction<E : FxMidiEvent>(eventType: EventType<E>, val device: MCUControlPanel, val handle: Int, name: String? = null, withAction: MidiAction<E>.() -> Unit = {}) : Action<E>(eventType) {
	abstract val control: MCUControl
	protected abstract var eventFiringListener: IntConsumer?
	var supressEvents = false

	override val logger: KLogger by lazy {
		val simpleName = this::class.simpleName?.let { ".$it" } ?: ""
		val finalName = ".${this.name ?: "event-${eventType.name}"}"
		KotlinLogging.logger("saalfx.action.midi$simpleName$finalName.$handle")
	}

	init {
		ignoreKeys()
		/* set the default name*/
		name?.let { this.name = it }
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

	abstract protected fun initializeControlState()

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

class PotentiometerAction(eventType: EventType<MidiPotentiometerEvent>, device: MCUControlPanel, handle: Int, name: String? = null, withAction: PotentiometerAction.() -> Unit = {}) : MidiAction<MidiPotentiometerEvent>(eventType, device, handle, name) {

	override val control: MCUVPotControl = device.getVPotControl(handle)
	override var eventFiringListener: IntConsumer? = null

	private var absolute: Boolean = (eventType == MidiPotentiometerEvent.POTENTIOMETER_ABSOLUTE).also { control.isAbsolute = it }

	/**
	 * Desired minimum value for the potentiometer event. Note, when the control is relative, it may override the specified min.
	 */
	var min: Int = (if (absolute) 0 else -MCUVPotControl.MAX_STEP).also { control.min = it }
		set(value) {
			control.min = value
			field = control.min
		}

	/**
	 * Desired maximum value for the potentiometer event. Note, when the control is relative, it may override the specified max.
	 */
	var max: Int = (if (absolute) 127 else MCUVPotControl.MAX_STEP).also { control.max = it }
		set(value) {
			control.max = value
			field = control.max
		}

	var displayType: DisplayType = (if (absolute) DisplayType.PAN else DisplayType.TRIM).also { control.setDisplayType(it) }

	var converter: (Int) -> Number = { it }

	var asPercent = false
		set(value) {
			field = value
			updateConverter()
		}

	private fun updateConverter() {
		when {
			absolute && asPercent -> converter = { (control.value.toDouble() - min) / (max - min) }
			asPercent -> converter = {
				when {
					control.value < 0 -> -(control.value.toDouble() / min)
					control.value > 0 -> control.value.toDouble() / max.absoluteValue
					else -> 0.0
				}
			}

			else -> converter = { it }
		}
	}

	private var controlValue: Int = let {
		control.setValueSilently(0)
		control.display()
		control.value
	}

	init {
		when (eventType) {
			MidiPotentiometerEvent.POTENTIOMETER_RELATIVE -> absolute = false
			MidiPotentiometerEvent.POTENTIOMETER_ABSOLUTE -> absolute = true
		}
		verify("Correct Handle") { it?.handle == handle }
		apply(withAction)
		if (!absolute) {
			min = min.coerceIn(-MCUVPotControl.MAX_STEP, MCUVPotControl.MAX_STEP)
			max = max.coerceIn(-MCUVPotControl.MAX_STEP, MCUVPotControl.MAX_STEP)
		}
	}

	override fun registerEvent(target: EventTarget?) {
		/* initialize control, since we can't guarantee it's current state otherwise */
		initializeControlState()
		eventFiringListener = IntConsumer {
			if (supressEvents) return@IntConsumer
			val value = converter(it)
			InvokeOnJavaFXApplicationThread {
				Event.fireEvent(target, MidiPotentiometerEvent(handle, value, eventType))
			}
		}
		control.addListener(eventFiringListener)
		afterRegisterEvent()
	}

	override fun initializeControlState() {
		control.isAbsolute = absolute
		control.setDisplayType(displayType)
		control.min = min
		control.max = max
		control.setValueSilently(controlValue)
		control.display()
	}
}

class ButtonAction(eventType: EventType<MidiButtonEvent>, device: MCUControlPanel, handle: Int, name: String? = null, withAction: ButtonAction.() -> Unit = {}) : MidiAction<MidiButtonEvent>(eventType, device, handle, name) {

	override val control: MCUButtonControl = device.getButtonControl(handle)
	override var eventFiringListener: IntConsumer? = null

	init {
		verify("Correct Handle") { it?.handle == handle }
		verify("Control is not toggle") { !control.isToggle }
		apply(withAction)
	}

	override fun initializeControlState() {
		control.isToggle = false
		control.setValueSilently(0)
	}

	override fun registerEvent(target: EventTarget?) {
		initializeControlState()
		eventFiringListener = listener(target)
		control.addListener(eventFiringListener)
		afterRegisterEvent()
	}

	private fun listener(target: EventTarget?): IntConsumer = IntConsumer {
		if (supressEvents) return@IntConsumer

		if (eventType == MidiButtonEvent.BUTTON_PRESSED && it != 0) {
			InvokeOnJavaFXApplicationThread {
				Event.fireEvent(target, MidiButtonEvent(handle, it, eventType))
			}
		} else if (eventType == MidiButtonEvent.BUTTON_RELEASED && it == 0) {
			InvokeOnJavaFXApplicationThread {
				Event.fireEvent(target, MidiButtonEvent(handle, it, eventType))
			}
		} else if (eventType == MidiButtonEvent.BUTTON) {
			InvokeOnJavaFXApplicationThread {
				Event.fireEvent(target, MidiButtonEvent(handle, it, eventType))
			}
		}
	}
}

class ToggleAction(eventType: EventType<MidiToggleEvent>, device: MCUControlPanel, handle: Int, name: String? = null, withAction: ToggleAction.() -> Unit = {}) : MidiAction<MidiToggleEvent>(eventType, device, handle, name) {

	override val control: MCUButtonControl = device.getButtonControl(handle)
	override var eventFiringListener: IntConsumer? = null

	val toggleDisplayProperty = SimpleBooleanProperty(control.value != TOGGLE_OFF)

	init {
		control.isToggle = true
		verify("Correct Handle") { it?.handle == handle }
		verify("Control is toggle") { control.isToggle }
		toggleDisplayProperty.addListener { _, _, toggle ->
			control.setValueSilently(if (toggle) TOGGLE_ON else TOGGLE_OFF)
			control.display()
		}
		apply(withAction)
	}

	override fun initializeControlState() {
		control.isToggle = true
		toggleDisplayProperty.unbind()
		toggleDisplayProperty.set(false)
		control.setValueSilently(TOGGLE_OFF)
	}

	override fun registerEvent(target: EventTarget?) {
		initializeControlState()
		eventFiringListener = IntConsumer {
			if (supressEvents) return@IntConsumer
			InvokeOnJavaFXApplicationThread {
				Event.fireEvent(target, MidiToggleEvent(handle, it, eventType))
			}
		}
		control.addListener(eventFiringListener)
		afterRegisterEvent()
	}

	override fun removeEvent() {
		super.removeEvent()
		/* Just to clean up the device, so it doesn't treat the button as a toggle anymore */
		toggleDisplayProperty.unbind()
		control.value = TOGGLE_OFF
		control.isToggle = false
	}

}

class FaderAction(eventType: EventType<MidiFaderEvent>, device: MCUControlPanel, handle: Int, name: String? = null, withAction: FaderAction.() -> Unit = {}) : MidiAction<MidiFaderEvent>(eventType, device, handle, name) {

	override val control: MCUFaderControl = device.getFaderControl(handle)
	override var eventFiringListener: IntConsumer? = null

	/**
	 * Desired minimum value for the fader event.
	 */
	var min: Number = control.min

	/**
	 * Desired maximum value for the fader event.
	 */
	var max: Number = control.max

	val convertToPerecent = { it: Int ->
		(it.toDouble() - control.min) / (control.max - control.min)
	}

	val convertToNumber = { it: Int ->
		(min.toDouble() + (max.toDouble() - min.toDouble()) * convertToPerecent(it))
	}

	/**
	 * Mapping from the fader's internal steps [0-127] to the target step.
	 */
	var converter: ((Int) -> Number) = convertToNumber

	var asPercent: Boolean = false
		set(value) {
			converter = if (value) convertToPerecent else convertToNumber
			field = value
		}

	init {
		verify("Correct Handle") { it?.handle == handle }
		apply(withAction)
	}

	override fun initializeControlState() {
		/* no control state*/
	}

	override fun registerEvent(target: EventTarget?) {
		initializeControlState()
		eventFiringListener = IntConsumer {
			if (supressEvents) return@IntConsumer
			val value = converter(it)
			InvokeOnJavaFXApplicationThread {
				Event.fireEvent(target, MidiFaderEvent(handle, value, eventType))
			}
		}
		control.addListener(eventFiringListener)
		afterRegisterEvent()
	}

	companion object {
		const val FADER_MAX = 127
		const val FADER_MIN = 0
	}
}
