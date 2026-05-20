package com.financial.tracker.financial_transactions.Controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public HealthResponse health(HttpServletRequest request) {
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
