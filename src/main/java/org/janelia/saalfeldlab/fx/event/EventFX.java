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

import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

public abstract class EventFX<E extends Event> implements EventHandler<E>, InstallAndRemove<Node>
{
	private final String name;

	private final EventType<E> eventType;

	private final Predicate<E> eventFilter;

	//	private final boolean consume;

	public EventFX(final String name, final EventType<E> eventType, final Predicate<E> eventFilter)
	{
		super();
		this.name = name;
		this.eventType = eventType;
		this.eventFilter = eventFilter;
	}

	public abstract void actOn(E event);

	public String name()
	{
		return name;
	}

	@Override
	public void installInto(final Node node)
	{
		node.addEventHandler(eventType, this);
	}

	@Override
	public void removeFrom(final Node node)
	{
		node.removeEventHandler(eventType, this);
	}

	@Override
	public void handle(final E e)
	{
		if (eventFilter.test(e))
		{
			actOn(e);
		}
	}

	public static EventFX<KeyEvent> KEY_PRESSED(final String name, final Consumer<KeyEvent> eventHandler, final
	Predicate<KeyEvent> eventFilter)
	{
		return new EventFXWithConsumer<>(name, KeyEvent.KEY_PRESSED, eventHandler, eventFilter);
	}

	public static EventFX<KeyEvent> KEY_RELEASED(final String name, final Consumer<KeyEvent> eventHandler, final
	Predicate<KeyEvent> eventFilter, final boolean consume)
	{
		return new EventFXWithConsumer<>(name, KeyEvent.KEY_RELEASED, eventHandler, eventFilter, consume);
	}

	public static EventFX<KeyEvent> KEY_RELEASED(final String name, final Consumer<KeyEvent> eventHandler, final
	Predicate<KeyEvent> eventFilter)
	{
		return KEY_RELEASED(name, eventHandler, eventFilter, true);
	}

	public static EventFX<KeyEvent> KEY_TYPED(final String name, final Consumer<KeyEvent> eventHandler, final
	Predicate<KeyEvent> eventFilter)
	{
		return new EventFXWithConsumer<>(name, KeyEvent.KEY_TYPED, eventHandler, eventFilter);
	}

	public static EventFX<MouseEvent> MOUSE_CLICKED(final String name, final Consumer<MouseEvent> eventHandler, final
	Predicate<MouseEvent> eventFilter)
	{
		return new EventFXWithConsumer<>(name, MouseEvent.MOUSE_CLICKED, eventHandler, eventFilter);
	}

	public static EventFX<MouseEvent> MOUSE_PRESSED(final String name, final Consumer<MouseEvent> eventHandler, final
	Predicate<MouseEvent> eventFilter)
	{
		return new EventFXWithConsumer<>(name, MouseEvent.MOUSE_PRESSED, eventHandler, eventFilter);
	}

	public static EventFX<MouseEvent> MOUSE_RELEASED(final String name, final Consumer<MouseEvent> eventHandler, final
	Predicate<MouseEvent> eventFilter)
	{
		return new EventFXWithConsumer<>(name, MouseEvent.MOUSE_RELEASED, eventHandler, eventFilter);
	}

	public static EventFX<MouseEvent> MOUSE_DRAGGED(final String name, final Consumer<MouseEvent> eventHandler, final
	Predicate<MouseEvent> eventFilter)
	{
		return new EventFXWithConsumer<>(name, MouseEvent.MOUSE_DRAGGED, eventHandler, eventFilter);
	}

	public static EventFX<MouseEvent> MOUSE_MOVED(final String name, final Consumer<MouseEvent> eventHandler, final
	Predicate<MouseEvent> eventFilter)
	{
		return new EventFXWithConsumer<>(name, MouseEvent.MOUSE_MOVED, eventHandler, eventFilter);
	}

	public static EventFX<ScrollEvent> SCROLL(final String name, final Consumer<ScrollEvent> eventHandler, final
	Predicate<ScrollEvent> eventFilter)
	{
		return new EventFXWithConsumer<>(name, ScrollEvent.SCROLL, eventHandler, eventFilter);
	}

	public static class EventFXWithConsumer<E extends Event> extends EventFX<E>
	{

		private final Consumer<E> eventHandler;

		private final boolean consume;

		public EventFXWithConsumer(
				final String name,
				final EventType<E> eventType,
				final Consumer<E> eventHandler,
				final Predicate<E> eventFilter)
		{
			this(name, eventType, eventHandler, eventFilter, false);
		}

		public EventFXWithConsumer(
				final String name,
				final EventType<E> eventType,
				final Consumer<E> eventHandler,
				final Predicate<E> eventFilter,
				final boolean consume)
		{
			super(name, eventType, eventFilter);
			this.eventHandler = eventHandler;
			this.consume = consume;
		}

		@Override
		public void actOn(final E event)
		{
			if (consume)
			{
				event.consume();
			}
			eventHandler.accept(event);
		}

	}

}
