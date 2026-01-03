package com.faisal.integration;

import com.faisal.dto.request.CreateUserRequest;
import com.faisal.enums.Role;
import com.faisal.model.User;
import com.faisal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_asAdmin_shouldSucceed() throws Exception {
        CreateUserRequest request = new CreateUserRequest("Faisal", "faisal@example.com", Role.USER);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Faisal"))
                .andExpect(jsonPath("$.data.email").value("faisal@example.com"));

        assertThat(userRepository.existsByEmail("faisal@example.com")).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_asAdmin_shouldSucceed() throws Exception {
        User user = userRepository.save(User.builder()
                .name("Test User")
                .email("test@example.com")
                .role(Role.USER)
                .build());

        mockMvc.perform(get("/users/" + user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Test User"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_asAdmin_shouldReturnPagedData() throws Exception {
        userRepository.save(User.builder().name("User 1").email("u1@example.com").role(Role.USER).build());
        userRepository.save(User.builder().name("User 2").email("u2@example.com").role(Role.USER).build());

        mockMvc.perform(get("/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    @WithMockUser(roles = "USER")
    void listUsers_asUser_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_asAdmin_shouldSucceed() throws Exception {
        User user = userRepository.save(User.builder()
                .name("Old Name")
                .email("old@example.com")
                .role(Role.USER)
                .build());

        CreateUserRequest updateRequest = new CreateUserRequest("New Name", "new@example.com", Role.ADMIN);

        mockMvc.perform(put("/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Name"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_asAdmin_shouldSucceed() throws Exception {
        User user = userRepository.save(User.builder()
                .name("Delete Me")
                .email("delete@example.com")
                .role(Role.USER)
                .build());

        mockMvc.perform(delete("/users/" + user.getId()))
                .andExpect(status().isOk());

        assertThat(userRepository.findById(user.getId())).isEmpty();
    }
}
