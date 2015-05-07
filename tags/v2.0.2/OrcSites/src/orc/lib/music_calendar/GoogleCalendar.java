//
// GoogleCalendar.java -- Java class GoogleCalendar
// Project OrcSites
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.music_calendar;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.oauth.OAuthProvider;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;

import com.google.gdata.client.calendar.CalendarService;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.calendar.CalendarEntry;
import com.google.gdata.data.calendar.CalendarEventEntry;
import com.google.gdata.data.calendar.CalendarFeed;
import com.google.gdata.data.calendar.ColorProperty;
import com.google.gdata.data.calendar.HiddenProperty;
import com.google.gdata.data.calendar.SelectedProperty;
import com.google.gdata.data.calendar.TimeZoneProperty;
import com.google.gdata.data.extensions.When;
import com.google.gdata.data.extensions.Where;
import com.google.gdata.util.ServiceException;

/**
 * @author quark, tfinster
 */
public class GoogleCalendar extends EvalSite {
	private static final String CALENDAR_TITLE = "Orc Music Calendar";
	private static final URL ownCalendarsURL;
	static {
		try {
			ownCalendarsURL = new URL("http://www.google.com/calendar/feeds/default/owncalendars/full");
		} catch (final MalformedURLException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * This implements the main Google Calendar stuff.
	 * @author quark
	 */
	public static class GoogleCalendarInstance {
		private final String consumer;
		private final CalendarService service;
		private boolean authenticated = false;
		private URL eventsURL;
		private final OAuthProvider provider;

		/**
		 * Constructs an object of class GoogleCalendarInstance.
		 *
		 * @param provider
		 * @param consumer name of the Google consumer in the properties file
		 * @throws IOException if properties cannot be loaded
		 */
		public GoogleCalendarInstance(final OAuthProvider provider, final String consumer) throws IOException {
			this.provider = provider;
			this.consumer = consumer;
			this.service = new CalendarService("ut-OrcMusicCalendar-dev");
		}

		/**
		 * Authenticate with Google using OAuth.
		 * Also creates a calendar as a side effect.
		 */
		public void authenticate() throws Exception {
			final OAuthAccessor accessor = provider.authenticate(consumer, OAuth.newList("scope", "http://www.google.com/calendar/feeds/"));
			service.setAuthSubToken(accessor.accessToken, provider.getPrivateKey(consumer));

			// Create the calendar we'll post events to
			final CalendarEntry cal = createMusicCalendar();
			// MAGIC: this technique for getting the events
			// URL is undocumented but works well
			eventsURL = new URL(cal.getLink("alternate", null).getHref());

			synchronized (this) {
				authenticated = true;
			}
		}

		/**
		 * Return the ID of the "Orc Music Calendar" calendar,
		 * creating one if it doesn't exist already.
		 */
		private CalendarEntry createMusicCalendar() throws IOException, ServiceException {
			// look for an existing calendar
			// FIXME: I wanted to use an ExtendedProperty to
			// mark our calendar but I couldn't get that to work
			final CalendarFeed calendars = service.getFeed(ownCalendarsURL, CalendarFeed.class);
			for (final CalendarEntry cal : calendars.getEntries()) {
				if (cal.getTitle().getPlainText().equals(CALENDAR_TITLE)) {
					return cal;
				}
			}
			// create a new calendar
			final CalendarEntry calendar = new CalendarEntry();
			calendar.setTitle(new PlainTextConstruct(CALENDAR_TITLE));
			calendar.setSummary(new PlainTextConstruct("This calendar was generated by the Orc Music Calendar demo at http://orc.csres.utexas.edu"));
			calendar.setTimeZone(new TimeZoneProperty("America/Chicago"));
			calendar.setHidden(HiddenProperty.FALSE);
			calendar.setSelected(SelectedProperty.TRUE);
			calendar.setColor(new ColorProperty("#2952A3"));
			calendar.addLocation(new Where("", "", "Austin, TX"));
			return service.insert(ownCalendarsURL, calendar);
		}

		/** Add a music show record. */
		public void addMusicShow(final MusicShow show) throws Exception {
			synchronized (this) {
				if (!authenticated) {
					throw new OAuthException("Not authenticated.");
				}
			}
			final Calendar startDate = new GregorianCalendar();
			startDate.setTime(show.getDate());

			final Calendar endDate = new GregorianCalendar();
			endDate.setTime(show.getDate());
			endDate.add(Calendar.MINUTE, 90);

			final String location = String.format("%s, %s, %s", show.getLocation(), show.getCity(), show.getState());

			addEventToCalendar(show.getTitle(), show.getTitle(), location, startDate, endDate);
		}

		/**
		 * Actually add an event to a calendar. BLOCKING.
		 */
		private void addEventToCalendar(final String eventTitle, final String eventContent, final String location, final Calendar startDate, final Calendar endDate) throws ServiceException, IOException {
			final When eventTimes = new When();
			eventTimes.setStartTime(new DateTime(startDate.getTime(), TimeZone.getDefault()));
			eventTimes.setEndTime(new DateTime(endDate.getTime(), TimeZone.getDefault()));

			final Where where = new Where();
			where.setValueString(location);

			final CalendarEventEntry myEntry = new CalendarEventEntry();

			myEntry.setTitle(new PlainTextConstruct(eventTitle));
			myEntry.setContent(new PlainTextConstruct(eventContent));
			myEntry.addLocation(where);
			myEntry.addTime(eventTimes);

			service.insert(eventsURL, myEntry);
		}
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		try {
			return new GoogleCalendarInstance((OAuthProvider) args.getArg(0), args.stringArg(1));
		} catch (final IOException e) {
			throw new JavaException(e);
		} catch (final ClassCastException e) {
			throw new ArgumentTypeMismatchException(0, "OAuthProvider", args.getArg(0).getClass().getCanonicalName());
		}
	}
}
