package com.rabitto.backend.controllers;

import com.rabitto.backend.models.Auth;
import com.rabitto.backend.models.Funcionario;
import com.rabitto.backend.models.Tutor;
import com.rabitto.backend.repositories.AuthRepository;
import com.rabitto.backend.repositories.FuncionarioRepository;
import com.rabitto.backend.repositories.TutorRepository;
import com.rabitto.backend.security.Roles;
import com.rabitto.backend.services.JwtService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private static final String TOKEN_TYPE = "Bearer";

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Value("${app.auth.jwt-secret:change-this-secret-key-with-at-least-32-characters}")
    private String jwtSecret;

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
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<Tutor> tutorOpt = tutorRepository.findByEmail(request.email());
        if (tutorOpt.isEmpty() || !isPasswordValid(request.senha(), tutorOpt.get().getSenha())) {
            log.warn("Login de tutor falhou: email={}", request.email());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Email ou senha invalidos"));
        }

        Tutor tutor = tutorOpt.get();
        authRepository.deleteByTutorId(tutor.getId());
        Auth refreshSession = buildRefreshSession(tutor);
        authRepository.save(refreshSession);

        String accessToken = jwtService.generateAccessToken(tutor.getId(), tutor.getEmail(), Roles.TUTOR);
        log.info("Login de tutor OK: tutorId={} email={}", tutor.getId(), tutor.getEmail());
        return ResponseEntity.ok(new TokenResponse(
                accessToken, refreshSession.getRefreshToken(), TOKEN_TYPE, jwtService.getAccessTokenSeconds()));
    }

    

    @PostMapping("/staff/login")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> staffLogin(@RequestBody LoginRequest request) {
        String identifier = request.email();
        Optional<Funcionario> funcOpt = funcionarioRepository.findByEmail(identifier);
        if (funcOpt.isEmpty()) {
            funcOpt = funcionarioRepository.findByCpf(identifier);
        }

        if (funcOpt.isEmpty() || !isPasswordValid(request.senha(), funcOpt.get().getSenha())) {
            log.warn("Login de staff falhou: identifier={}", identifier);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciais invalidas"));
        }
        if (!Boolean.TRUE.equals(funcOpt.get().getAtivo())) {
            log.warn("Login de staff rejeitado (conta inativa): funcionarioId={}", funcOpt.get().getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Conta inativa. Contate o administrador"));
        }

        Funcionario func = funcOpt.get();
        String role = normalizeRole(func.getCargo());
        String subject = func.getEmail() != null ? func.getEmail() : func.getCpf();
        String accessToken = jwtService.generateAccessToken(func.getId(), subject, role);

        log.info("Login de staff OK: funcionarioId={} role={}", func.getId(), role);
        return ResponseEntity.ok(new TokenResponse(
                accessToken, "", TOKEN_TYPE, jwtService.getAccessTokenSeconds()));
    }

    

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String header) {
        var claims = jwtService.parse(header);
        String role = claims.get("role", String.class);
        Long uid = claims.get("uid", Number.class).longValue();

        if (Roles.TUTOR.equals(role)) {
            Tutor t = tutorRepository.findById(uid).orElse(null);
            return ResponseEntity.ok(Map.of(
                    "id", uid, "role", role,
                    "nome", t != null ? t.getNome() : "",
                    "email", t != null ? t.getEmail() : ""));
        }
        Funcionario f = funcionarioRepository.findById(uid).orElse(null);
        return ResponseEntity.ok(Map.of(
                "id", uid, "role", role == null ? "" : role,
                "nome", f != null && f.getNome() != null ? f.getNome() : "",
                "cargo", f != null && f.getCargo() != null ? f.getCargo() : ""));
    }

    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<?> logout(@RequestBody(required = false) RefreshRequest request) {
        if (request != null && request.refreshToken() != null) {
            authRepository.findByRefreshTokenAndRevokedFalse(request.refreshToken())
                    .ifPresent(token -> {
                        token.setRevoked(true);
                        authRepository.save(token);
                        log.info("Logout: tutorId={} refreshToken revogado", token.getTutor().getId());
                    });
        }
        return ResponseEntity.ok(Map.of("message", "Logout realizado com sucesso"));
    }

    @PostMapping("/refresh")
    @Transactional
    public ResponseEntity<?> refresh(@RequestBody(required = false) RefreshRequest request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token invalido"));
        }

        Optional<Auth> tokenOpt = authRepository.findByRefreshTokenAndRevokedFalse(request.refreshToken());
        if (tokenOpt.isEmpty()) {
            log.warn("Refresh token invalido/ja utilizado");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token invalido"));
        }

        Auth currentToken = tokenOpt.get();
        if (currentToken.getExpiresAt().isBefore(Instant.now())) {
            currentToken.setRevoked(true);
            authRepository.save(currentToken);
            log.warn("Refresh token expirado: tutorId={}", currentToken.getTutor().getId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token expirado"));
        }

        currentToken.setRevoked(true);
        authRepository.save(currentToken);

        Tutor tutor = currentToken.getTutor();
        Auth newRefreshSession = buildRefreshSession(tutor);
        authRepository.save(newRefreshSession);

        String accessToken = jwtService.generateAccessToken(tutor.getId(), tutor.getEmail(), Roles.TUTOR);
        log.info("Token renovado: tutorId={}", tutor.getId());
        return ResponseEntity.ok(new TokenResponse(
                accessToken, newRefreshSession.getRefreshToken(), TOKEN_TYPE, jwtService.getAccessTokenSeconds()));
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

    
    private String normalizeRole(String cargo) {
        if (cargo == null) {
            return Roles.CAIXA;
        }
        String c = cargo.trim().toUpperCase();
        if (c.contains("GEREN")) return Roles.GERENTE;
        if (c.contains("VETERIN")) return Roles.VETERINARIO;
        if (c.contains("TOSAD") || c.contains("BANHIST")) return Roles.TOSADOR;
        if (c.contains("CAIXA")) return Roles.CAIXA;
        return Roles.STAFF.contains(c) ? c : Roles.CAIXA;
    }

    private Auth buildRefreshSession(Tutor tutor) {
        Auth auth = new Auth();
        auth.setTutor(tutor);
        auth.setRevoked(false);
        auth.setRefreshToken(UUID.randomUUID() + "." + UUID.randomUUID());
        auth.setExpiresAt(Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS));
        return auth;
    }

    public record LoginRequest(String email, String senha) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
    }
}
