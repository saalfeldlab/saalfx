/**
 *
 */
package org.janelia.saalfeldlab.control.mcu;

import javax.sound.midi.*;

/**
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public abstract class MCUControlPanel implements Receiver {

	private static final int STATUS_CONTROL = 0xb0;
	private static final int STATUS_KEY = 0x90;
	private static final int STATUS_FADER = 0xe8;

	private final MidiDevice device;
	private final Transmitter trans;
	private final Receiver rec;

	public MCUControlPanel(final MidiDevice device,  final Transmitter trans, final Receiver rec) {

		this.device = device;
		this.trans = trans;
		this.rec = rec;
		trans.setReceiver(this);
	}

	abstract public MCUVPotControl getVPotControl(final int i);

	abstract protected MCUVPotControl getVPotControlById(final int i);

	abstract public MCUFaderControl getFaderControl(final int i);

	abstract protected MCUFaderControl getFaderControlById(final int i);

	abstract public MCUButtonControl getButtonControl(final int i);

	abstract protected MCUButtonControl getButtonControlById(final int i);

	abstract public int getNumVPotControls();

	abstract public int getNumButtonControls();

	abstract public int getNumFaderControls();

	protected void send(final ShortMessage msg) throws InvalidMidiDataException {

		rec.send(msg, System.currentTimeMillis());
	}

	protected void send(final byte status, final byte data1, final byte data2) throws InvalidMidiDataException {

		send(new ShortMessage(status, data1, data2));
	}

	protected void send(final int status, final int data1, final int data2) throws InvalidMidiDataException {

		send((byte)status, (byte)data1, (byte)data2);
	}

	@Override
	public void send(final MidiMessage msg, final long timeStamp) {

		if (msg instanceof ShortMessage) {

			final ShortMessage sm = (ShortMessage)msg;

			final int status = sm.getStatus();
			switch (status) {
			case STATUS_CONTROL: {
				final int data = sm.getData2();
				final MCUVPotControl control = getVPotControlById(sm.getData1());
				control.update(data);
			}
			break;
			case STATUS_KEY: {
				final int data = sm.getData2();
				final MCUButtonControl key = getButtonControlById(sm.getData1());
				key.update(data);
			}
			break;
			case STATUS_FADER: {
				final int data = sm.getData2();
				final MCUFaderControl fader = getFaderControlById(sm.getData1());
				fader.update(data);
			}
			break;
			}
		}
	}

	@Override
	public void close() {

		trans.close();
		rec.close();
		device.close();
	}
}
