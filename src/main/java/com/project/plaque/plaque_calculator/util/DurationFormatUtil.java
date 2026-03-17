package com.project.plaque.plaque_calculator.util;

/**
 * Utility class for formatting elapsed time (seconds) into a readable string
 */
public class DurationFormatUtil {

	private static final long SECONDS_PER_MINUTE = 60;
	private static final long SECONDS_PER_HOUR = 3600;

	private DurationFormatUtil() {
	}

	/**
	 * Formats a duration given in seconds into a readable string
	 * Returns "-" if the value is null
	 */
	public static String formatSeconds(Long totalSeconds) {
		if (totalSeconds == null) {
			return "-";
		}

		long seconds = Math.abs(totalSeconds);

		if (seconds < SECONDS_PER_MINUTE) {
			// Under 1 minute: show only seconds (e.g. "45s")
			return seconds + "s";
		}

		if (seconds < SECONDS_PER_HOUR) {
			// Under 1 hour: show minutes and seconds (e.g. "3m 13s")
			long minutes = seconds / SECONDS_PER_MINUTE;
			long remainingSeconds = seconds % SECONDS_PER_MINUTE;
			return minutes + "m " + remainingSeconds + "s";
		}

		// 1 hour or more: show hours, minutes and seconds (e.g. "1h 1m 1s")
		long hours = seconds / SECONDS_PER_HOUR;
		long remaining = seconds % SECONDS_PER_HOUR;
		long minutes = remaining / SECONDS_PER_MINUTE;
		long remainingSeconds = remaining % SECONDS_PER_MINUTE;
		return hours + "h " + minutes + "m " + remainingSeconds + "s";
	}

	/**
	 * Formats an average duration into a readable string
	 * Returns null if the value is negative (meaning no data available).
	 */
	public static String formatAverage(double avgSeconds) {
		if (avgSeconds < 0) {
			// Negative value means there is no data for this mode
			return null;
		}

		// Round to the nearest second before formatting
		long rounded = Math.round(avgSeconds);
		return formatSeconds(rounded);
	}
}

