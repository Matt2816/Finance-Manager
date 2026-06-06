package com.financial.tracker.financial_transactions.Services;

import com.financial.tracker.financial_transactions.Controller.dto.AuthResponse;
import com.financial.tracker.financial_transactions.Controller.dto.LoginRequest;
import com.financial.tracker.financial_transactions.Controller.dto.RegisterRequest;
import com.financial.tracker.financial_transactions.Controller.dto.UserResponse;
import com.financial.tracker.financial_transactions.analytics.config.CategorySeedService;
import com.financial.tracker.financial_transactions.model.User;
import com.financial.tracker.financial_transactions.repo.UserRepository;
import com.financial.tracker.financial_transactions.security.AuthenticatedUser;
import com.financial.tracker.financial_transactions.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.ZonedDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CategorySeedService categorySeedService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            CategorySeedService categorySeedService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.categorySeedService = categorySeedService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCreatedAt(ZonedDateTime.now());
        user = userRepository.save(user);

        categorySeedService.seedForUser(user.getId());

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user);
        String token = jwtUtil.generateToken(authenticatedUser);
        return new AuthResponse(token, toUserResponse(user));
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
            String token = jwtUtil.generateToken(authenticatedUser);
            User user = userRepository.findByUsername(authenticatedUser.getUsername())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
            return new AuthResponse(token, toUserResponse(user));
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

    public UserResponse getCurrentUser(AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toUserResponse(user);
    }

    private static UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail());
    }
}
