package com.project.plaque.plaque_calculator.service;

import com.project.plaque.plaque_calculator.model.LogEntry;
import com.project.plaque.plaque_calculator.repository.LogRepository;
import com.project.plaque.plaque_calculator.util.StarRatingUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LogService {

	@Autowired
	private LogRepository logRepository;

	/**
	 * Logs a successful BCNF normalization process and calculates a star rating.
	 * The star rating is computed based on the number of attempts and elapsed time.
	 * @return The calculated star rating (1-5 stars)
	 */
	public int logBcnfSuccess(String userName, int attempts, long elapsedTimeSecs,
							 Integer tableCount, Boolean dependencyPreserved, String plaqueMode) {
		LogEntry logEntry = new LogEntry();
		logEntry.setUserName(userName);
		logEntry.setAttempts(attempts > 0 ? attempts : 1);
		logEntry.setElapsedTimeSecs(elapsedTimeSecs);
		logEntry.setTimestamp(LocalDateTime.now());
		logEntry.setActivityType("BCNF_SUCCESS");

		// Calculate star rating based on attempts and elapsed time
		int starRating = StarRatingUtil.calculateStarRating(
			attempts > 0 ? attempts : 1,
			elapsedTimeSecs
		);
		logEntry.setStarRating(starRating);

		// Store plaque mode directly (enabled/disabled)
		logEntry.setPlaqueMode(plaqueMode != null ? plaqueMode : "enabled");

		// Save log entry to database
		logRepository.save(logEntry);

		// Console output for debugging and monitoring
		System.out.println("Logged BCNF Success: User=" + userName
			+ ", Attempts=" + attempts
			+ ", Time=" + elapsedTimeSecs + "s"
			+ ", Rating=" + starRating + " stars"
			+ ", Tables=" + (tableCount != null ? tableCount : 0)
			+ ", DP=" + (dependencyPreserved != null && dependencyPreserved)
			+ ", Mode=" + plaqueMode);

		return starRating;
	}

	/**
	 * Logs an informational message to the console.
	 */
	public void info(String message) {
		System.out.println(message);
	}
}