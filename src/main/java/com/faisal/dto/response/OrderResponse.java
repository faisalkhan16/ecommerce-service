package com.faisal.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        @JsonProperty("order_id")
        Long orderId,

        @JsonProperty("user_id")
        Long userId,

        @JsonProperty("total")
        BigDecimal total,

        @JsonProperty("total_discount")
        BigDecimal totalDiscount,

        @JsonProperty("created_at")
        Instant createdAt,

        @JsonProperty("items")
        List<OrderItemResponse> items
) {
}
