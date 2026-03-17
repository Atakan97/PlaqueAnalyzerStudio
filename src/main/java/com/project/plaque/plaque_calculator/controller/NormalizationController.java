package com.project.plaque.plaque_calculator.controller;

import com.google.gson.Gson;
import com.project.plaque.plaque_calculator.service.LogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import java.util.*;

@Controller
@RequestMapping("/normalize")
public class NormalizationController {

	private final Gson gson = new Gson();
	private final LogService logService;

	// The key to keeping the past in Session
	private static final String HISTORY_SESSION_KEY = "normalizationHistory";
	private static final String START_TIME_SESSION_KEY = "normalizationSessionStart";
	private static final String BCNF_SUMMARY_SESSION_KEY = "bcnfSummary";
	private static final String RESTORE_SESSION_KEY = "normalizationRestoreState";
	private static final String RESET_SESSION_KEY = "normalizationReset";
	private static final String DECOMPOSED_RESTORE_SESSION_KEY = "decomposedRestoreState";

	@Autowired
	public NormalizationController(LogService logService) {
		this.logService = logService;
	}

	private String getSessionKeyPrefix(String computationId) {
		if (computationId == null || computationId.isBlank() || "null".equals(computationId)) {
			return "";
		}
		return "computation_" + computationId + "_";
	}

	@PostMapping("/continue")
	public String continueNormalization(@RequestBody Map<String, Object> body, HttpSession session) {
		logService.info("[NormalizationController] /continue invoked with payload keys=" + body.keySet());
		String computationId = body != null && body.get("computationId") != null ? String.valueOf(body.get("computationId")) : null;
		String prefix = getSessionKeyPrefix(computationId);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> history = (List<Map<String, Object>>) session.getAttribute(prefix + HISTORY_SESSION_KEY);
		if (history == null) {
			history = new ArrayList<>();
		}
		// Append current state (body) to the end of history
		history.add(body);
		logService.info("[NormalizationController] history size after append=" + history.size());
		session.setAttribute(prefix + HISTORY_SESSION_KEY, history);
		session.removeAttribute(prefix + RESTORE_SESSION_KEY);

		// Reset attempts counter after a successful advance
		session.removeAttribute(prefix + "normalizeAttempts");

		session.removeAttribute(prefix + RESET_SESSION_KEY);
		// Redirect to normalization page
		return computationId == null || computationId.isBlank()
			? "redirect:/normalization"
			: ("redirect:/normalization?id=" + computationId);
	}

	public Long setAndGetNormalizationStartTime(HttpSession session, String computationId) {
		String prefix = getSessionKeyPrefix(computationId);
		Long startTime = (Long) session.getAttribute(prefix + START_TIME_SESSION_KEY);
		if (startTime == null) {
			startTime = System.currentTimeMillis();
			session.setAttribute(prefix + START_TIME_SESSION_KEY, startTime);
		}
		return startTime;
	}

	// Adding new API method
	@PostMapping("/log-success")
	public ResponseEntity<?> logBcnfSuccess(@RequestParam("userName") String userName,
											@RequestParam("attempts") int attempts,
											@RequestParam("elapsedTime") long elapsedTime,
											@RequestParam(value = "computationId", required = false) String computationId,
											HttpSession session) {
		try {
			String prefix = getSessionKeyPrefix(computationId);
			String plaqueMode = (String) session.getAttribute(prefix + "plaqueMode");
			if (plaqueMode == null) {
				plaqueMode = (String) session.getAttribute("plaqueMode");
			}

			// Read FD data from session (stored by DecomposeController when BCNF is achieved)
			String originalFds = (String) session.getAttribute(prefix + "bcnfOriginalFds");
			String decomposedTablesFds = (String) session.getAttribute(prefix + "bcnfDecomposedTablesFds");

			// Legacy fallback for old non-prefixed sessions
			if (originalFds == null) {
				originalFds = (String) session.getAttribute("bcnfOriginalFds");
			}
			if (decomposedTablesFds == null) {
				decomposedTablesFds = (String) session.getAttribute("bcnfDecomposedTablesFds");
			}

			// Log the success and get the calculated star rating
			int starRating = logService.logBcnfSuccess(userName, attempts, elapsedTime,
				plaqueMode, originalFds, decomposedTablesFds);

			// Clean up FD session attributes after saving
			session.removeAttribute(prefix + "bcnfOriginalFds");
			session.removeAttribute(prefix + "bcnfDecomposedTablesFds");

			// Return the star rating to the frontend
			return ResponseEntity.ok().body(java.util.Map.of("starRating", starRating));
		} catch (Exception e) {
			System.err.println("Error logging BCNF success: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to log success.");
		}
	}

