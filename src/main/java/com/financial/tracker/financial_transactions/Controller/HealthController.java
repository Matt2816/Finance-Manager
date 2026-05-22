package com.financial.tracker.financial_transactions.Controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @GetMapping("/health")
    public HealthResponse health(HttpServletRequest request) {
        ControllerRequestLogger.logIncoming(log, "health");
        return new HealthResponse(
                "ok",
                "Finance Manager API is running",
                Instant.now().toString(),
                clientAddress(request)
        );
    }

    public record HealthResponse(
            String status,
            String message,
            String timestamp,
            String clientAddress
    ) {}

    static String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
