/**
 *
 */
package org.janelia.saalfeldlab.control;

/**
 * A control element that modifies an integer value but clips
 * it to a given range.
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public interface AdjustableClippingIntControl extends ClippingIntControl {

	void setMin(final int min);

	void setMax(final int max);

	void setMinMax(final int min, final int max);
}
