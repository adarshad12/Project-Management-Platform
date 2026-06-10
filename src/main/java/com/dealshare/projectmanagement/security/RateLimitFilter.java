package com.dealshare.projectmanagement.security;

import com.dealshare.projectmanagement.common.error.ApiErrorResponse;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.common.web.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final int requestsPerMinute;

    public RateLimitFilter(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${security.rate-limit.requests-per-minute:120}") int requestsPerMinute
    ) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/health") || path.startsWith("/actuator") || path.equals("/ws");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String principal = request.getHeader("Authorization");
        if (principal == null || principal.isBlank()) {
            principal = request.getRemoteAddr();
        }
        String key = "rate-limit:" + Integer.toHexString(principal.hashCode()) + ":" + Instant.now().getEpochSecond() / 60;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, Duration.ofSeconds(90));
        }
        if (count != null && count > requestsPerMinute) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            objectMapper.writeValue(response.getWriter(), new ApiErrorResponse(
                    ErrorCode.RATE_LIMITED.name(),
                    "Rate limit exceeded",
                    List.of(),
                    MDC.get(CorrelationIdFilter.MDC_KEY),
                    Instant.now()
            ));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
