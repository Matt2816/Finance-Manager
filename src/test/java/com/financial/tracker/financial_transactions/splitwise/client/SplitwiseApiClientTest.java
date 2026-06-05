package com.financial.tracker.financial_transactions.splitwise.client;

import com.financial.tracker.financial_transactions.splitwise.client.dto.SplitwiseUserDto;
import com.financial.tracker.financial_transactions.splitwise.config.SplitwiseProperties;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import java.util.List;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SplitwiseApiClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private SplitwiseApiClient client;

    @BeforeEach
    void setUp() {
        SplitwiseProperties properties = new SplitwiseProperties();
        properties.getApi().setBaseUrl(wireMock.baseUrl());
        properties.setPageSize(2);
        client = new SplitwiseApiClient(properties);
    }

    @Test
    void getCurrentUserReturnsUser() {
        wireMock.stubFor(get(urlPathEqualTo("/get_current_user"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"user":{"id":123,"first_name":"Test","last_name":"User"}}
                                """)));

        SplitwiseUserDto user = client.getCurrentUser("test-key");

        assertEquals(123L, user.getId());
        wireMock.verify(getRequestedFor(urlPathEqualTo("/get_current_user"))
                .withHeader("Authorization", equalTo("Bearer test-key")));
    }

    @Test
    void getExpensesPaginatesUntilEmpty() {
        wireMock.stubFor(get(urlPathEqualTo("/get_expenses"))
                .withQueryParam("offset", equalTo("0"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"expenses":[{"id":1,"description":"A"},{"id":2,"description":"B"}]}
                                """)));
        wireMock.stubFor(get(urlPathEqualTo("/get_expenses"))
                .withQueryParam("offset", equalTo("2"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"expenses":[{"id":3,"description":"C"}]}
                                """)));

        var expenses = client.getExpenses("key", null, null, null);

        assertEquals(3, expenses.size());
        wireMock.verify(2, getRequestedFor(urlPathEqualTo("/get_expenses")));
    }

    @Test
    void getCurrentUserThrowsOnInvalidKey() {
        wireMock.stubFor(get(urlPathEqualTo("/get_current_user"))
                .willReturn(aResponse().withStatus(401)));

        assertThrows(SplitwiseApiException.class, () -> client.getCurrentUser("bad-key"));
    }
}
