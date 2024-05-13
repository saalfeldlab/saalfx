/**
 *
 */
package org.janelia.saalfeldlab.control.mcu;

import org.janelia.saalfeldlab.control.ButtonControl;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.function.IntConsumer;

/**
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class MCUButtonControl extends MCUControl implements ButtonControl {

	public static final int TOGGLE_ON = 127;

	public static final int TOGGLE_OFF = 0;

	private static final int STATUS = 0x90;

	private final int led;

	private boolean isSwitch = false;

	private final Receiver rec;

	private final ShortMessage ledMsg = new ShortMessage();

	public MCUButtonControl(final int led, final Receiver rec) {

		this.led = led;
		this.rec = rec;
	}

	private void send(final ShortMessage msg) throws InvalidMidiDataException {

		rec.send(msg, System.currentTimeMillis());
	}

	public void display() {

		if (led > 0) {
			try {
				ledMsg.setMessage(STATUS, led, value);
				send(ledMsg);
			} catch (final InvalidMidiDataException e) {
				e.printStackTrace();
			}
		}
	}

	public void setValueSilently(final int value) {
		var normValue = Math.min(127, Math.max(0, value));
		if (normValue != this.value) {
			this.value = normValue;
			display();
		}
	}

	@Override
	public void setValue(final int value) {

		var normValue = Math.min(127, Math.max(0, value));
		if (normValue != this.value) {
			this.value = normValue;
			display();

			for (final IntConsumer listener : listeners) {
				listener.accept(this.value);
			}
		}
	}

	@Override
	public int getMin() {

		return 0;
	}

	@Override
	public int getMax() {

		return 127;
	}

	@Override
	void update(final int data) {

		if (isSwitch) {
			if (data != 0)
				setValue(value == 0 ? data : 0);
		} else
			setValue(data);
	}

	@Override
	public boolean isToggle() {

		return isSwitch;
	}

	@Override
	public void setToggle(final boolean isSwitch) {

		this.isSwitch = isSwitch;
		display();
	}
}
