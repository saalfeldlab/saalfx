package org.janelia.saalfeldlab.fx.midi

import javafx.scene.Scene
import javafx.scene.layout.HBox
import javafx.stage.Stage
import org.janelia.saalfeldlab.control.mcu.MCUButtonControl
import org.janelia.saalfeldlab.control.mcu.MCUControlPanel
import org.janelia.saalfeldlab.control.mcu.MCUFaderControl
import org.janelia.saalfeldlab.control.mcu.MCUVPotControl
import org.janelia.saalfeldlab.fx.actions.ActionSet.Companion.installActionSet
import org.janelia.saalfeldlab.fx.midi.MidiPotentiometerEvent.Companion.POTENTIOMETER_ABSOLUTE
import org.janelia.saalfeldlab.fx.midi.MidiPotentiometerEvent.Companion.POTENTIOMETER_RELATIVE
import org.testfx.framework.junit.ApplicationTest
import org.testfx.util.WaitForAsyncUtils
import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiMessage
import javax.sound.midi.Receiver
import javax.sound.midi.Transmitter
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MidiActionSetTest : ApplicationTest() {

	companion object XTouchMiniFxTest {

		private val mockTransmitter = object : Transmitter {
			override fun close() = Unit
			override fun setReceiver(receiver: Receiver?) = Unit
			override fun getReceiver() = mockReceiver
		}

		private val mockReceiver = object : Receiver {
			override fun close() = Unit
			override fun send(message: MidiMessage?, timeStamp: Long) = Unit
		}

		private val mockDevice = object : MidiDevice {
			override fun close() = Unit
			override fun getDeviceInfo(): MidiDevice.Info? = null
			override fun open() = Unit
			override fun isOpen(): Boolean = false
			override fun getMicrosecondPosition(): Long = 0L
			override fun getMaxReceivers(): Int = 0
			override fun getMaxTransmitters(): Int = 0
			override fun getReceiver(): Receiver? = null
			override fun getReceivers(): MutableList<Receiver>? = null
			override fun getTransmitter(): Transmitter? = null
			override fun getTransmitters(): MutableList<Transmitter>? = null
		}

		private val mockMidiControlPanel = object : MCUControlPanel(mockDevice, mockTransmitter, mockReceiver) {
			val vpotControls = mutableMapOf<Int, MCUVPotControl>()
			val vpotControlsId = mutableMapOf<Int, MCUVPotControl>()

			val faderControls = mutableMapOf<Int, MCUFaderControl>()
			val faderControlsId = mutableMapOf<Int, MCUFaderControl>()

			val buttonControls = mutableMapOf<Int, MCUButtonControl>()
			val buttonControlsId = mutableMapOf<Int, MCUButtonControl>()

			override fun getVPotControl(i: Int) = vpotControls.putIfAbsent(i, MCUVPotControl(0, mockReceiver)).let { vpotControls[i]!! }
			override fun getVPotControlById(i: Int) = vpotControlsId.putIfAbsent(i, MCUVPotControl(0, mockReceiver)).let { vpotControlsId[i]!! }
			override fun getFaderControl(i: Int) = faderControls.putIfAbsent(i, MCUFaderControl()).let { faderControls[i]!! }
			override fun getFaderControlById(i: Int) = faderControlsId.putIfAbsent(i, MCUFaderControl()).let { faderControlsId[i]!! }
			override fun getButtonControl(i: Int) = buttonControls.putIfAbsent(i, MCUButtonControl(0, mockReceiver)).let { buttonControls[i]!! }
			override fun getButtonControlById(i: Int) = buttonControlsId.putIfAbsent(i, MCUButtonControl(0, mockReceiver)).let { buttonControlsId[i]!! }
			override fun getNumVPotControls() = 8
			override fun getNumButtonControls() = 26
			override fun getNumFaderControls() = 1
		}
	}

	private lateinit var root: HBox

	override fun start(stage: Stage) {
		root = HBox()
		stage.scene = Scene(root)
		stage.show()
	}

	@AfterTest
	fun resetControl() {
		mockMidiControlPanel.vpotControls.clear()
		mockMidiControlPanel.vpotControlsId.clear()
		mockMidiControlPanel.faderControls.clear()
		mockMidiControlPanel.faderControlsId.clear()
		mockMidiControlPanel.buttonControls.clear()
		mockMidiControlPanel.buttonControlsId.clear()
	}

	@Test
	fun `absolute potentiometer`() {
		var prevValue: Number = -1
		MidiActionSet("Abs Pot", mockMidiControlPanel, root) {
			POTENTIOMETER_ABSOLUTE(0) {
				min = -223
				max = 223
				onAction { prevValue = it!!.value }
			}

			POTENTIOMETER_ABSOLUTE(1) { onAction { prevValue = it!!.value } }

			POTENTIOMETER_ABSOLUTE(2) {
				asPercent = true
				onAction { prevValue = it!!.value.toDouble() }
			}

			root.installActionSet(this)
		}

		mockMidiControlPanel.getVPotControl(0).value = -1000
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(-223, prevValue)

		mockMidiControlPanel.getVPotControl(0).value = 1000
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(223, prevValue)

		mockMidiControlPanel.getVPotControl(1).value = 30
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(30, prevValue)

		mockMidiControlPanel.getVPotControl(2).value = -1000
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(0.0, prevValue)

		mockMidiControlPanel.getVPotControl(2).value = 1000
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(1.0, prevValue)

		val expected: Double
		mockMidiControlPanel.getVPotControl(2).apply {
			val intValue = Random.nextInt(min, max + 1)
			expected = intValue.toDouble() / (max + min)
			value = intValue
		}
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(expected, prevValue)


	}

	@Test
	fun `relative potentiometer`() {
		var prevValue: Number = -1
		MidiActionSet("Rel Pot", mockMidiControlPanel, root) {
			POTENTIOMETER_RELATIVE(0) {
				min = -223
				max = 223
				onAction { prevValue = it!!.value }
			}

			POTENTIOMETER_RELATIVE(1) { onAction { prevValue = it!!.value } }

			POTENTIOMETER_RELATIVE(2) {
				asPercent = true
				onAction { prevValue = it!!.value }
			}
			root.installActionSet(this)
		}

		mockMidiControlPanel.getVPotControl(0).value = -1000
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(-7, prevValue)

		mockMidiControlPanel.getVPotControl(0).value = 1000
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(7, prevValue)

		mockMidiControlPanel.getVPotControl(1).value = 3
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(3, prevValue)


		mockMidiControlPanel.getVPotControl(2).value = -1000
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(-1.0, prevValue)

		mockMidiControlPanel.getVPotControl(2).value = 1000
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(1.0, prevValue)

		var expected: Double
		mockMidiControlPanel.getVPotControl(2).apply {
			val intValue = Random.nextInt(min, 0)
			expected = -(intValue.toDouble() / min)
			value = intValue
		}
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(expected, prevValue)

		mockMidiControlPanel.getVPotControl(2).apply {
			val intValue = Random.nextInt(0, max + 1)
			expected = intValue.toDouble() / max
			value = intValue
		}
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(expected, prevValue)
	}

	@Test
	fun `midi button`() {
		var prevValue: Number = Int.MIN_VALUE
		MidiActionSet("Button", mockMidiControlPanel, root) {
			MidiButtonEvent.BUTTON(0) { onAction { prevValue = it!!.value } }

			MidiButtonEvent.BUTTON_PRESSED(1) { onAction { prevValue = it!!.value } }
			MidiButtonEvent.BUTTON_RELEASED(2) { onAction { prevValue = it!!.value } }

			MidiButtonEvent.BUTTON_PRESSED(3) { onAction { prevValue = it!!.value } }
			MidiButtonEvent.BUTTON_RELEASED(3) { onAction { prevValue = it!!.value } }

			root.installActionSet(this)
		}

		mockMidiControlPanel.getButtonControl(0).value = 10
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(10, prevValue)
		mockMidiControlPanel.getButtonControl(0).value = 0
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(0, prevValue)

		mockMidiControlPanel.getButtonControl(1).value = 100
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(100, prevValue)
		mockMidiControlPanel.getButtonControl(1).value = 0
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(100, prevValue)

		mockMidiControlPanel.getButtonControl(2).value = 50
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(100, prevValue)
		mockMidiControlPanel.getButtonControl(2).value = 0
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(0, prevValue)


		mockMidiControlPanel.getButtonControl(0).value = 50
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(50, prevValue)
		mockMidiControlPanel.getButtonControl(0).value = 0
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(0, prevValue)
	}


	@Test
	fun `toggle button`() {
		var prevValue = false
		MidiActionSet("Toggle Button", mockMidiControlPanel, root) {
			MidiToggleEvent.BUTTON_TOGGLE(0) { onAction { prevValue = it!!.isOn } }
			root.installActionSet(this)
		}

		mockMidiControlPanel.getButtonControl(0).value = 10
		WaitForAsyncUtils.waitForFxEvents()
		assert(prevValue)
		mockMidiControlPanel.getButtonControl(0).value = 0
		WaitForAsyncUtils.waitForFxEvents()
		assert(!prevValue)
		mockMidiControlPanel.getButtonControl(0).value = 8
		WaitForAsyncUtils.waitForFxEvents()
		assert(prevValue)
	}

	@Test
	fun `midi fader`() {
		var prevValue: Number = Int.MIN_VALUE
		MidiActionSet("Fader", mockMidiControlPanel, root) {
			MidiFaderEvent.FADER(0) { onAction { prevValue = it!!.value } }
			MidiFaderEvent.FADER(1) {
				min = -100
				max = 100
				onAction { prevValue = it!!.value }
			}
			MidiFaderEvent.FADER(2) {
				asPercent = true
				onAction { prevValue = it!!.value }
			}
			root.installActionSet(this)
		}

		mockMidiControlPanel.getFaderControl(0).value = -1
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(0.0, prevValue)

		mockMidiControlPanel.getFaderControl(0).value = 128
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(127.0, prevValue)

		mockMidiControlPanel.getFaderControl(0).value = 10
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(10.0, prevValue)

		mockMidiControlPanel.getFaderControl(0).value = 0
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(0.0, prevValue)

		mockMidiControlPanel.getFaderControl(1).value = 127
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(100.0, prevValue)

		mockMidiControlPanel.getFaderControl(1).value = 0
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(-100.0, prevValue)

		mockMidiControlPanel.getFaderControl(1).value = 127 / 2
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(-100 + (100 - -100) * ((127 / 2) / 127.toDouble()), prevValue)

		mockMidiControlPanel.getFaderControl(1).value = 1270
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(100.0, prevValue)

		mockMidiControlPanel.getFaderControl(1).value = -1
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(-100.0, prevValue)

		mockMidiControlPanel.getFaderControl(2).value = -1000
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(0.0, prevValue)

		mockMidiControlPanel.getFaderControl(2).value = 1000
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(1.0, prevValue)

		val expected: Double
		mockMidiControlPanel.getFaderControl(2).apply {
			val intValue = Random.nextInt(0, 128)
			expected = intValue.toDouble() / 127
			value = intValue
		}
		WaitForAsyncUtils.waitForFxEvents()
		assertEquals(expected, prevValue)

	}


}


