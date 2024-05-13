package org.janelia.saalfeldlab.fx.midi

import javafx.event.EventType
import javafx.scene.input.InputEvent

abstract class FxMidiEvent(val handle: Int, val value: Number, eventType: EventType<out FxMidiEvent>) : InputEvent(eventType) {

	companion object {
		val ANY = EventType<FxMidiEvent>(InputEvent.ANY, "MIDI")
	}

}

class MidiPotentiometerEvent(handle: Int, value: Number, eventType: EventType<out MidiPotentiometerEvent>) : FxMidiEvent(handle, value, eventType) {

	companion object {
		private val POTENTIOMETER: EventType<MidiPotentiometerEvent> = EventType(ANY, "POTENTIOMETER")
		val POTENTIOMETER_ABSOLUTE: EventType<MidiPotentiometerEvent> = EventType(POTENTIOMETER, "ABSOLUTE")
		val POTENTIOMETER_RELATIVE: EventType<MidiPotentiometerEvent> = EventType(POTENTIOMETER, "RELATIVE")
	}
}

open class MidiButtonEvent(handle: Int, value: Int, eventType: EventType<out MidiButtonEvent>) : FxMidiEvent(handle, value, eventType) {

	companion object {
		val BUTTON: EventType<MidiButtonEvent> = EventType(ANY, "BUTTON")
		val BUTTON_PRESSED: EventType<MidiButtonEvent> = EventType(BUTTON, "BUTTON_PRESSED")
		val BUTTON_RELEASED: EventType<MidiButtonEvent> = EventType(BUTTON, "BUTTON_RELEASED")
	}
}

class MidiToggleEvent(handle: Int, value: Int, eventType: EventType<out MidiToggleEvent>) : MidiButtonEvent(handle, value, eventType) {
	val isOn = value > 0

	companion object {

		val BUTTON_TOGGLE = EventType<MidiToggleEvent>(BUTTON, "BUTTON_TOGGLE")
	}
}

class MidiFaderEvent(handle: Int, value: Number, eventType: EventType<out MidiFaderEvent>) : FxMidiEvent(handle, value, eventType) {

	companion object {
		val FADER: EventType<MidiFaderEvent> = EventType(ANY, "FADER")
	}
}
