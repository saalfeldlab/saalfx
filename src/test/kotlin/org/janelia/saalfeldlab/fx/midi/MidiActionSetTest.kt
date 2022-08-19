package org.janelia.saalfeldlab.fx.midi

import javafx.scene.Scene
import javafx.scene.layout.HBox
import javafx.stage.Stage
import org.janelia.saalfeldlab.control.mcu.*
import org.janelia.saalfeldlab.fx.actions.ActionSet.Companion.installActionSet
import org.janelia.saalfeldlab.fx.midi.MidiPotentiometerEvent.Companion.POTENTIOMETER_ABSOLUTE
import org.janelia.saalfeldlab.fx.midi.MidiPotentiometerEvent.Companion.POTENTIOMETER_RELATIVE
import org.testfx.framework.junit.ApplicationTest
import org.testfx.util.WaitForAsyncUtils
import javax.sound.midi.MidiMessage
import javax.sound.midi.Receiver
import javax.sound.midi.Transmitter
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MidiActionSetTest : ApplicationTest() {

    companion object XTouchMiniFx {

        private val dummyTransmitter = object : Transmitter {
            override fun close() {}
            override fun setReceiver(receiver: Receiver?) {}
            override fun getReceiver() = dummyReceiver
        }

        private val dummyReceiver = object : Receiver {
            override fun close() {}
            override fun send(message: MidiMessage?, timeStamp: Long) {}
        }

        private val dummyDevice = object : MCUControlPanel(dummyTransmitter, dummyReceiver) {
            val vpotControls = mutableMapOf<Int, MCUVPotControl>()
            val vpotControlsId = mutableMapOf<Int, MCUVPotControl>()

            val faderControls = mutableMapOf<Int, MCUFaderControl>()
            val faderControlsId = mutableMapOf<Int, MCUFaderControl>()

            val buttonControls = mutableMapOf<Int, MCUButtonControl>()
            val buttonControlsId = mutableMapOf<Int, MCUButtonControl>()

            override fun getVPotControl(i: Int) = vpotControls.putIfAbsent(i, MCUVPotControl(0, dummyReceiver)).let { vpotControls[i]!! }
            override fun getVPotControlById(i: Int) = vpotControlsId.putIfAbsent(i, MCUVPotControl(0, dummyReceiver)).let { vpotControlsId[i]!! }
            override fun getFaderControl(i: Int) = faderControls.putIfAbsent(i, MCUFaderControl()).let { faderControls[i]!! }
            override fun getFaderControlById(i: Int) = faderControlsId.putIfAbsent(i, MCUFaderControl()).let { faderControlsId[i]!! }
            override fun getButtonControl(i: Int) = buttonControls.putIfAbsent(i, MCUButtonControl(0, dummyReceiver)).let { buttonControls[i]!! }
            override fun getButtonControlById(i: Int) = buttonControlsId.putIfAbsent(i, MCUButtonControl(0, dummyReceiver)).let { buttonControlsId[i]!! }
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
        dummyDevice.vpotControls.clear()
        dummyDevice.vpotControlsId.clear()
        dummyDevice.faderControls.clear()
        dummyDevice.faderControlsId.clear()
        dummyDevice.buttonControls.clear()
        dummyDevice.buttonControlsId.clear()
    }

    @Test
    fun `absolute potentiometer`() {
        var prevValue = -1
        MidiActionSet("Abs Pot", dummyDevice, root) {
            POTENTIOMETER_ABSOLUTE(0) {
                min = -223
                max = 223
                onAction { prevValue = it!!.value }
            }

            POTENTIOMETER_ABSOLUTE(1) { onAction { prevValue = it!!.value } }
            root.installActionSet(this)
        }

        dummyDevice.getVPotControl(0).value = -1000
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue == -223)

        dummyDevice.getVPotControl(0).value = 1000
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue == 223)

        dummyDevice.getVPotControl(1).value = 30
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue == 30)

    }

    @Test
    fun `relative potentiometer`() {
        var prevValue = -1
        MidiActionSet("Rel Pot", dummyDevice, root) {
            POTENTIOMETER_RELATIVE(0) {
                min = -223
                max = 223
                onAction { prevValue = it!!.value }
            }

            POTENTIOMETER_RELATIVE(1) { onAction { prevValue = it!!.value } }
            root.installActionSet(this)
        }

        dummyDevice.getVPotControl(0).value = -1000
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue == -7)

        dummyDevice.getVPotControl(0).value = 1000
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue == 7)

        dummyDevice.getVPotControl(1).value = 3
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue == 3)
    }

    @Test
    fun `midi button`() {
        var prevValue = Int.MIN_VALUE
        MidiActionSet("Button", dummyDevice, root) {
            MidiButtonEvent.BUTTON(0) { onAction { prevValue = it!!.value } }

            MidiButtonEvent.BUTTON_PRESED(1) { onAction { prevValue = it!!.value } }
            MidiButtonEvent.BUTTON_RELEASED(2) { onAction { prevValue = it!!.value } }

            MidiButtonEvent.BUTTON_PRESED(3) { onAction { prevValue = it!!.value } }
            MidiButtonEvent.BUTTON_RELEASED(3) { onAction { prevValue = it!!.value } }

            root.installActionSet(this)
        }

        dummyDevice.getButtonControl(0).value = 10
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue == 10)
        dummyDevice.getButtonControl(0).value = 0
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue == 0)

        dummyDevice.getButtonControl(1).value = 100
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue == 100)
        dummyDevice.getButtonControl(1).value = 0
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue == 100)

        dummyDevice.getButtonControl(2).value = 50
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue == 100)
        dummyDevice.getButtonControl(2).value = 0
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue == 0)


        dummyDevice.getButtonControl(0).value = 50
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue == 50)
        dummyDevice.getButtonControl(0).value = 0
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue == 0)
    }


    @Test
    fun `toggle button`() {
        var prevValue = false
        MidiActionSet("Toggle Button", dummyDevice, root) {
            MidiToggleEvent.BUTTON_TOGGLE(0) { onAction { prevValue = it!!.isOn }}
            root.installActionSet(this)
        }

        dummyDevice.getButtonControl(0).value = 10
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue)
        dummyDevice.getButtonControl(0).value = 0
        WaitForAsyncUtils.waitForFxEvents()
        assert(!prevValue)
        dummyDevice.getButtonControl(0).value = 8
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue)
    }

    @Test
    fun `midi fader`() {
        var prevValue = Int.MIN_VALUE
        MidiActionSet("Fader", dummyDevice, root) {
            MidiFaderEvent.FADER(0) { onAction { prevValue = it!!.value } }
            MidiFaderEvent.FADER(1) {
                min = -100
                max = 100
                onAction { prevValue = it!!.value }
            }
            root.installActionSet(this)
        }

        dummyDevice.getFaderControl(0).value = 10
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue == 10)
        dummyDevice.getFaderControl(0).value = 0
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue == 0)
        dummyDevice.getFaderControl(0).value = 8
        WaitForAsyncUtils.waitForFxEvents()
        assert(prevValue == 8)


        dummyDevice.getFaderControl(1).value = (.5 * 127).toInt()
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals(0, prevValue)

        dummyDevice.getFaderControl(1).value = 127
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals(100, prevValue)

        dummyDevice.getFaderControl(1).value = 0
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals(-100, prevValue)

        dummyDevice.getFaderControl(1).value = 1270
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals(100, prevValue)

        dummyDevice.getFaderControl(1).value = -1
        WaitForAsyncUtils.waitForFxEvents()
        assertEquals(-100, prevValue)
    }


}


