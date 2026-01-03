package com.faisal.unit.controller;

import com.faisal.controller.AuthController;
import com.faisal.config.JwtBlacklistFilter;
import com.faisal.dto.request.LoginRequest;
import com.faisal.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtBlacklistFilter jwtBlacklistFilter;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    void login_shouldReturn200_whenPayloadIsValid() throws Exception {
        LoginRequest request = new LoginRequest("test@mail.com", "password");
        String token = "jwt-token";
        when(authService.login(request.email(), request.password())).thenReturn(token);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value(token))
                .andExpect(jsonPath("$.data.token_type").value("Bearer"));
    }

    @Test
    void login_shouldReturn400_whenEmailIsInvalid() throws Exception {
        LoginRequest request = new LoginRequest("invalid-email", "password");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_shouldReturn200_whenHeaderIsPresent() throws Exception {
        String authHeader = "Bearer some-token";

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Logged out"));

        verify(authService).logout(authHeader);
    }

    @Test
    void logout_shouldReturn400_whenHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isInternalServerError());
    }
}
