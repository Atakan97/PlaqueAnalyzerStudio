package com.project.plaque.plaque_calculator.controller;

import com.project.plaque.plaque_calculator.model.LogEntry;
import com.project.plaque.plaque_calculator.repository.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.util.Comparator;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.project.plaque.plaque_calculator.util.DurationFormatUtil;

@Controller
@RequestMapping("/admin")
public class AdminController {

	private final LogRepository logRepository;

	// Default sorting (newest logs first).
	private static final String DEFAULT_SORT_OPTION = "timestamp_desc";

	// Inject Admin Credentials from application.properties
	@Value("${admin.username}")
	private String adminUsername;

	@Value("${admin.password}")
	private String adminPassword;

	@Autowired
	public AdminController(LogRepository logRepository) {
		this.logRepository = logRepository;
	}

	// Existing method to view logs (requires a successful login check)
	@GetMapping("/logs")
	@Transactional
	public String viewLogs(
			HttpSession session,
			Model model,
			// userNameFilter filter
			@RequestParam(value = "userNameFilter", required = false) String userNameFilter,
			// sortOption controls ordering on the Admin Panel
			@RequestParam(value = "sortOption", required = false) String sortOption
	) {
		// Simple security check
		if (session.getAttribute("isAdmin") == null) {
			return "redirect:/";
		}

		// Clean inputs (trim spaces and use a safe default for sort).
		String nameFilter = userNameFilter != null ? userNameFilter.trim() : "";
		String resolvedSortOption = resolveSortOption(sortOption);

		// Load logs and apply filtering/sorting in one place for predictable results.
		List<LogEntry> allLogs = logRepository.findAll();

		List<LogEntry> filteredLogs;

		if (nameFilter.isEmpty()) {
			// If there is no filter, show all logs
			filteredLogs = allLogs;
		} else {
			// If there is a filter, show only BCNF logs and those matching the userName
			filteredLogs = allLogs.stream()
					.filter(log ->
							log.getUserName() != null &&
									log.getUserName().toLowerCase().contains(nameFilter.toLowerCase())
					)
					.collect(Collectors.toList());
		}

		// Apply the selected sort option (rating/date asc/desc).
		filteredLogs.sort(buildLogComparator(resolvedSortOption));

		// Active user count (unique userName count)
		long activeUsers = filteredLogs.stream()
				.filter(log -> log.getUserName() != null && !log.getUserName().isEmpty())
				.map(LogEntry::getUserName)
				.distinct()
				.count();

		// Average completion time per mode (plaque enabled vs disabled)
		double avgDurationPlaque = calculateAvgDuration(filteredLogs, "enabled");
		double avgDurationNoPlaque = calculateAvgDuration(filteredLogs, "disabled");

		// Build a map of log ID -> formatted duration string (e.g. "3m 13s")
		Map<Long, String> durationMap = filteredLogs.stream()
				.collect(Collectors.toMap(
						LogEntry::getId,
						log -> DurationFormatUtil.formatSeconds(log.getElapsedTimeSecs())
				));

		// Add logs and filter value to the Model
		model.addAttribute("userNameFilter", nameFilter);
		// Keep the selected sort option so the UI can show it as selected
		model.addAttribute("sortOption", resolvedSortOption);
		// Adds logs to the model
		model.addAttribute("logs", filteredLogs);
		model.addAttribute("activeUsers", activeUsers);
		// Formatted duration for each log row
		model.addAttribute("durationMap", durationMap);

		// Formatted average durations; returns null when no data is available for a mode
		model.addAttribute("avgDurationPlaque",
				DurationFormatUtil.formatAverage(avgDurationPlaque));
		model.addAttribute("avgDurationNoPlaque",
				DurationFormatUtil.formatAverage(avgDurationNoPlaque));


		return "admin-logs";
	}

	/**
	 * Converts the raw query parameter to a known value.
	 * This prevents invalid values and keeps the UI stable.
	 */
	private static String resolveSortOption(String sortOption) {
		if (sortOption == null || sortOption.isBlank()) {
			return DEFAULT_SORT_OPTION;
		}

		String normalized = sortOption.trim().toLowerCase(Locale.ROOT);
		return switch (normalized) {
			case "timestamp_asc", "timestamp_desc", "rating_asc", "rating_desc" -> normalized;
			default -> DEFAULT_SORT_OPTION;
		};
	}

