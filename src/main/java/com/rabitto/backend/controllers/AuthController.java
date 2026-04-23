package com.rabitto.backend.controllers;

import com.rabitto.backend.models.Auth;
import com.rabitto.backend.models.Tutor;
import com.rabitto.backend.repositories.AuthRepository;
import com.rabitto.backend.repositories.TutorRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String TOKEN_TYPE = "Bearer";

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private AuthRepository authRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.auth.jwt-secret:change-this-secret-key-with-at-least-32-characters}")
    private String jwtSecret;

    @Value("${app.auth.access-token-minutes:15}")
    private long accessTokenMinutes;

    @Value("${app.auth.refresh-token-days:7}")
    private long refreshTokenDays;

    @PostConstruct
    public void validateConfig() {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.auth.jwt-secret must have at least 32 bytes");
        }
    }

    @PostMapping("/login")
    @Transactional
    @CrossOrigin(origins = "*") // Permitir CORS para todos os domínios (ajuste conforme necessário)
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<Tutor> tutorOpt = tutorRepository.findByEmail(request.email());
        if (tutorOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Email ou senha invalidos"));
        }

        Tutor tutor = tutorOpt.get();
        if (!isPasswordValid(request.senha(), tutor.getSenha())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Email ou senha invalidos"));
        }

        authRepository.deleteByTutorId(tutor.getId());
        Auth refreshSession = buildRefreshSession(tutor);
        authRepository.save(refreshSession);

        TokenResponse tokenResponse = buildTokenResponse(tutor, refreshSession.getRefreshToken());
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<?> logout(@RequestBody RefreshRequest request) {
        Optional<Auth> tokenOpt = authRepository.findByRefreshTokenAndRevokedFalse(request.refreshToken());
        if (tokenOpt.isPresent()) {
            Auth token = tokenOpt.get();
            token.setRevoked(true);
            authRepository.save(token);
        }

        return ResponseEntity.ok(Map.of("message", "Logout realizado com sucesso"));
    }

    @PostMapping("/refresh")
    @Transactional
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        Optional<Auth> tokenOpt = authRepository.findByRefreshTokenAndRevokedFalse(request.refreshToken());
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Refresh token invalido"));
        }

        Auth currentToken = tokenOpt.get();
        if (currentToken.getExpiresAt().isBefore(Instant.now())) {
            currentToken.setRevoked(true);
            authRepository.save(currentToken);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Refresh token expirado"));
        }

        currentToken.setRevoked(true);
        authRepository.save(currentToken);

        Tutor tutor = currentToken.getTutor();
        Auth newRefreshSession = buildRefreshSession(tutor);
        authRepository.save(newRefreshSession);

        TokenResponse tokenResponse = buildTokenResponse(tutor, newRefreshSession.getRefreshToken());
        return ResponseEntity.ok(tokenResponse);
    }

    private boolean isPasswordValid(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }

        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        return rawPassword.equals(storedPassword);
    }

    private Auth buildRefreshSession(Tutor tutor) {
        Auth auth = new Auth();
        auth.setTutor(tutor);
        auth.setRevoked(false);
        auth.setRefreshToken(UUID.randomUUID() + "." + UUID.randomUUID());
        auth.setExpiresAt(Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS));
        return auth;
    }

    private TokenResponse buildTokenResponse(Tutor tutor, String refreshToken) {
        Instant now = Instant.now();
        Instant accessTokenExpiresAt = now.plus(accessTokenMinutes, ChronoUnit.MINUTES);

        String accessToken = Jwts.builder()
                .subject(tutor.getEmail())
                .claim("tutorId", tutor.getId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(accessTokenExpiresAt))
                .signWith(getSigningKey())
                .compact();

        long expiresInSeconds = ChronoUnit.SECONDS.between(now, accessTokenExpiresAt);
        return new TokenResponse(accessToken, refreshToken, TOKEN_TYPE, expiresInSeconds);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public record LoginRequest(String email, String senha) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
    }

}