	@PostMapping("/bcnf-review")
	public ResponseEntity<?> storeBcnfReview(@RequestBody Map<String, Object> body, HttpSession session) {
		if (body == null || body.isEmpty()) {
			return ResponseEntity.badRequest().body("BCNF data payload is empty.");
		}
		Map<String, Object> summaryData = new HashMap<>(body);

		// Extract and store computationId
		String computationId = body.get("computationId") != null ? String.valueOf(body.get("computationId")) : null;
		if (computationId != null && !computationId.isEmpty() && !"null".equals(computationId)) {
			summaryData.put("computationId", computationId);
		}

		// Save the current normalization step for returning to it later
		String prefix = getSessionKeyPrefix(computationId);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> history = (List<Map<String, Object>>) session.getAttribute(prefix + HISTORY_SESSION_KEY);
		if (history == null) {
			history = new ArrayList<>();
		}
		Map<String, Object> currentSnapshot = buildNormalizationSnapshot(body);
		Map<String, Object> lastSnapshot = history.isEmpty()
			? null
			: buildNormalizationSnapshot(history.get(history.size() - 1));
		if (lastSnapshot == null || !lastSnapshot.equals(currentSnapshot)) {
			history.add(currentSnapshot);
			session.setAttribute(prefix + HISTORY_SESSION_KEY, history);
		}

		Number attemptsFromSession = (Number) session.getAttribute(prefix + "bcnfAttempts");
		if (attemptsFromSession == null) {
			attemptsFromSession = (Number) session.getAttribute("bcnfAttempts");
		}
		logService.info("[NormalizationController] bcnf-review: bcnfAttempts from session = " + attemptsFromSession);
		if (attemptsFromSession != null) {
			summaryData.put("attempts", attemptsFromSession.intValue());
		}

		Number elapsedFromSession = (Number) session.getAttribute(prefix + "bcnfElapsedTime");
		if (elapsedFromSession == null) {
			elapsedFromSession = (Number) session.getAttribute("bcnfElapsedTime");
		}
		logService.info("[NormalizationController] bcnf-review: bcnfElapsedTime from session = " + elapsedFromSession);
		if (elapsedFromSession != null) {
			summaryData.put("elapsedTime", elapsedFromSession.longValue());
		}

		session.setAttribute(prefix + BCNF_SUMMARY_SESSION_KEY, summaryData);

		// Clean up normalization-related prefixed attributes for this computation.
		session.removeAttribute(prefix + "bcnfAttempts");
		session.removeAttribute(prefix + "bcnfElapsedTime");
		session.removeAttribute(prefix + START_TIME_SESSION_KEY);
		session.removeAttribute(prefix + "attemptCount");
		session.removeAttribute(prefix + "normalizationStartTime");

		Map<String, Object> response = new HashMap<>();
		response.put("redirectUrl", computationId == null || computationId.isBlank() ? "/normalize/bcnf-summary" : ("/normalize/bcnf-summary?id=" + computationId));
		return ResponseEntity.ok(response);
	}

	@GetMapping("/bcnf-summary")
	public String showBcnfSummary(@RequestParam(value = "id", required = false) String computationId,
									HttpSession session,
									Model model) {
		String prefix = getSessionKeyPrefix(computationId);
		@SuppressWarnings("unchecked")
		Map<String, Object> summary = (Map<String, Object>) session.getAttribute(prefix + BCNF_SUMMARY_SESSION_KEY);
		if (summary == null) {
			// Legacy fallback for old non-prefixed sessions
			summary = (Map<String, Object>) session.getAttribute(BCNF_SUMMARY_SESSION_KEY);
		}
		if (summary == null) {
			return computationId == null || computationId.isBlank()
				? "redirect:/normalization"
				: ("redirect:/normalization?id=" + computationId);
		}

		model.addAttribute("bcnfSummaryJson", gson.toJson(summary));

		Number attemptsNumber = extractNumber(summary.get("attempts"));
		Number elapsedNumber = extractNumber(summary.get("elapsedTime"));

		model.addAttribute("bcnfAttempts", attemptsNumber != null ? attemptsNumber.intValue() : 0);
		model.addAttribute("bcnfElapsedSeconds", elapsedNumber != null ? elapsedNumber.longValue() : 0L);

		String resolvedComputationId = computationId;
		if (resolvedComputationId == null || resolvedComputationId.isBlank()) {
			Object computationIdFromSummary = summary.get("computationId");
			resolvedComputationId = computationIdFromSummary != null ? String.valueOf(computationIdFromSummary) : null;
		}
		model.addAttribute("computationId", resolvedComputationId);

		// Add plaqueMode to model
		String plaqueMode = (String) session.getAttribute(getSessionKeyPrefix(resolvedComputationId) + "plaqueMode");
		if (plaqueMode == null) {
			plaqueMode = (String) session.getAttribute("plaqueMode");
		}
		model.addAttribute("plaqueMode", plaqueMode != null ? plaqueMode : "enabled");

		return "bcnf-summary";
	}

