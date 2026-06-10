package com.rabitto.backend.security;

import com.rabitto.backend.services.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Enforce de RBAC baseado no claim "role" do JWT.
 *
 * Regras (primeira correspondencia vence). Papeis exigidos ou:
 *   - PUBLIC: sem token
 *   - AUTH:   qualquer usuario autenticado
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true; // preflight CORS
        }

        String path = request.getRequestURI();
        Set<String> required = requiredRoles(method, path);

        if (required == Rule.PUBLIC) {
            return true;
        }

        String header = request.getHeader("Authorization");
        String role = jwtService.extractRole(header); // lanca 401 se ausente/invalido

        if (required == Rule.AUTH) {
            if (role == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido");
            }
            return true;
        }

        if (role == null || !required.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para o seu perfil");
        }
        return true;
    }

    private Set<String> requiredRoles(String method, String path) {
        boolean isGet = "GET".equalsIgnoreCase(method);

        // --- Publico ---
        if (path.startsWith("/auth/")) {
            return Rule.PUBLIC;
        }
        if ("POST".equalsIgnoreCase(method) && path.equals("/tutores")) {
            return Rule.PUBLIC; // cadastro de tutor
        }

        // --- Servicos ---
        if (path.startsWith("/servicos")) {
            return isGet ? Rule.AUTH : Set.of(Roles.GERENTE);
        }

        // --- Pets (ownership garantido no controller) ---
        if (path.startsWith("/pets")) {
            return Rule.AUTH;
        }

        // --- Agendamentos ---
        if (path.equals("/agendamentos/horarios-disponiveis")) {
            return Rule.AUTH;
        }
        if (path.equals("/agendamentos/vet/agenda")) {
            return Set.of(Roles.VETERINARIO);
        }
        if (path.equals("/agendamentos/meus")) {
            return Rule.AUTH; // tutor consulta os proprios agendamentos
        }
        if (path.equals("/agendamentos") && "POST".equalsIgnoreCase(method)) {
            return Rule.AUTH; // tutor agenda servico
        }
        if (path.startsWith("/agendamentos")) {
            return Roles.STAFF; // listagens, status, edicao, remocao
        }

        // --- Funcionarios ---
        if (path.startsWith("/funcionarios")) {
            return Set.of(Roles.GERENTE);
        }

        // --- Tutores ---
        if (path.equals("/tutores/me")) {
            return Rule.AUTH; // perfil do proprio tutor
        }
        if (path.startsWith("/tutores")) {
            return Set.of(Roles.GERENTE);
        }

        return Rule.AUTH;
    }

    /** Marcadores especiais reutilizando referencia de Set por identidade. */
    private static final class Rule {
        static final Set<String> PUBLIC = Set.of("__PUBLIC__");
        static final Set<String> AUTH = Set.of("__AUTH__");
    }
}
