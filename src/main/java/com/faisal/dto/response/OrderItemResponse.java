package com.faisal.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record OrderItemResponse(

        @JsonProperty("product_id")
        Long productId,

        @JsonProperty("quantity")
        Integer quantity,

        @JsonProperty("unit_price")
        BigDecimal unitPrice
) {}
