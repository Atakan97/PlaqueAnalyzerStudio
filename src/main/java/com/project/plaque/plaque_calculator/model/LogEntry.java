package com.project.plaque.plaque_calculator.model;

import lombok.Getter;
import jakarta.persistence.*;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class LogEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// User information
	private String userName;

	// Process information
	// Ex: BCNF_SUCCESS
	private String activityType;
	private LocalDateTime timestamp = LocalDateTime.now();

	// Process duration
	private Long elapsedTimeSecs;

	// Plaque mode status: "enabled" for with-plaque mode, "disabled" for no-plaque mode
	private String plaqueMode;

	// Number of attempts
	private Integer attempts;

	// Star rating based on performance (1 to 5 stars)
	private Integer starRating;
}