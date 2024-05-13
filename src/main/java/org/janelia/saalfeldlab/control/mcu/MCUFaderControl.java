/**
 *
 */
package org.janelia.saalfeldlab.control.mcu;

import org.janelia.saalfeldlab.control.ClippingIntControl;

import java.util.function.IntConsumer;

/**
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class MCUFaderControl extends MCUControl implements ClippingIntControl {

	private final int min = 0;
	private final int max = 127;

	@Override
	public void setValue(final int value) {

		this.value = Math.min(max, Math.max(min, value));

		for (final IntConsumer listener : listeners) {
			listener.accept(this.value);
		}
	}

	@Override
	public int getMin() {

		return min;
	}

	@Override
	public int getMax() {

		return max;
	}

	@Override
	void update(final int data) {

		setValue(data);
	}
}
