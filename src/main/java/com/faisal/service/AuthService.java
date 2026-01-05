package com.faisal.service;

import com.faisal.dto.response.LoginResponse;
import com.faisal.enums.Role;
import com.faisal.exception.BadRequestException;
import com.faisal.model.User;
import com.faisal.repository.UserRepository;
import com.faisal.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Service
public class AuthService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    private final SecretKey key;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtService jwtService,
            StringRedisTemplate redisTemplate,
            @Value("${security.jwt.secret}") String secret
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public LoginResponse login(String email, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        Role role = user.getRole();
        Long userId = user.getId();

        return new LoginResponse(jwtService.generateToken(email, role,userId), "Bearer");
    }

    public void logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String jti = claims.getId();
        Date exp = claims.getExpiration();

        if (jti == null || exp == null) {
            throw new BadRequestException("Invalid token");
        }

        long ttlMillis = exp.getTime() - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return;
        }

        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", Duration.ofMillis(ttlMillis));
    }

    private String extractBearerToken(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BadRequestException("Missing or invalid Authorization header");
        }
        return header.substring("Bearer ".length());
    }
}