	/**
	 * Calculates the average elapsed time for logs that the plaque mode ("enabled" or "disabled")
	 * Returns -1.0 if there are no matching logs, so the UI will show "--".
	 */
	private static double calculateAvgDuration(List<LogEntry> logs, String plaqueMode) {
		return logs.stream()
				.filter(log -> log.getElapsedTimeSecs() != null)
				.filter(log -> plaqueMode.equalsIgnoreCase(log.getPlaqueMode()))
				.mapToLong(LogEntry::getElapsedTimeSecs)
				.average()
				.orElse(-1.0);
	}

	/**
	 * Builds a comparator for the log list based on the selected sort option.
	 * We use null-safe comparators so logs with missing values are always shown last.
	 */
	private static Comparator<LogEntry> buildLogComparator(String sortOption) {
		Comparator<Integer> ratingAsc = Comparator.nullsLast(Comparator.naturalOrder());
		Comparator<Integer> ratingDesc = Comparator.nullsLast(Comparator.reverseOrder());
		Comparator<Long> idAsc = Comparator.nullsLast(Comparator.naturalOrder());
		Comparator<Long> idDesc = Comparator.nullsLast(Comparator.reverseOrder());
		Comparator<java.time.LocalDateTime> timeAsc = Comparator.nullsLast(Comparator.naturalOrder());
		Comparator<java.time.LocalDateTime> timeDesc = Comparator.nullsLast(Comparator.reverseOrder());

		return switch (sortOption) {
			case "timestamp_asc" -> Comparator
					.comparing(LogEntry::getTimestamp, timeAsc)
					.thenComparing(LogEntry::getId, idAsc);
			case "rating_desc" -> Comparator
					.comparing(LogEntry::getStarRating, ratingDesc)
					.thenComparing(LogEntry::getTimestamp, timeDesc)
					.thenComparing(LogEntry::getId, idDesc);
			case "rating_asc" -> Comparator
					.comparing(LogEntry::getStarRating, ratingAsc)
					.thenComparing(LogEntry::getTimestamp, timeDesc)
					.thenComparing(LogEntry::getId, idDesc);
			default -> Comparator
					.comparing(LogEntry::getTimestamp, timeDesc)
					.thenComparing(LogEntry::getId, idDesc);
		};
	}

	// Log Deletion
	@PostMapping("/delete/{id}")
	@Transactional // Deletion requires a transaction
	public String deleteLogEntry(
			@PathVariable Long id,
			@RequestParam(value = "userNameFilter", required = false) String userNameFilter,
			@RequestParam(value = "sortOption", required = false) String sortOption,
			HttpSession session,
			RedirectAttributes redirectAttributes
	) {

		// Simple Security Check (prevent unauthorized access)
		if (session.getAttribute("isAdmin") == null) {
			return "redirect:/";
		}

		try {
			// Delete the log entry by its ID
			logRepository.deleteById(id);
		} catch (Exception e) {
			// Log the error but continue to redirect
			System.err.println("Error deleting log entry with ID " + id + ": " + e.getMessage());
		}

		// Keep current filter/sort values after deletion (better admin experience).
		if (userNameFilter != null && !userNameFilter.isBlank()) {
			redirectAttributes.addAttribute("userNameFilter", userNameFilter.trim());
		}
		String resolvedSortOption = resolveSortOption(sortOption);
		redirectAttributes.addAttribute("sortOption", resolvedSortOption);

		// Redirect back to the logs page after deletion to show the updated list
		return "redirect:/admin/logs";
	}

	// Handle admin login form submission
	@PostMapping("/login")
	public String handleAdminLogin(
			@RequestParam("adminUsername") String username,
			@RequestParam("adminPassword") String password,
			HttpSession session,
			Model model) {

		// Authentication check
		if (adminUsername.equals(username) && adminPassword.equals(password)) {

			// Successful login, set a flag in the session
			session.setAttribute("isAdmin", true);

			// Redirect to the logs page
			return "redirect:/admin/logs";

		} else {
			// Failed login, add an error message to the model (to be displayed on index.html)
			model.addAttribute("adminError", "Invalid username or password.");
			// Return to the index page to show the error
			return "index";
		}
	}

	// Handle Admin Logout
	@GetMapping("/logout")
	public String adminLogout(HttpSession session) {

		// Remove the "isAdmin" flag from the session and invalidate the session
		session.removeAttribute("isAdmin");
		session.invalidate();

		// Redirect to the main index page (login screen)
		return "redirect:/";
	}
}
