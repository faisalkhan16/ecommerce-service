package com.faisal.integration;

import com.faisal.dto.request.LoginRequest;
import com.faisal.enums.Role;
import com.faisal.model.User;
import com.faisal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void fullAuthFlow_shouldWorkCorrectly() throws Exception {
        // 1. Setup: Create a user
        String email = "integration@test.com";
        String password = "password123";
        userRepository.save(User.builder()
                .name("Integration Test")
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.USER)
                .build());

        // 2. Login: Get Token
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("data").get("token").asText();
        String authHeader = "Bearer " + token;

        // 3. Use Token: Access protected resource (GET /products)
        mockMvc.perform(get("/products")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. Logout: Revoke Token
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Logged out"));

        // 5. Use Revoked Token: Should be rejected
        mockMvc.perform(get("/products")
                        .header("Authorization", authHeader))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.message").value("Token revoked"));
    }

    @Test
    void accessProtectedResource_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withInvalidCredentials_shouldReturnBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistent@test.com", "wrongpass");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
