package com.faisal.unit.service;

import com.faisal.dto.request.CreateUserRequest;
import com.faisal.dto.response.UserResponse;
import com.faisal.exception.BadRequestException;
import com.faisal.exception.ResourceNotFoundException;
import com.faisal.mapper.UserMapper;
import com.faisal.model.User;
import com.faisal.repository.UserRepository;
import com.faisal.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    /* ---------------- CREATE USER ---------------- */

    @Test
    void createUser_shouldCreateUser_whenEmailNotExists() {
        CreateUserRequest request = mock(CreateUserRequest.class);
        User user = mock(User.class);
        User savedUser = mock(User.class);
        UserResponse response = mock(UserResponse.class);

        when(request.email()).thenReturn("test@mail.com");
        when(userRepository.existsByEmail("test@mail.com")).thenReturn(false);
        when(userMapper.fromCreate(request)).thenReturn(user);
        when(passwordEncoder.encode("abcd1234")).thenReturn("encodedPass");
        when(userRepository.save(user)).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(response);

        UserResponse result = userService.createUser(request);

        verify(user).setPassword("encodedPass");
        verify(userRepository).save(user);
        assertThat(result).isSameAs(response);
    }

    @Test
    void createUser_shouldThrowException_whenEmailAlreadyExists() {
        CreateUserRequest request = mock(CreateUserRequest.class);

        when(request.email()).thenReturn("test@mail.com");
        when(userRepository.existsByEmail("test@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already in use");

        verify(userRepository, never()).save(any());
    }

    /* ---------------- GET USER ---------------- */

    @Test
    void getUser_shouldReturnUser_whenExists() {
        User user = mock(User.class);
        UserResponse response = mock(UserResponse.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.getUser(1L);

        assertThat(result).isSameAs(response);
    }

    @Test
    void getUser_shouldThrowException_whenNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    /* ---------------- LIST USERS ---------------- */

    @Test
    void list_shouldReturnPagedUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = mock(User.class);
        UserResponse response = mock(UserResponse.class);

        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(pageable)).thenReturn(page);
        when(userMapper.toResponse(user)).thenReturn(response);

        Page<UserResponse> result = userService.list(pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    /* ---------------- UPDATE USER ---------------- */

    @Test
    void update_shouldUpdateUser_whenEmailUnchanged() {
        CreateUserRequest request = mock(CreateUserRequest.class);
        User user = mock(User.class);
        User savedUser = mock(User.class);
        UserResponse response = mock(UserResponse.class);

        when(request.email()).thenReturn("same@mail.com");
        when(user.getEmail()).thenReturn("same@mail.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(response);

        UserResponse result = userService.update(1L, request);

        verify(userMapper).update(user, request);
        assertThat(result).isSameAs(response);
    }

    @Test
    void update_shouldThrowException_whenEmailAlreadyExists() {
        CreateUserRequest request = mock(CreateUserRequest.class);
        User user = mock(User.class);

        when(request.email()).thenReturn("new@mail.com");
        when(user.getEmail()).thenReturn("old@mail.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowException_whenUserNotFound() {
        CreateUserRequest request = mock(CreateUserRequest.class);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    /* ---------------- DELETE USER ---------------- */

    @Test
    void delete_shouldDeleteUser_whenExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.delete(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowException_whenUserNotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, never()).deleteById(any());
    }
}
