/*-
 * #%L
 * Saalfeld lab JavaFX tools and extensions
 * %%
 * Copyright (C) 2019 Philipp Hanslovsky, Stephan Saalfeld
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.janelia.saalfeldlab.fx.event;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class MouseClickFX implements InstallAndRemove<Node>
{

	private final EventFX<MouseEvent> onPress;

	private final EventFX<MouseEvent> onRelease;

	private final Consumer<MouseEvent> onPressConsumer;

	private final Consumer<MouseEvent> onReleaseConsumer;

	private final Predicate<MouseEvent> eventFilter;

	public MouseClickFX(final String name, final Consumer<MouseEvent> onReleaseConsumer, final Predicate<MouseEvent>
			eventFilter)
	{
		this(name, event -> {
		}, onReleaseConsumer, eventFilter);
	}

	public MouseClickFX(final String name, final Consumer<MouseEvent> onPressConsumer, final Consumer<MouseEvent>
			onReleaseConsumer, final Predicate<MouseEvent> eventFilters)
	{
		super();
		this.onPressConsumer = onPressConsumer;
		this.onReleaseConsumer = onReleaseConsumer;
		this.eventFilter = eventFilters;
		this.onPress = EventFX.MOUSE_PRESSED(name, this::press, this.eventFilter);
		this.onRelease = EventFX.MOUSE_RELEASED(name, this::release, event -> isEvent);
	}

	private double startX;

	private double startY;

	private boolean isEvent;

	private final double tolerance = 1.0;

	private boolean testEvent(final MouseEvent event)
	{
		return eventFilter.test(event);
	}

	private void press(final MouseEvent event)
	{
		if (testEvent(event))
		{
			startX = event.getX();
			startY = event.getY();
			isEvent = true;
			onPressConsumer.accept(event);
		}
	}

	private void release(final MouseEvent event)
	{
		final double x  = event.getX();
		final double y  = event.getY();
		final double dX = x - startX;
		final double dY = y - startY;
		if (dX * dX + dY * dY <= tolerance * tolerance)
		{
			onReleaseConsumer.accept(event);
		}
		isEvent = false;
	}

	@Override
	public void installInto(final Node node)
	{
		onPress.installInto(node);
		onRelease.installInto(node);
	}

	@Override
	public void removeFrom(final Node node)
	{
		onPress.removeFrom(node);
		onRelease.removeFrom(node);
	}

	public EventHandler<MouseEvent> handler() {
		return event -> {
			if (MouseEvent.MOUSE_PRESSED.equals(event.getEventType()))
				onPress.handle(event);
			else if (MouseEvent.MOUSE_RELEASED.equals(event.getEventType()))
				onRelease.handle(event);

		};
	}

}
