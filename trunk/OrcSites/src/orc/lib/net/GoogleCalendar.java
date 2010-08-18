//
// GoogleCalendar.java -- Java class GoogleCalendar
// Project OrcSites
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.net;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import orc.oauth.OAuthProvider;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.gdata.client.calendar.CalendarQuery;
import com.google.gdata.client.calendar.CalendarService;
import com.google.gdata.data.calendar.CalendarEntry;
import com.google.gdata.data.calendar.CalendarEventFeed;
import com.google.gdata.data.calendar.CalendarFeed;
import com.google.gdata.util.ServiceException;

/**
 * @author quark
 */
public class GoogleCalendar {
	private static final URL eventsURL;
	private static final URL calendarsURL;
	static {
		try {
			eventsURL = new URL("http://www.google.com/calendar/feeds/default/private/full");
			calendarsURL = new URL("http://www.google.com/calendar/feeds/default/owncalendars/full");
		} catch (final MalformedURLException e) {
			throw new AssertionError(e);
		}
	}

	CalendarService service;

	private GoogleCalendar(final CalendarService service) {
		this.service = service;
	}

	/**
	 * Return a new authenticated Google calendar.
	 * This cannot be a constructor because constructors are not pausable.
	 */
	public static GoogleCalendar authenticate(final OAuthProvider provider, final String consumer) throws Exception {
		final OAuthAccessor accessor = provider.authenticate(consumer, OAuth.newList("scope", "http://www.google.com/calendar/feeds/"));
		final CalendarService service = new CalendarService("orc-csres-utexas-edu");
		service.setAuthSubToken(accessor.accessToken, provider.getPrivateKey(consumer));
		return new GoogleCalendar(service);
	}

//	private static <E> E runThreaded(final Callable<E> thunk) throws IOException, ServiceException {
//		try {
//			return Kilim.runThreaded(thunk);
//		} catch (final IOException e) {
//			throw e;
//		} catch (final ServiceException e) {
//			throw e;
//		} catch (final Exception e) {
//			throw (RuntimeException) e;
//		}
//	}

	public CalendarEventFeed getEvents(final DateTime start, final DateTime end) throws IOException, ServiceException {
//		return runThreaded(new Callable<CalendarEventFeed>() {
//			public CalendarEventFeed call() throws IOException, ServiceException {
				final CalendarQuery query = new CalendarQuery(eventsURL);
				query.setMinimumStartTime(new com.google.gdata.data.DateTime(start.getMillis()));
				query.setMaximumStartTime(new com.google.gdata.data.DateTime(end.getMillis() - 1));
				return service.query(query, CalendarEventFeed.class);
//			}
//		});
	}

	public DateTimeZone getDefaultTimeZone() throws IOException, ServiceException {
//		return runThreaded(new Callable<DateTimeZone>() {
//			public DateTimeZone call() throws IOException, ServiceException {
				final CalendarFeed resultFeed = service.getFeed(calendarsURL, CalendarFeed.class);
				final CalendarEntry calendar = resultFeed.getEntries().get(0);
				return DateTimeZone.forID(calendar.getTimeZone().getValue());
//			}
//		});
	}
}
