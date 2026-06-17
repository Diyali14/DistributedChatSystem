package com.chatsphere.gateway.filter;

import com.chatsphere.common.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Bypass check for auth paths, swagger documentation, and actuator health endpoints
        if (isSecuredBypass(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(request);

        if (token == null || !JwtUtil.validateToken(token)) {
            return onError(exchange, "Unauthorized: Invalid or missing token", HttpStatus.UNAUTHORIZED);
        }

        try {
            Claims claims = JwtUtil.extractClaims(token);
            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            List<?> rolesList = claims.get("roles", List.class);
            String sessionId = claims.get("sessionId", String.class);

            String roles = "";
            if (rolesList != null) {
                roles = StringUtils.collectionToCommaDelimitedString(rolesList);
            }

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Username", username)
                    .header("X-User-Roles", roles)
                    .header("X-Session-Id", sessionId)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            return onError(exchange, "Unauthorized: Error parsing token claims", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isSecuredBypass(String path) {
        return path.contains("/api/auth/login") ||
               path.contains("/api/auth/register") ||
               path.contains("/api/auth/refresh") ||
               path.contains("/api/auth/verify") ||
               path.contains("/api/auth/forgot-password") ||
               path.contains("/api/auth/reset-password") ||
               path.contains("/actuator") ||
               path.contains("/v3/api-docs") ||
               path.contains("/swagger-ui");
    }

    private String extractToken(ServerHttpRequest request) {
        // 1. Try extracting from Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. Try extracting from query parameter (vital for WebSocket connection handshakes)
        String paramToken = request.getQueryParams().getFirst("token");
        if (StringUtils.hasText(paramToken)) {
            return paramToken;
        }

        return null;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        byte[] bytes = String.format("{\"error\": \"%s\"}", err).getBytes();
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -100; // Run early in the gateway chain
    }
}