	private Number extractNumber(Object value) {
		if (value instanceof Number number) {
			return number;
		}
		if (value instanceof String str) {
			try {
				if (str.contains(".")) {
					return Double.parseDouble(str);
				}
				return Long.parseLong(str);
			} catch (NumberFormatException ignored) {
			}
		}
		return null;
	}

	// Keep only the fields needed to restore a normalization step.
	private Map<String, Object> buildNormalizationSnapshot(Map<String, Object> source) {
		Map<String, Object> snapshot = new HashMap<>();
		if (source == null) {
			return snapshot;
		}
		snapshot.put("columnsPerTable", source.get("columnsPerTable"));
		snapshot.put("manualPerTable", source.get("manualPerTable"));
		snapshot.put("fdsPerTable", source.get("fdsPerTable"));
		snapshot.put("fdsPerTableOriginal", source.get("fdsPerTableOriginal"));
		snapshot.put("normalFormsPerTable", source.get("normalFormsPerTable"));
		snapshot.put("transitiveFdsPerTable", source.get("transitiveFdsPerTable"));
		snapshot.put("ricPerTable", source.get("ricPerTable"));
		snapshot.put("globalRic", source.get("globalRic"));
		snapshot.put("unionCols", source.get("unionCols"));
		snapshot.put("originalTable", source.get("originalTable"));
		snapshot.put("originalRic", source.get("originalRic"));
		return snapshot;
	}
	@PostMapping("/increment-attempt")
	public ResponseEntity<?> incrementAttempt(
			@RequestParam(value = "computationId", required = false) String computationId,
			HttpSession session) {
		// Build the prefix based on computationId
		String prefix = getSessionKeyPrefix(computationId);

		// Use prefixed key for attempt count
		Integer attemptCount = (Integer) session.getAttribute(prefix + "attemptCount");
		if (attemptCount == null) {
			attemptCount = 1;
		}
		attemptCount++;
		session.setAttribute(prefix + "attemptCount", attemptCount);
		logService.info("[NormalizationController] Attempt count incremented to: " + attemptCount + " (prefix=" + prefix + ")");
		return ResponseEntity.ok().build();
	}

	@GetMapping("/previous")
	public String returnToPrevious(@RequestParam(value = "id", required = false) String computationId, HttpSession session) {
		String prefix = getSessionKeyPrefix(computationId);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> history = (List<Map<String, Object>>) session.getAttribute(prefix + HISTORY_SESSION_KEY);
		logService.info("[NormalizationController] /previous invoked. history size=" + (history == null ? 0 : history.size()));
		if (history == null || history.isEmpty()) {
			session.setAttribute(prefix + RESET_SESSION_KEY, Boolean.TRUE);
			logService.info("[NormalizationController] history empty. Trigger reset state.");
			return computationId == null || computationId.isBlank()
				? "redirect:/normalization"
				: ("redirect:/normalization?id=" + computationId);
		}

		List<Map<String, Object>> updatedHistory = new ArrayList<>(history);
		Map<String, Object> poppedState = updatedHistory.remove(updatedHistory.size() - 1);
		logService.info("[NormalizationController] popped state. new history size=" + updatedHistory.size());
		session.setAttribute(prefix + HISTORY_SESSION_KEY, updatedHistory);

		// Clear restore flags so we can rely on the updated history for rendering.
		clearRestoreState(session, prefix);

		if (updatedHistory.isEmpty()) {
			// Save the popped state so that the page can
			// restore the decomposed tables the user had created in the previous step
			// instead of showing a blank initial view
			logService.info("[NormalizationController] history exhausted after pop; saving decomposed restore state.");
			session.setAttribute(prefix + DECOMPOSED_RESTORE_SESSION_KEY, buildNormalizationSnapshot(poppedState));
		}
		return computationId == null || computationId.isBlank()
			? "redirect:/normalization"
			: ("redirect:/normalization?id=" + computationId);
	}

	private void clearRestoreState(HttpSession session, String prefix) {
		session.removeAttribute(prefix + "usingDecomposedAsOriginal");
		session.removeAttribute(prefix + RESTORE_SESSION_KEY);
	}

	private void clearRestoreState(HttpSession session) {
		clearRestoreState(session, "");
	}
}

