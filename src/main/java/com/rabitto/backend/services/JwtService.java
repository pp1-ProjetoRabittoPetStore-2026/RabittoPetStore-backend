package com.rabitto.backend.services;

import com.rabitto.backend.security.Roles;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtService {

    @Value("${app.auth.jwt-secret:change-this-secret-key-with-at-least-32-characters}")
    private String jwtSecret;

    @Value("${app.auth.access-token-minutes:1440}")
    private long accessTokenMinutes;

    
    public String generateAccessToken(Long uid, String subject, String role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(subject)
                .claim("uid", uid)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(getSigningKey())
                .compact();
    }

    public long getAccessTokenSeconds() {
        return accessTokenMinutes * 60;
    }

    
    public Claims parse(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de acesso ausente");
        }
        String token = authorizationHeader.substring(7);
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido", ex);
        }
    }

    public String extractRole(String authorizationHeader) {
        return parse(authorizationHeader).get("role", String.class);
    }

    public Long extractUid(String authorizationHeader) {
        Number uid = parse(authorizationHeader).get("uid", Number.class);
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido");
        }
        return uid.longValue();
    }

    
    public Long extractTutorId(String authorizationHeader) {
        Claims claims = parse(authorizationHeader);
        String role = claims.get("role", String.class);
        if (!Roles.TUTOR.equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso permitido apenas para tutores");
        }
        Number uid = claims.get("uid", Number.class);
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido");
        }
        return uid.longValue();
    }

    public SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
