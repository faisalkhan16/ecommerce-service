package com.faisal.unit.service;

import com.faisal.enums.Role;
import com.faisal.exception.BadRequestException;
import com.faisal.model.User;
import com.faisal.repository.UserRepository;
import com.faisal.security.JwtService;
import com.faisal.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private AuthService authService;
    private final String secret = "v9y$B&E)H@McQfTjWmZq4t7w!z%C*F-JaNdRgUkXp2s5u8x/A?D(G+KbPeShVkYp";
    private SecretKey key;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, userRepository, jwtService, redisTemplate, secret);
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() {
        String email = "test@example.com";
        String password = "password";
        String token = "jwt-token";
        User user = mock(User.class);
        when(user.getRole()).thenReturn(Role.USER);
        when(user.getId()).thenReturn(1L);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(email, Role.USER, 1L)).thenReturn(token);

        String result = authService.login(email, password);

        assertThat(result).isEqualTo(token);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_shouldThrowException_whenUserNotFound() {
        String email = "test@example.com";
        String password = "password";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(email, password))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void logout_shouldBlacklistToken_whenTokenIsValid() {
        String jti = "test-jti";
        long ttlMillis = 100000;
        Date exp = new Date(System.currentTimeMillis() + ttlMillis);
        String token = Jwts.builder()
                .setId(jti)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
        String header = "Bearer " + token;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.logout(header);

        verify(valueOperations).set(eq("jwt:blacklist:" + jti), eq("1"), any(Duration.class));
    }

    @Test
    void logout_shouldThrowException_whenHeaderIsMissing() {
        assertThatThrownBy(() -> authService.logout(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Missing or invalid Authorization header");
    }

    @Test
    void logout_shouldThrowException_whenHeaderIsInvalid() {
        assertThatThrownBy(() -> authService.logout("InvalidToken"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Missing or invalid Authorization header");
    }

    @Test
    void logout_shouldThrowException_whenTokenIsInvalid() {
        String invalidToken = "Bearer invalid.token.here";
        assertThatThrownBy(() -> authService.logout(invalidToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    void logout_shouldNotBlacklist_whenTokenIsExpired() {
        String jti = "test-jti";
        // Creating an already expired token for the test
        // parseClaimsJws will throw ExpiredJwtException.
        Date exp = new Date(System.currentTimeMillis() - 10000);
        String token = Jwts.builder()
                .setId(jti)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
        String header = "Bearer " + token;

        assertThatThrownBy(() -> authService.logout(header))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);

        verify(redisTemplate, never()).opsForValue();
    }
}
