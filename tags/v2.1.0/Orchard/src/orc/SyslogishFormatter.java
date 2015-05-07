//
// SyslogishFormatter.java -- Java class SyslogishFormatter
// Project Orchard
//
// $Id$
//
// Created by jthywiss on Apr 25, 2012.
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Log formatter that uses a syslog-inspired format
 *
 * @author jthywiss
 */
public class SyslogishFormatter extends Formatter {

	protected static String lineSeparator = System.getProperty("line.separator");
	private final Calendar timestamp = new GregorianCalendar(TimeZone.getTimeZone("GMT"), Locale.ROOT);

	/* (non-Javadoc)
	 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
	 */
	@Override
	public String format(final LogRecord record) {
		// Log line format:
		// dateTtimeZ app class method [thread]: level: message
		// possibly followed by stack trace

		final StringBuffer sb = new StringBuffer();

		timestamp.setTimeInMillis(record.getMillis());
		sb.append(timestamp.get(Calendar.YEAR));
		sb.append('-');
		if (timestamp.get(Calendar.MONTH) < 10) {
			sb.append('0');
		}
		sb.append(timestamp.get(Calendar.MONTH));
		sb.append('-');
		if (timestamp.get(Calendar.DAY_OF_MONTH) < 10) {
			sb.append('0');
		}
		sb.append(timestamp.get(Calendar.DAY_OF_MONTH));
		sb.append('T');
		if (timestamp.get(Calendar.HOUR_OF_DAY) < 10) {
			sb.append('0');
		}
		sb.append(timestamp.get(Calendar.HOUR_OF_DAY));
		sb.append(':');
		if (timestamp.get(Calendar.MINUTE) < 10) {
			sb.append('0');
		}
		sb.append(timestamp.get(Calendar.MINUTE));
		sb.append(':');
		if (timestamp.get(Calendar.SECOND) < 10) {
			sb.append('0');
		}
		sb.append(timestamp.get(Calendar.SECOND));
		sb.append('.');
		if (timestamp.get(Calendar.MILLISECOND) < 100) {
			sb.append('0');
		}
		if (timestamp.get(Calendar.MILLISECOND) < 10) {
			sb.append('0');
		}
		sb.append(timestamp.get(Calendar.MILLISECOND));
		sb.append("Z - "); // No app in jul.LogRecords

		if (record.getSourceClassName() != null && !record.getSourceClassName().isEmpty()) {
			sb.append(record.getSourceClassName());
		} else {
			// If no stack trace, assume the logger name is the class name
			if (record.getLoggerName() != null && !record.getLoggerName().isEmpty()) {
				sb.append(record.getLoggerName());
			} else {
				sb.append('-');
			}
		}
		sb.append(' ');

		if (record.getSourceMethodName() != null && !record.getSourceMethodName().isEmpty()) {
			sb.append(record.getSourceMethodName());
		} else {
			sb.append('-');
		}

		sb.append(" [thread ");
		sb.append(record.getThreadID());
		sb.append("]: ");

		sb.append(record.getLevel().getLocalizedName());
		sb.append(": ");

		sb.append(formatMessage(record));
		sb.append(lineSeparator);

		if (record.getThrown() != null) {
			try {
				final StringWriter sw = new StringWriter();
				final PrintWriter pw = new PrintWriter(sw);
				record.getThrown().printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (final Exception ex) {
			}
		}
		return sb.toString();
	}

}
