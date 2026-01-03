package com.faisal.controller;

import com.faisal.dto.request.LoginRequest;
import com.faisal.dto.response.ApiResponse;
import com.faisal.dto.response.LoginResponse;
import com.faisal.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description = "Authenticates a user using email/password and returns a Bearer JWT access token.",
            operationId = "login"
    )
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Parameter(description = "Login request payload containing email and password.")
            @Valid @RequestBody LoginRequest request
    ) {
        String token = authService.login(request.email(), request.password());
        return ResponseEntity.ok(new ApiResponse<>(true, new LoginResponse(token, "Bearer")));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout",
            description = "Revokes the presented Bearer token by blacklisting its jti in Redis until it expires.",
            operationId = "logout"
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<String>> logout(
            @Parameter(
                    description = "Authorization header containing Bearer token. Format: 'Bearer <JWT>'.",
                    example = "Bearer <JWT_TOKEN_PLACEHOLDER>"
            )
            @RequestHeader("Authorization") String authorization
    ) {
        authService.logout(authorization);
        return ResponseEntity.ok(new ApiResponse<>(true, "Logged out"));
    }
}