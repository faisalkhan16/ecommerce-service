package com.faisal.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank
        @Email(message = "email format is invalid")
        @JsonProperty("email")
        String email,

        @NotBlank
        @JsonProperty("password")
        String password
) {}