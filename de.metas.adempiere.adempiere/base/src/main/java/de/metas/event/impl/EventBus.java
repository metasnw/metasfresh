package de.metas.event.impl;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
 * %%
 * Copyright (C) 2015 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */


import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

import de.metas.event.Event;
import de.metas.event.EventBusConstants;
import de.metas.event.IEventBus;
import de.metas.event.IEventListener;
import de.metas.event.Type;
import org.adempiere.util.Check;
import org.adempiere.util.lang.ObjectUtils;
import org.compiere.util.CLogger;

import com.google.common.eventbus.Subscribe;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;

final class EventBus implements IEventBus
{
	private static final transient CLogger logger = EventBusConstants.getLogger();

	/** Log all event bus exceptions */
	private static final SubscriberExceptionHandler exceptionHandler = new SubscriberExceptionHandler()
	{
		@Override
		public void handleException(final Throwable exception, final SubscriberExceptionContext context)
		{
			String errmsg = "Could not dispatch event: "  + context.getSubscriber() + " to " + context.getSubscriberMethod()
					+"\n Event: "+context.getEvent()
					+"\n Bus: "+context.getEventBus();
			logger.log(Level.SEVERE, errmsg, exception);
		}
	};

	private final String name;
	private com.google.common.eventbus.EventBus eventBus;

	private boolean destroyed = false;

	/**
	 * The default type is local, unless the factory makes this event bus "remote" by registering some sort of forwarder-subscriber.
	 */
	private Type type = Type.LOCAL;

	public EventBus(final String topicName, final ExecutorService executor)
	{
		super();
		Check.assumeNotEmpty(topicName, "name not empty");
		this.name = topicName;

		if (executor == null)
		{
			eventBus = new com.google.common.eventbus.EventBus(exceptionHandler);
		}
		else
		{
			eventBus = new com.google.common.eventbus.AsyncEventBus(executor, exceptionHandler);
		}
	}

	@Override
	public final String getName()
	{
		return name;
	}

	/**
	 * To be invoked only by the factory.
	 * 
	 * @param type
	 */
	/* package */void setTypeRemote()
	{
		this.type = Type.REMOTE;
	}

	@Override
	public Type getType()
	{
		return type;
	}

	void destroy()
	{
		this.destroyed = true;
		this.eventBus = null;
	}

	@Override
	public void subscribe(final IEventListener listener)
	{
		// Do nothing if destroyed
		if (destroyed)
		{
			logger.log(Level.WARNING, "Attempt to register a listener to a destroyed bus. Ignored."
					+ "\n Bus: " + this
					+ "\n Listener: " + listener);
			return;
		}

		final GuavaEventListenerAdapter listenerAdapter = new GuavaEventListenerAdapter(listener);
		eventBus.register(listenerAdapter);
	}

	@Override
	public void subscribeWeak(final IEventListener listener)
	{
		// Do nothing if destroyed
		if (destroyed)
		{
			logger.log(Level.WARNING, "Attempt to register a listener to a destroyed bus. Ignored."
					+ "\n Bus: " + this
					+ "\n Listener: " + listener);
			return;
		}

		final WeakGuavaEventListenerAdapter listenerAdapter = new WeakGuavaEventListenerAdapter(listener);
		eventBus.register(listenerAdapter);
	}

	@Override
	public void postEvent(final Event event)
	{
		// Do nothing if destroyed
		if (destroyed)
		{
			logger.log(Level.WARNING, "Attempt to post an event to a destroyed bus. Ignored."
					+ "\n Bus: " + this
					+ "\n Event: " + event);
			return;
		}

		logger.log(Level.FINE, "Posting event: {0}", event);
		eventBus.post(event);
	}

	public class GuavaEventListenerAdapter
	{
		private final IEventListener eventListener;

		GuavaEventListenerAdapter(final IEventListener eventListener)
		{
			super();

			Check.assumeNotNull(eventListener, "eventListener not null");
			this.eventListener = eventListener;
		}

		@Override
		public String toString()
		{
			return ObjectUtils.toString(this);
		}

		@Subscribe
		public void onEvent(final Event event)
		{
			eventListener.onEvent(EventBus.this, event);
		}
	}

	public class WeakGuavaEventListenerAdapter
	{
		private final WeakReference<IEventListener> eventListenerRef;

		WeakGuavaEventListenerAdapter(final IEventListener eventListener)
		{
			super();

			Check.assumeNotNull(eventListener, "eventListener not null");
			eventListenerRef = new WeakReference<>(eventListener);
		}

		@Override
		public String toString()
		{
			return ObjectUtils.toString(this);
		}

		@Subscribe
		public void onEvent(final Event event)
		{
			final IEventListener eventListener = eventListenerRef.get();
			if (eventListener == null)
			{
				final com.google.common.eventbus.EventBus guavaEventBus = EventBus.this.eventBus;
				if (guavaEventBus != null)
				{
					guavaEventBus.unregister(this);
				}
				return;
			}

			eventListener.onEvent(EventBus.this, event);
		}
	}

	@Override
	public boolean isDestroyed()
	{
		return destroyed;
	}

}