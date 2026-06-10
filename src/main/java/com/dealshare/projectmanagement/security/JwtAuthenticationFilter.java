package com.dealshare.projectmanagement.security;

import com.dealshare.projectmanagement.common.error.ApiErrorResponse;
import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.common.web.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/")
                || path.startsWith("/api/health")
                || path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api/v1/api-docs")
                || path.equals("/swagger-ui.html")
                || path.equals("/api/v1/auth/token");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = header != null && header.startsWith("Bearer ")
                ? header.substring("Bearer ".length()).trim()
                : request.getParameter("access_token");
        if (token == null || token.isBlank()) {
            writeError(response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED, "Bearer token is required");
            return;
        }

        try {
            AuthenticatedUser user = jwtService.validate(token);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
            filterChain.doFilter(request, response);
        } catch (DomainException exception) {
            writeError(response, exception.status(), exception.code(), exception.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void writeError(HttpServletResponse response, HttpStatus status, ErrorCode code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), new ApiErrorResponse(
                code.name(),
                message,
                List.of(),
                MDC.get(CorrelationIdFilter.MDC_KEY),
                Instant.now()
        ));
    }
}
