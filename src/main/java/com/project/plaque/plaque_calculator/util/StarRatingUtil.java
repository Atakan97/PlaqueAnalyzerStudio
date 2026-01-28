package com.project.plaque.plaque_calculator.util;


// Utility class for calculating star ratings based on normalization performance.
public class StarRatingUtil {

	// Time thresholds in seconds (2 min, 4 min, 6 min, 8 min)
	private static final int TWO_MINUTES = 120;
	private static final int FOUR_MINUTES = 240;
	private static final int SIX_MINUTES = 360;
	private static final int EIGHT_MINUTES = 480;

	/**
	 * Table for star ratings.
	 * Rows represent attempt numbers
	 * Columns represent time ranges
	 */
	private static final int[][] RATING_TABLE = {
		// Time:    <2min  2-4min  4-6min  6-8min  >8min
		/* 1st */  {  5,     4,      3,      2,      1  },
		/* 2nd */  {  4,     3,      2,      1,      1  },
		/* 3rd */  {  3,     2,      1,      1,      1  },
		/* 4th */  {  2,     1,      1,      1,      1  },
		/* 5th+ */ {  1,     1,      1,      1,      1  }
	};

	// Calculates the star rating based on attempts and elapsed time.
	public static int calculateStarRating(int attempts, long elapsedSeconds) {
		// Determine the row index based on attempts
		int attemptIndex = Math.min(attempts - 1, 4);

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
		if (elapsedSeconds < TWO_MINUTES) {
			return 0;  // Under 2 minutes
		} else if (elapsedSeconds < FOUR_MINUTES) {
			return 1;  // 2 to 4 minutes
		} else if (elapsedSeconds < SIX_MINUTES) {
			return 2;  // 4 to 6 minutes
		} else if (elapsedSeconds < EIGHT_MINUTES) {
			return 3;  // 6 to 8 minutes
		} else {
			return 4;  // Over 8 minutes
		}
	}
}
