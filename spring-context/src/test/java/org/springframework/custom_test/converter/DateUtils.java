package org.springframework.custom_test.converter;

import org.springframework.util.Assert;

import java.lang.ref.SoftReference;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Description: java类作用描述
 * @author: sxk
 * @CreateDate: 2021/1/4 11:23
 * @Version: 1.0
 */
public class DateUtils {

	public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

	public static final String PATTERN_RFC1036 = "EEE, dd-MMM-yy HH:mm:ss zzz";

	public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";


	private static final String[] DEFAULT_PATTERNS = new String[]{
			PATTERN_RFC1123,
			PATTERN_RFC1036,
			PATTERN_ASCTIME
	};

	private static final Date DEFAULT_TWO_DIGIT_YEAR_START;

	public static final TimeZone GMT = TimeZone.getTimeZone("GMT");

	static {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(GMT);
		calendar.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		DEFAULT_TWO_DIGIT_YEAR_START = calendar.getTime();
	}

	public static Date parseDate(final String dateValue) {
		return parseDate(dateValue, null, null);
	}

	public static Date parseDate(final String dateValue, final String[] dateFormats) {
		return parseDate(dateValue, dateFormats, null);
	}

	public static String formatDate(final Date date) {
		return formatDate(date, PATTERN_RFC1123);
	}

	public static String formatDate(final Date date, final String pattern) {
		Assert.notNull(date, "Date");
		Assert.notNull(pattern, "Pattern");
		final SimpleDateFormat formatter = DateFormatHolder.formatFor(pattern);
		return formatter.format(date);
	}

	public static Date parseDate(
			final String dateValue,
			final String[] dateFormats,
			final Date startDate) {
		Assert.notNull(dateValue, "Date value");
		final String[] localDateFormats = dateFormats != null ? dateFormats : DEFAULT_PATTERNS;
		final Date localStartDate = startDate != null ? startDate : DEFAULT_TWO_DIGIT_YEAR_START;
		String v = dateValue;
		// trim single quotes around date if present
		// see issue #5279
		if (v.length() > 1 && v.startsWith("'") && v.endsWith("'")) {
			v = v.substring(1, v.length() - 1);
		}

		for (final String dateFormat : localDateFormats) {
			final SimpleDateFormat dateParser = DateFormatHolder.formatFor(dateFormat);
			dateParser.set2DigitYearStart(localStartDate);
			final ParsePosition pos = new ParsePosition(0);
			final Date result = dateParser.parse(v, pos);
			if (pos.getIndex() != 0) {
				return result;
			}
		}
		return null;
	}

	final static class DateFormatHolder {

		private static final ThreadLocal<SoftReference<Map<String, SimpleDateFormat>>>
				THREADLOCAL_FORMATS = new ThreadLocal<SoftReference<Map<String, SimpleDateFormat>>>();

		/**
		 * creates a {@link SimpleDateFormat} for the requested format string.
		 *
		 * @param pattern a non-{@code null} format String according to
		 *                {@link SimpleDateFormat}. The format is not checked against
		 *                {@code null} since all paths go through
		 *                {@link DateUtils}.
		 * @return the requested format. This simple dateformat should not be used
		 * to {@link SimpleDateFormat#applyPattern(String) apply} to a
		 * different pattern.
		 */
		public static SimpleDateFormat formatFor(final String pattern) {
			final SoftReference<Map<String, SimpleDateFormat>> ref = THREADLOCAL_FORMATS.get();
			Map<String, SimpleDateFormat> formats = ref == null ? null : ref.get();
			if (formats == null) {
				formats = new HashMap<String, SimpleDateFormat>();
				THREADLOCAL_FORMATS.set(
						new SoftReference<Map<String, SimpleDateFormat>>(formats));
			}

			SimpleDateFormat format = formats.get(pattern);
			if (format == null) {
				format = new SimpleDateFormat(pattern, Locale.US);
				format.setTimeZone(TimeZone.getTimeZone("GMT"));
				formats.put(pattern, format);
			}

			return format;
		}

		public static void clearThreadLocal() {
			THREADLOCAL_FORMATS.remove();
		}

	}
}
