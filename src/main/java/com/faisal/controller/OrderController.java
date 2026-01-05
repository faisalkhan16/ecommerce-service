package com.faisal.controller;

import com.faisal.dto.request.CreateOrderRequest;
import com.faisal.dto.response.ApiResponse;
import com.faisal.dto.response.OrderResponse;
import com.faisal.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','PREMIUM_USER','ADMIN')")
    @Operation(
            summary = "Place order",
            description = "Places a new order for a user and calculates totals (including discounts if applicable).",
            operationId = "placeOrder"
    )
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @Parameter(description = "Order creation payload including userId, role and items.")
            @Valid @RequestBody CreateOrderRequest orderRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, orderService.placeOrder(orderRequest.items())));
    }
}