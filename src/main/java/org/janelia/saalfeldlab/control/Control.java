/**
 *
 */
package org.janelia.saalfeldlab.control;

import java.util.Set;
import java.util.function.Consumer;

/**
 * A control element with listeners.
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public interface Control<C> {

	Set<Consumer<C>> getListeners();

	boolean addListener(Consumer<C> listener);

	boolean removeListener(Consumer<C> listener);

	void clearListeners();

	C getValue();

	void setValue(final C c);
}
