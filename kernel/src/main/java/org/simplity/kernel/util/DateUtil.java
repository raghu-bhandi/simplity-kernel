/*
 * Copyright (c) 2016 simplity.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.kernel.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * utilities for handling date
 *
 * @author simplity.org
 *
 */
public class DateUtil {
	/**
	 * server always uses this format for date
	 */
	public static final String SERVER_DATE_FORMAT = "yyyy-MM-dd";

	private static final int MILLISECS_PER_DAY = 24 * 60 * 60 * 1000;
	private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(
			SERVER_DATE_FORMAT);
	private static final int UTC_LENGTH = 24;
	private static final SimpleDateFormat UTC_FORMATTER = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	static {
		DATE_FORMATTER.setLenient(false);
		DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
		UTC_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/**
	 * number of days between two dates. time part is ignored from both dates.
	 * If you want to consider time as well, then use elapsedDaysBetweenDates()
	 *
	 * @param toDate
	 *            note that this is long - date in milliseconds (date.getTime())
	 * @param fromDate
	 *            note that this is long - date in milliseconds (date.getTime())
	 * @return number of days as counted from date to date, and not based on
	 *         time.
	 */
	public static int daysBetweenDates(long toDate, long fromDate) {
		long toDays = toDate / DateUtil.MILLISECS_PER_DAY;
		long fromDays = fromDate / DateUtil.MILLISECS_PER_DAY;
		return (int) (toDays - fromDays);
	}

	/**
	 * number of days the supplied date is from today in the future. same as
	 * date - today.
	 *
	 * @param toDate
	 *            note that this is long - date in milliseconds (date.getTime())
	 * @return number of days as counted from today forward into toDate. If
	 *         toDate is the past, returned value is -ve.
	 */
	public static int daysFromToday(long toDate) {
		return DateUtil.daysBetweenDates(toDate, Calendar.getInstance()
				.getTimeInMillis());
	}

	/**
	 * number of elapsed days (completed 24 hours) between two specific time
	 * intervals.
	 *
	 * @param toTime
	 *            note that this is long - date in milliseconds (date.getTime())
	 * @param fromTime
	 *            note that this is long - date in milliseconds (date.getTime())
	 * @return number of days counted as 1 for every 24 hours from startDate =
	 *         toDate - fromDate
	 */
	public static int elapsedDaysBetweenDates(long toTime, long fromTime) {
		return (int) (toTime - fromTime) / DateUtil.MILLISECS_PER_DAY;
	}

	/**
	 * number of elapsed days (completed 24 hours) between two specific time
	 * intervals.
	 *
	 * @param toDate
	 * @param fromDate
	 * @return number of days counted as 1 for every 24 hours from startDate =
	 *         toDate - fromDate
	 */
	public static int elapsedDaysBetweenDates(Date toDate, Date fromDate) {
		return (int) (toDate.getTime() - fromDate.getTime())
				/ DateUtil.MILLISECS_PER_DAY;
	}

	/**
	 * add number of days to a date
	 *
	 * @param date
	 * @param days
	 * @return date offset date
	 */
	public static long addDays(long date, long days) {
		return date + days * DateUtil.MILLISECS_PER_DAY;
	}

	/**
	 * add number of days to a date
	 *
	 * @param date
	 * @param days
	 * @return date offset date
	 */
	public static Date addDays(Date date, int days) {
		return new Date(date.getTime() + days * DateUtil.MILLISECS_PER_DAY);
	}

	/**
	 * @param date
	 * @return date with its time component set to 0
	 */
	public static long trimTime(long date) {
		long time = date % DateUtil.MILLISECS_PER_DAY;
		return date - time;
	}

	/**
	 * @return today's date with time as 00:00:00.000
	 */
	public static Date getToday() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
		/*
		 * is this better??? return trimTime(cal.getTime());
		 */
	}

	/**
	 * @param value
	 * @return date or null if it is not a valid date
	 */
	public static Date parseYmd(String value) {
		try {
			return DateUtil.DATE_FORMATTER.parse(value);
		} catch (Exception ignore) {
			return null;
		}
	}

	/**
	 * format date into text
	 *
	 * @param date
	 * @return text
	 */
	public static String format(Date date) {
		return DATE_FORMATTER.format(date);
	}

	/**
	 *
	 * @param date
	 * @return UTC formatted date-time
	 */
	public static String toUtc(Date date) {
		return UTC_FORMATTER.format(date);
	}

	/**
	 * @param utc
	 *            date string in UTC format
	 * @return parsed date, null if the string is not a valid date/time
	 *
	 */
	public static Date parseUtc(String utc) {
		if (utc.length() == UTC_LENGTH) {
			try {
				return UTC_FORMATTER.parse(utc);
			} catch (Exception ignore) {
				//
			}
		}
		// Tracer.trace("we encountered an invalid date " + utc);
		return null;
	}
}
