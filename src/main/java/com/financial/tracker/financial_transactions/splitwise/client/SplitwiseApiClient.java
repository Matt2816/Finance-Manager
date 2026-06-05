package com.financial.tracker.financial_transactions.splitwise.client;

import com.financial.tracker.financial_transactions.splitwise.client.dto.SplitwiseCurrentUserResponse;
import com.financial.tracker.financial_transactions.splitwise.client.dto.SplitwiseExpenseDto;
import com.financial.tracker.financial_transactions.splitwise.client.dto.SplitwiseExpensesResponse;
import com.financial.tracker.financial_transactions.splitwise.client.dto.SplitwiseGroupDto;
import com.financial.tracker.financial_transactions.splitwise.client.dto.SplitwiseGroupsResponse;
import com.financial.tracker.financial_transactions.splitwise.client.dto.SplitwiseUserDto;
import com.financial.tracker.financial_transactions.splitwise.config.SplitwiseProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SplitwiseApiClient {

    private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;

    private final SplitwiseProperties properties;

    public SplitwiseApiClient(SplitwiseProperties properties) {
        this.properties = properties;
    }

    public SplitwiseUserDto getCurrentUser(String apiKey) {
        SplitwiseCurrentUserResponse response = restClient(apiKey)
                .get()
                .uri("/get_current_user")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw apiException("get_current_user failed", res.getStatusCode().value());
                })
                .body(SplitwiseCurrentUserResponse.class);
        if (response == null || response.getUser() == null) {
            throw new SplitwiseApiException("get_current_user returned no user", 502);
        }
        return response.getUser();
    }

    public List<SplitwiseGroupDto> getGroups(String apiKey) {
        SplitwiseGroupsResponse response = executeWithRetry(apiKey, () -> restClient(apiKey)
                .get()
                .uri("/get_groups")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw apiException("get_groups failed", res.getStatusCode().value());
                })
                .body(SplitwiseGroupsResponse.class));
        return response == null ? List.of() : response.getGroups();
    }

    public List<SplitwiseExpenseDto> getExpenses(
            String apiKey,
            Long groupId,
            Instant updatedAfter,
            LocalDate datedAfter
    ) {
        int limit = properties.getPageSize();
        int offset = 0;
        Map<Long, SplitwiseExpenseDto> byId = new LinkedHashMap<>();

        while (true) {
            final int pageOffset = offset;
            SplitwiseExpensesResponse page = executeWithRetry(apiKey, () -> fetchExpensePage(
                    apiKey, groupId, updatedAfter, datedAfter, limit, pageOffset
            ));
            List<SplitwiseExpenseDto> expenses = page == null ? List.of() : page.getExpenses();
            if (expenses.isEmpty()) {
                break;
            }
            for (SplitwiseExpenseDto expense : expenses) {
                if (expense.getId() != null) {
                    byId.put(expense.getId(), expense);
                }
            }
            if (expenses.size() < limit) {
                break;
            }
            offset += limit;
        }

        return new ArrayList<>(byId.values());
    }

    private SplitwiseExpensesResponse fetchExpensePage(
            String apiKey,
            Long groupId,
            Instant updatedAfter,
            LocalDate datedAfter,
            int limit,
            int offset
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/get_expenses")
                .queryParam("limit", limit)
                .queryParam("offset", offset);
        if (groupId != null) {
            builder.queryParam("group_id", groupId);
        }
        if (updatedAfter != null) {
            builder.queryParam("updated_after", ISO_INSTANT.format(updatedAfter));
        }
        if (datedAfter != null) {
            builder.queryParam("dated_after", datedAfter.toString());
        }

        return restClient(apiKey)
                .get()
                .uri(builder.build().toUriString())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw apiException("get_expenses failed", res.getStatusCode().value());
                })
                .body(SplitwiseExpensesResponse.class);
    }

    private RestClient restClient(String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getApi().getConnectTimeoutMs());
        factory.setReadTimeout(properties.getApi().getReadTimeoutMs());

        return RestClient.builder()
                .baseUrl(properties.getApi().getBaseUrl())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    private <T> T executeWithRetry(String apiKey, RetryableRequest<T> request) {
        int maxRetries = properties.getApi().getMaxRetries();
        long backoffMs = 500;
        RuntimeException lastFailure = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return request.execute();
            } catch (SplitwiseApiException ex) {
                lastFailure = ex;
                if (ex.getStatusCode() == 429 && attempt < maxRetries) {
                    sleep(backoffMs);
                    backoffMs = Math.min(backoffMs * 2, 30_000);
                    continue;
                }
                if (ex.getStatusCode() >= 500 && attempt < maxRetries) {
                    sleep(backoffMs + (long) (Math.random() * 200));
                    backoffMs = Math.min(backoffMs * 2, 30_000);
                    continue;
                }
                throw ex;
            }
        }
        throw lastFailure == null
                ? new SplitwiseApiException("Splitwise request failed after retries", 502)
                : lastFailure;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SplitwiseApiException("Interrupted during Splitwise API backoff", 503, ex);
        }
    }

    private static SplitwiseApiException apiException(String message, int status) {
        return new SplitwiseApiException(message + " (HTTP " + status + ")", status);
    }

    @FunctionalInterface
    private interface RetryableRequest<T> {
        T execute();
    }
}
