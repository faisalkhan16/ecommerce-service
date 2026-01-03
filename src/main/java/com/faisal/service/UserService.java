package com.faisal.service;

import com.faisal.exception.BadRequestException;
import com.faisal.exception.ResourceNotFoundException;
import com.faisal.dto.request.CreateUserRequest;
import com.faisal.dto.response.UserResponse;
import com.faisal.mapper.UserMapper;
import com.faisal.model.User;
import com.faisal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("User registration failed: Email '" + request.email() + "' is already in use.");
        }

        String randomPassword = "abcd1234";
        User user = userMapper.fromCreate(request);

        String encodedPassword = passwordEncoder.encode(randomPassword);
        user.setPassword(encodedPassword);

        User saved = userRepository.save(user);

        log.info("Created user id={}, email={}", saved.getId(), saved.getEmail());
        return userMapper.toResponse(saved);
    }

    public UserResponse getUser(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    public UserResponse update(Long id, CreateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already exists");
        }

        userMapper.update(user, request);
        User saved = userRepository.save(user);

        log.info("Updated user id={}", id);
        return userMapper.toResponse(saved);
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found");
        }
        userRepository.deleteById(id);
        log.info("Deleted user id={}", id);
    }
}