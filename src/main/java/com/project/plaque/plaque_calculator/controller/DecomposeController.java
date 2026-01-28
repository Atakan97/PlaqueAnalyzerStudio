package com.project.plaque.plaque_calculator.controller;

import com.project.plaque.plaque_calculator.dto.DecomposeAllRequest;
import com.project.plaque.plaque_calculator.dto.DecomposeAllResponse;
import com.project.plaque.plaque_calculator.dto.DecomposeRequest;
import com.project.plaque.plaque_calculator.dto.DecomposeResponse;
import com.project.plaque.plaque_calculator.dto.DecomposeStreamInitResponse;
import com.project.plaque.plaque_calculator.service.DecomposeService;
import com.project.plaque.plaque_calculator.service.LogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/normalize")
public class DecomposeController {

	private final DecomposeService decomposeService;
	private final LogService logService;
	private static final String ATTEMPT_COUNT_SESSION_KEY = "attemptCount";
	private static final String NORMALIZATION_START_TIME_KEY = "normalizationStartTime";
	private static final String STREAM_REQUESTS_SESSION_KEY = "decomposeStreamRequests";
	// Session key used by NormalizationController for per-computation start time
	private static final String COMPUTATION_START_TIME_KEY = "normalizationSessionStart";

	public DecomposeController(DecomposeService decomposeService, LogService logService) {
		this.decomposeService = decomposeService;
		this.logService = logService;
	}

	@PostMapping("/decompose-stream/start")
	public ResponseEntity<?> startDecomposeStream(@RequestBody DecomposeAllRequest req, HttpSession session) {
		if (req == null || req.getTables() == null || req.getTables().isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", "No decomposed tables were provided."));
		}

		Map<String, DecomposeAllRequest> staged = getOrCreateStreamRequests(session);
		String token = UUID.randomUUID().toString();
		staged.put(token, req);
		session.setAttribute(STREAM_REQUESTS_SESSION_KEY, staged);

		return ResponseEntity.ok(new DecomposeStreamInitResponse(token));
	}

	@GetMapping(value = "/decompose-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter decomposeStream(@RequestParam("token") String token, HttpSession session) {
		SseEmitter emitter = new SseEmitter(0L);
		CompletableFuture.runAsync(() -> {
			DecomposeAllRequest req = consumeStagedRequest(session, token);
			if (req == null) {
				emitError(emitter, "Stream token is invalid or expired.");
				return;
			}
			streamDecomposition(req, session, emitter);
		});
		return emitter;
	}

	// POST /normalize/decompose
	@PostMapping("/decompose")
	public ResponseEntity<?> decompose(
			@RequestBody DecomposeRequest req,
			HttpSession session
	) {
		DecomposeResponse resp = decomposeService.decompose(req, session);
		return ResponseEntity.ok(resp);
	}

	// POST /normalize/project-fds
	@PostMapping("/project-fds")
	public ResponseEntity<?> projectFDs(
			@RequestBody DecomposeRequest req,
			HttpSession session
	) {
		try {
			DecomposeResponse resp = decomposeService.projectFDsOnly(req, session);
			return ResponseEntity.ok(resp);
		} catch (IllegalStateException ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
		} catch (Exception ex) {
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Server error"));
		}
	}

	// POST /normalize/decompose-all (processing multiple decomposed-tables)
	@PostMapping("/decompose-all")
	public ResponseEntity<?> decomposeAll(@RequestBody DecomposeAllRequest req, HttpSession session) {
		String computationId = req.getComputationId();
		recordNormalizationAttemptsAndStartTime(session, computationId);

		DecomposeAllResponse response = decomposeService.decomposeAll(req, session);

		storeBcnfDataIfComplete(session, response, computationId);

		return ResponseEntity.ok(response);
	}

	private void streamDecomposition(DecomposeAllRequest req, HttpSession session, SseEmitter emitter) {
		long overallStartNs = System.nanoTime();
		try {
			var tables = req.getTables();
			if (tables == null || tables.isEmpty()) {
				emitError(emitter, "No decomposed tables were provided.");
				return;
			}

			String computationId = req.getComputationId();
			recordNormalizationAttemptsAndStartTime(session, computationId);
			AtomicInteger index = new AtomicInteger(1);
			tables.forEach(table -> {
				// propagate computationId to per-table requests so DecomposeService can resolve session keys
				if (computationId != null && (table.getComputationId() == null || table.getComputationId().isBlank())) {
					table.setComputationId(computationId);
				}

				int current = index.getAndIncrement();
				String label = "Decomposed Table " + current;
				long startNs = System.nanoTime();
				try {
					emitProgress(emitter, label + ": Starting computations.");
					decomposeService.decomposeWithProgress(table, session, message -> emitProgress(emitter, message), label);
					long elapsedMs = Math.max(0, (System.nanoTime() - startNs) / 1_000_000);
					emitProgress(emitter, label + ": Completed in " + formatDuration(elapsedMs) + ".");
				} catch (Exception ex) {
					long elapsedMs = Math.max(0, (System.nanoTime() - startNs) / 1_000_000);
					String reason = ex.getMessage() == null ? "Computation failed." : ex.getMessage();
					emitProgress(emitter, label + ": " + reason + " (after " + formatDuration(elapsedMs) + ").");
				}
			});

			DecomposeAllResponse aggregate = decomposeService.decomposeAll(req, session);
			storeBcnfDataIfComplete(session, aggregate, computationId);
			emitComplete(emitter, aggregate);
		} catch (Exception ex) {
			emitError(emitter, ex.getMessage() == null ? "Normalization failed." : ex.getMessage());
		} finally {
			emitter.complete();
		}
	}

