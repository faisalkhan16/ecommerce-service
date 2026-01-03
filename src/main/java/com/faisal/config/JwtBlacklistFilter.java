package com.faisal.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtBlacklistFilter extends OncePerRequestFilter {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = auth.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            writeUnauthorized(response, "Missing Bearer token");
            return;
        }

        final Claims claims;
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException ex) {
            // Token is present but invalid (expired, malformed, bad signature, etc.)
            log.debug("Invalid JWT presented to blacklist filter: {}", ex.getMessage());
            writeUnauthorized(response, "Invalid token");
            return;
        } catch (IllegalArgumentException ex) {
            log.debug("Invalid JWT input presented to blacklist filter: {}", ex.getMessage());
            writeUnauthorized(response, "Invalid token");
            return;
        }

        String jti = claims.getId();
        if (jti != null && !jti.isBlank()) {
            String key = BLACKLIST_PREFIX + jti;
            String blacklisted = redisTemplate.opsForValue().get(key);
            if (blacklisted != null) {
                log.info("Blocked request with blacklisted token jti={}", jti);
                writeUnauthorized(response, "Token revoked");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"success\":false,\"data\":{\"message\":\"" + escapeJson(message) + "\"}}");
    }

    private String escapeJson(String s) {
        // minimal escaping to keep JSON valid
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}