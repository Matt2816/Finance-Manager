package com.financial.tracker.financial_transactions.Controller;

import com.financial.tracker.financial_transactions.Controller.dto.AuthResponse;
import com.financial.tracker.financial_transactions.Controller.dto.LoginRequest;
import com.financial.tracker.financial_transactions.Controller.dto.RegisterRequest;
import com.financial.tracker.financial_transactions.Controller.dto.UserResponse;
import com.financial.tracker.financial_transactions.Services.AuthService;
import com.financial.tracker.financial_transactions.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        ControllerRequestLogger.logIncoming(log, "register", "request", request);
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        ControllerRequestLogger.logIncoming(log, "login", "request", request);
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        ControllerRequestLogger.logIncoming(log, "me", "user", user);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return authService.getCurrentUser(user);
    }
}
