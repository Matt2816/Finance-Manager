package com.financial.tracker.financial_transactions.Controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class ControllerRequestLogger {

    private static final int MAX_LOG_LENGTH = 4000;

    private ControllerRequestLogger() {}

    public static void logIncoming(Logger log, String operation) {
        logIncoming(log, operation, new Object[0]);
    }

    /**
     * Logs an incoming request. Pass a single value for the body, or key/value pairs
     * (e.g. {@code "id", id, "body", requestBody}).
     */
    public static void logIncoming(Logger log, String operation, Object... context) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        String contextSummary = formatContext(context);
        if (attributes == null) {
            log.info("Incoming request: {}{}", operation, contextSummary);
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        log.info(
                "Incoming request: {} {} {}{} from {}",
                operation,
                request.getMethod(),
                request.getRequestURI(),
                contextSummary,
                HealthController.clientAddress(request)
        );
    }

    public static <T> ResponseEntity<T> logResponse(Logger log, String operation, ResponseEntity<T> response) {
        log.info(
                "Response {}: status={}, body={}",
                operation,
                response.getStatusCode(),
                summarize(response.getBody())
        );
        return response;
    }

    public static <T> T logResponseBody(Logger log, String operation, T body) {
        log.info("Response {}: body={}", operation, summarize(body));
        return body;
    }

    private static String formatContext(Object... context) {
        if (context == null || context.length == 0) {
            return "";
        }
        if (context.length == 1) {
            return " body=" + summarize(context[0]);
        }

        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < context.length; i += 2) {
            Object key = context[i];
            Object value = i + 1 < context.length ? context[i + 1] : null;
            formatted.append(' ').append(key).append('=').append(summarize(value));
        }
        return formatted.toString();
    }

    static String summarize(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof MultipartFile file) {
            return "MultipartFile(name="
                    + file.getOriginalFilename()
                    + ", size="
                    + file.getSize()
                    + ", contentType="
                    + file.getContentType()
                    + ")";
        }
        if (value instanceof byte[] bytes) {
            return "byte[" + bytes.length + "]";
        }
        if (value instanceof Collection<?> collection) {
            return collection.getClass().getSimpleName() + "(size=" + collection.size() + ")";
        }
        if (value instanceof Map<?, ?> map) {
            return map.getClass().getSimpleName() + "(size=" + map.size() + ")";
        }
        if (value instanceof List<?> list) {
            if (list.size() > 20) {
                return "List(size=" + list.size() + ")";
            }
        }

        String text = String.valueOf(value);
        if (text.length() <= MAX_LOG_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_LOG_LENGTH)
                + "... [truncated, "
                + text.length()
                + " chars]";
    }
}
