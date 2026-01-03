package com.faisal.mapper;

import com.faisal.dto.response.OrderItemResponse;
import com.faisal.dto.response.OrderResponse;
import com.faisal.model.Order;
import com.faisal.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "total", source = "orderTotal")
    OrderResponse toResponse(Order order);

    OrderItemResponse toItemResponse(OrderItem item);

    default OrderResponse toResponse(Order order, BigDecimal discountTotal) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getOrderTotal(),
                discountTotal,  // set here
                order.getCreatedAt(),
                order.getItems().stream().map(this::toItemResponse).toList()
        );
    }
}