	private void emitProgress(SseEmitter emitter, String message) {
		try {
			emitter.send(SseEmitter.event().name("progress").data(Map.of("message", message)));
		} catch (IOException ignored) {
		}
	}

	private void emitError(SseEmitter emitter, String message) {
		try {
			emitter.send(SseEmitter.event().name("stream-error").data(Map.of("message", message)));
		} catch (IOException ignored) {
		}
	}

	private void emitComplete(SseEmitter emitter, DecomposeAllResponse payload) {
		try {
			emitter.send(SseEmitter.event().name("complete").data(Map.of(
				"status", "done",
				"payload", payload
			)));
		} catch (IOException ignored) {
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, DecomposeAllRequest> getOrCreateStreamRequests(HttpSession session) {
		Object attr = session.getAttribute(STREAM_REQUESTS_SESSION_KEY);
		if (attr instanceof Map<?, ?> existing) {
			return (Map<String, DecomposeAllRequest>) existing;
		}
		Map<String, DecomposeAllRequest> fresh = new ConcurrentHashMap<>();
		session.setAttribute(STREAM_REQUESTS_SESSION_KEY, fresh);
		return fresh;
	}

	private DecomposeAllRequest consumeStagedRequest(HttpSession session, String token) {
		if (token == null || token.isBlank()) {
			return null;
		}
		Map<String, DecomposeAllRequest> staged = getOrCreateStreamRequests(session);
		return staged.remove(token);
	}

	private String formatDuration(long elapsedMs) {
		if (elapsedMs < 1000) {
			return elapsedMs + " ms";
		}
		return String.format(Locale.US, "%.2f s", elapsedMs / 1000.0);
	}

	/**
	 * Resolves the session key prefix based on computationId.
	 * If computationId is provided, returns "computation_{id}_", otherwise returns empty string.
	 */
	private String getSessionKeyPrefix(String computationId) {
		if (computationId == null || computationId.isBlank()) {
			return "";
		}
		return "computation_" + computationId + "_";
	}

	private int recordNormalizationAttemptsAndStartTime(HttpSession session, String computationId) {
		String prefix = getSessionKeyPrefix(computationId);

		// Get current attempt count - if not set, initialize to 1
		Integer attemptCount = (Integer) session.getAttribute(prefix + ATTEMPT_COUNT_SESSION_KEY);
		if (attemptCount == null) {
			attemptCount = 1;
			session.setAttribute(prefix + ATTEMPT_COUNT_SESSION_KEY, attemptCount);
		}

		// Start time is set by NormalizationController when page loads
		// We use the same key to read it: "computation_{id}_normalizationSessionStart"
		Long sessionStart = (Long) session.getAttribute(prefix + COMPUTATION_START_TIME_KEY);
		if (sessionStart == null) {
			// If not set, use current time
			sessionStart = System.currentTimeMillis();
			session.setAttribute(prefix + COMPUTATION_START_TIME_KEY, sessionStart);
		}

		if (session.getAttribute(prefix + NORMALIZATION_START_TIME_KEY) == null) {
			session.setAttribute(prefix + NORMALIZATION_START_TIME_KEY, sessionStart);
		}

		return attemptCount;
	}

	private void storeBcnfDataIfComplete(HttpSession session, DecomposeAllResponse response, String computationId) {
		if (response == null || !response.isBCNFDecomposition()) {
			System.out.println("[DecomposeController] storeBcnfDataIfComplete: BCNF not yet achieved, skipping storage.");
			return;
		}

		System.out.println("[DecomposeController] storeBcnfDataIfComplete: BCNF achieved, storing data...");

		String prefix = getSessionKeyPrefix(computationId);

		// Read start time from the prefixed session key (set by NormalizationController)
		Long startTime = (Long) session.getAttribute(prefix + COMPUTATION_START_TIME_KEY);
		if (startTime == null) {
			startTime = (Long) session.getAttribute(prefix + NORMALIZATION_START_TIME_KEY);
		}
		// Fallback to non-prefixed keys for backwards compatibility
		if (startTime == null) {
			startTime = (Long) session.getAttribute(COMPUTATION_START_TIME_KEY);
		}
		if (startTime == null) {
			startTime = (Long) session.getAttribute(NORMALIZATION_START_TIME_KEY);
		}

		long elapsedSeconds = 0;
		if (startTime != null) {
			long currentTime = System.currentTimeMillis();
			elapsedSeconds = Math.max(0, (currentTime - startTime) / 1000);
		}

		// Read attempt count from the prefixed session key
		Integer attemptCount = (Integer) session.getAttribute(prefix + ATTEMPT_COUNT_SESSION_KEY);
		if (attemptCount == null) {
			// Fallback to non-prefixed key
			attemptCount = (Integer) session.getAttribute(ATTEMPT_COUNT_SESSION_KEY);
		}
		if (attemptCount == null) {
			attemptCount = 1;
		}


		// Store BCNF data in non-prefixed session keys (used by bcnf-review endpoint)
		session.setAttribute("bcnfAttempts", attemptCount);
		session.setAttribute("bcnfElapsedTime", elapsedSeconds);
		int tableCount = response.getTableResults() != null ? response.getTableResults().size() : 0;
		session.setAttribute("bcnfTableCount", tableCount);
		session.setAttribute("bcnfDependencyPreserved", response.isDpPreserved());

	}
}
