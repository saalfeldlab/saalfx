/**
 *
 */
package org.janelia.saalfeldlab.control;

import java.util.Set;
import java.util.function.IntConsumer;

/**
 * A control element that modifies an integer value.
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public interface IntControl extends IntConsumer {

	Set<IntConsumer> getListeners();

	boolean addListener(IntConsumer listener);

	boolean removeListener(IntConsumer listener);

	void clearListeners();

	int getValue();

	void setValue(final int i);

	@Override
	default void accept(final int i) {

		setValue(i);
	}
}
