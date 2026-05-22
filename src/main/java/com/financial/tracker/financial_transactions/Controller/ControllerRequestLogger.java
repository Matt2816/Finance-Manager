package com.financial.tracker.financial_transactions.Controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class ControllerRequestLogger {

    private ControllerRequestLogger() {}

    public static void logIncoming(Logger log, String operation) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.info("Incoming request: {}", operation);
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        log.info(
                "Incoming request: {} {} {} from {}",
                operation,
                request.getMethod(),
                request.getRequestURI(),
                HealthController.clientAddress(request)
        );
    }
}
