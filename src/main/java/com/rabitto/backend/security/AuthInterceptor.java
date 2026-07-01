package com.rabitto.backend.security;

import com.rabitto.backend.services.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;


@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;

        }

        String path = request.getRequestURI();
        Set<String> required = requiredRoles(method, path);

        if (required == Rule.PUBLIC) {
            return true;
        }

        String header = request.getHeader("Authorization");
        String role;
        try {
            role = jwtService.extractRole(header);
        } catch (ResponseStatusException ex) {
            log.warn("Auth rejeitado: {} {} - {}", method, path, ex.getReason());
            throw ex;
        }

        if (required == Rule.AUTH) {
            if (role == null) {
                log.warn("Auth rejeitado: {} {} - token sem role", method, path);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido");
            }
            markAuthenticated(header, role);
            return true;
        }

        if (role == null || !required.contains(role)) {
            log.warn("Acesso negado: {} {} - role={} nao autorizado ({})", method, path, role, required);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para o seu perfil");
        }
        markAuthenticated(header, role);
        return true;
    }

    private void markAuthenticated(String header, String role) {
        MDC.put("role", role);
        try {
            MDC.put("uid", String.valueOf(jwtService.extractUid(header)));
        } catch (ResponseStatusException ignored) {

        }
    }

    private Set<String> requiredRoles(String method, String path) {
        boolean isGet = "GET".equalsIgnoreCase(method);

        

        if (path.startsWith("/auth/")) {
            return Rule.PUBLIC;
        }
        if ("POST".equalsIgnoreCase(method) && path.equals("/tutores")) {
            return Rule.PUBLIC; 

        }

        

        if (path.startsWith("/servicos")) {
            return isGet ? Rule.AUTH : Set.of(Roles.GERENTE);
        }

        

        if (path.startsWith("/pets")) {
            return Rule.AUTH;
        }

        

        if (path.equals("/agendamentos/horarios-disponiveis")) {
            return Rule.AUTH;
        }
        if (path.equals("/agendamentos/vet/agenda")) {
            return Set.of(Roles.VETERINARIO);
        }
        if (path.equals("/agendamentos/meus")) {
            return Rule.AUTH; 

        }
        if (path.equals("/agendamentos") && "POST".equalsIgnoreCase(method)) {
            return Rule.AUTH; 

        }
        if (path.startsWith("/agendamentos")) {
            return Roles.STAFF; 

        }

        

        if (path.startsWith("/funcionarios")) {
            return Set.of(Roles.GERENTE);
        }

        

        if (path.equals("/tutores/me")) {
            return Rule.AUTH; 

        }
        if (path.startsWith("/tutores")) {
            return Set.of(Roles.GERENTE);
        }

        return Rule.AUTH;
    }

    
    private static final class Rule {
        static final Set<String> PUBLIC = Set.of("__PUBLIC__");
        static final Set<String> AUTH = Set.of("__AUTH__");
    }
}
