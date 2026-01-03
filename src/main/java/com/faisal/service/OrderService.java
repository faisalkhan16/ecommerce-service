package com.faisal.service;

import com.faisal.dto.request.OrderItemRequest;
import com.faisal.discount.DiscountService;
import com.faisal.dto.response.OrderResponse;
import com.faisal.mapper.OrderMapper;
import com.faisal.model.Order;
import com.faisal.model.OrderItem;
import com.faisal.repository.OrderRepository;
import com.faisal.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final DiscountService discountService;
    private final ProductService productService;
    private final OrderMapper orderMapper;


    @Transactional
    public OrderResponse placeOrder(Long userId, Role role, List<OrderItemRequest> itemRequests) {

        List<OrderItem> items = new ArrayList<>();

        for (OrderItemRequest req : itemRequests) {
            BigDecimal officialPrice = productService.reserveStock(req.productId(), req.quantity());
            items.add(OrderItem.builder()
                    .productId(req.productId())
                    .quantity(req.quantity())
                    .unitPrice(officialPrice)
                    .build());
        }

        BigDecimal subtotal = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountTotal = discountService.totalDiscount(role, subtotal);
        if (discountTotal == null) {
            discountTotal = BigDecimal.ZERO;
        }
        if (discountTotal.compareTo(subtotal) > 0) {
            discountTotal = subtotal;
        }

        for (OrderItem i : items) {
            BigDecimal lineTotal = i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal share = subtotal.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : lineTotal.divide(subtotal, 8, RoundingMode.HALF_UP);

            BigDecimal lineDiscount = discountTotal.multiply(share).setScale(2, RoundingMode.HALF_UP);
            i.setDiscountApplied(lineDiscount);
            i.setTotalPrice(lineTotal.subtract(lineDiscount).setScale(2, RoundingMode.HALF_UP));
        }

        BigDecimal orderTotal = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Order order = Order.builder()
                .userId(userId)
                .items(items)
                .orderTotal(orderTotal)
                .build();

        Order saved = orderRepository.save(order);
        OrderResponse orderResponse = orderMapper.toResponse(saved,discountTotal);
        log.info("Placed order id={} userId={} total={}", saved.getId(), userId, orderTotal);
        return orderResponse;
    }
}