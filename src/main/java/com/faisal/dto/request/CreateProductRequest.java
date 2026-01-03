package com.faisal.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateProductRequest(

        @NotBlank(message = "name is required")
        @JsonProperty("name")
        String name,

        @NotBlank(message = "description is required")
        @JsonProperty("description")
        String description,

        @NotNull(message = "price is required")
        @Positive(message = "price must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "price must be a valid monetary format (e.g., 10.00)")
        @JsonProperty("price")
        BigDecimal price,

        @NotNull(message = "quantity is required")
        @Min(value = 0, message = "quantity cannot be negative")
        @JsonProperty("quantity")
        Integer quantity
) {}