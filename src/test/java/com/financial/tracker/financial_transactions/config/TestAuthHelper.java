package com.financial.tracker.financial_transactions.config;

import com.financial.tracker.financial_transactions.model.User;
import com.financial.tracker.financial_transactions.security.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public final class TestAuthHelper {

    private TestAuthHelper() {
    }

    public static void setAuthenticatedUser(long userId, String username) {
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setEmail(username + "@test.local");
        user.setPasswordHash("hash");

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        authenticatedUser.getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
}
