package com.financial.tracker.financial_transactions.Controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.springframework.web.multipart.MultipartFile;

public final class ControllerRequestLogger {

    private ControllerRequestLogger() {}

    public static void logReceived(Logger log, String handler) {
        log.info("Incoming request [{}]", handler);
    }

    public static void logReceived(Logger log, String handler, Object payload) {
        log.info("Incoming request [{}]: payload={}", handler, payload);
    }

    public static void logReceived(Logger log, String handler, MultipartFile file) {
        if (file == null) {
            log.info("Incoming request [{}]: file=null", handler);
            return;
        }
        log.info(
                "Incoming request [{}]: fileName={}, size={}, contentType={}",
                handler,
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType()
        );
    }

    public static void logReceived(Logger log, String handler, HttpServletRequest request) {
        log.info(
                "Incoming request [{}]: method={}, path={}, query={}, client={}",
                handler,
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                HealthController.clientAddress(request)
        );
    }
}
