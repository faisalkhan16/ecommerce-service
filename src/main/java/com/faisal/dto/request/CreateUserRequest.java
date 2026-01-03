package com.faisal.dto.request;

import com.faisal.enums.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record  CreateUserRequest (
        @NotBlank(message = "name is required")
        @JsonProperty("name")
        String name,

        @NotBlank(message = "email is required")
        @Email(message = "email format is invalid")
        @JsonProperty("email")
        String email,

        @NotNull(message = "role is required")
        @JsonProperty("role")
        Role role
){}
