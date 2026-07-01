package com.rabitto.backend.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Access log: every HTTP request gets a short correlation id (MDC "reqId") so all
 * log lines from the same request can be grepped together, plus a start/end line
 * with method, path, status and duration.
 */
@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("com.rabitto.backend.access");

    public static final String REQUEST_ID_MDC_KEY = "reqId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String path = query != null ? uri + "?" + query : uri;

        long start = System.currentTimeMillis();
        log.info(">> {} {}", method, path);
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            int status = response.getStatus();
            if (status >= 500) {
                log.error("<< {} {} -> {} ({} ms)", method, path, status, durationMs);
            } else if (status >= 400) {
                log.warn("<< {} {} -> {} ({} ms)", method, path, status, durationMs);
            } else {
                log.info("<< {} {} -> {} ({} ms)", method, path, status, durationMs);
            }
            MDC.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}
