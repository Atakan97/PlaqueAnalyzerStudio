package com.project.plaque.plaque_calculator.util;


// Utility class for calculating star ratings based on normalization performance.
public class StarRatingUtil {

	// Time thresholds in seconds (5 min, 10 min, 15 min, 20 min)
	private static final int FIVE_MINUTES = 300;
	private static final int TEN_MINUTES = 600;
	private static final int FIFTEEN_MINUTES = 900;
	private static final int TWENTY_MINUTES = 1200;

	/**
	 * Table for star ratings.
	 * Rows represent attempt numbers (1st, 2nd, 3rd, 4th+)
	 * Columns represent time ranges: <5min, 5-10min, 10-15min, 15-20min, >20min
	 */
	private static final int[][] RATING_TABLE = {
		// Time:    <5min  5-10min  10-15min  15-20min  >20min
		/* 1st  */ {  5,     4,       3,        2,        1  },
		/* 2nd  */ {  4,     3,       2,        1,        1  },
		/* 3rd  */ {  3,     2,       1,        1,        1  },
		/* 4th+ */ {  2,     1,       1,        1,        1  }
	};

	// Calculates the star rating based on attempts and elapsed time.
	public static int calculateStarRating(int attempts, long elapsedSeconds) {
		// Determine the row index based on attempts (capped at 4th+ row)
		int attemptIndex = Math.min(attempts - 1, 3);

		// Ensure attempt index is not negative
		if (attemptIndex < 0) {
			attemptIndex = 0;
		}

		// Determine the column index based on elapsed time
		int timeIndex = getTimeIndex(elapsedSeconds);
		return RATING_TABLE[attemptIndex][timeIndex];
	}

	// Determines the time range index for the table.
	private static int getTimeIndex(long elapsedSeconds) {
		if (elapsedSeconds < FIVE_MINUTES) {
			return 0;  // Under 5 minutes
		} else if (elapsedSeconds < TEN_MINUTES) {
			return 1;  // 5 to 10 minutes
		} else if (elapsedSeconds < FIFTEEN_MINUTES) {
			return 2;  // 10 to 15 minutes
		} else if (elapsedSeconds < TWENTY_MINUTES) {
			return 3;  // 15 to 20 minutes
		} else {
			return 4;  // Over 20 minutes
		}
	}
}
