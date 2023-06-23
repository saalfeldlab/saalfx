/**
 *
 */
package org.janelia.saalfeldlab.control;

/**
 * A virtual potentiometer control (V-Pot).  V-Pots are report either an
 * absolute position or a relative step size up or down.  Regardless of the
 * reported value, they have no physical start- or end-position, i.e. they are
 * like mouse-wheels and can rotate indefinitely in one direction.
 * <p>
 * V-Pots have a supporting LED display that supports various visual feedback
 * about the current state of the control.
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public interface VPotControl extends AdjustableClippingIntControl {

	enum DisplayType {

		/**
		 * no display
		 */
		NONE,

		/**
		 * single LED display starting with the first LED and ending with the last
		 * (good for visualizing the position in an absolute range [0,n])
		 */
		PAN,

		/**
		 * fan out left or right from the center LED (good for visualizing relative
		 * steps or for a position in an absolute range [-n,n])
		 */
		TRIM,

		/**
		 * fan out starting with first LED until all LEDs are on (good for
		 * visualizing the position in an absolute range [0,n])
		 */
		FAN,

		/**
		 * fan out left and right from the center LED until all LEDs are on (good
		 * for visualizing the position in an absolute range [0,n])
		 */
		SPREAD
	}

	/*
	 * Returns whether this VPot control tracks an absolute value
	 * or reports relative changes.
	 *
	 * @return true if the control tracks an absolute value
	 *         false if this control reports relative changes
	 */
	boolean isAbsolute();

	void setAbsolute(final boolean absolute);

	/**
	 * Set the LED display type to one of the above types.
	 *
	 * @param display the {@link DisplayType} to use
	 */
	void setDisplayType(final DisplayType display);
}
