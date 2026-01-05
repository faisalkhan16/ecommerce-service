package com.faisal.unit.service;

import com.faisal.discount.DiscountService;
import com.faisal.dto.AuthUser;
import com.faisal.security.SecurityUtils;
import com.faisal.dto.request.OrderItemRequest;
import com.faisal.dto.response.OrderResponse;
import com.faisal.enums.Role;
import com.faisal.mapper.OrderMapper;
import com.faisal.model.Order;
import com.faisal.model.OrderItem;
import com.faisal.repository.OrderRepository;
import com.faisal.service.OrderService;
import com.faisal.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DiscountService discountService;

    @Mock
    private ProductService productService;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    /* ---------------- PLACE ORDER ---------------- */

    @Test
    void placeOrder_shouldCreateOrderWithoutDiscount() {
        AuthUser authUser = new AuthUser(1L, Role.USER, "test@example.com");
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUser).thenReturn(authUser);

            OrderItemRequest item1 = new OrderItemRequest(101L, 2);
            OrderItemRequest item2 = new OrderItemRequest(102L, 1);

            when(productService.reserveStock(101L, 2))
                    .thenReturn(BigDecimal.valueOf(50)); // 100
            when(productService.reserveStock(102L, 1))
                    .thenReturn(BigDecimal.valueOf(100)); // 100

            // subtotal = 200
            when(discountService.totalDiscount(eq(Role.USER), any(BigDecimal.class), anyList()))
                    .thenReturn(BigDecimal.ZERO);

            Order savedOrder = mock(Order.class);
            OrderResponse response = mock(OrderResponse.class);

            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(orderMapper.toResponse(eq(savedOrder), eq(BigDecimal.ZERO)))
                    .thenReturn(response);

            OrderResponse result = orderService.placeOrder(
                    List.of(item1, item2)
            );

            assertThat(result).isSameAs(response);

            verify(productService).reserveStock(101L, 2);
            verify(productService).reserveStock(102L, 1);
            verify(orderRepository).save(any(Order.class));
        }
    }

    @Test
    void placeOrder_shouldApplyDiscountProportionally() {
        AuthUser authUser = new AuthUser(2L, Role.PREMIUM_USER, "premium@example.com");
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUser).thenReturn(authUser);

            OrderItemRequest item = new OrderItemRequest(201L, 2);

            // unit price 50 → subtotal 100
            when(productService.reserveStock(201L, 2))
                    .thenReturn(BigDecimal.valueOf(50));

            when(discountService.totalDiscount(
                    eq(Role.PREMIUM_USER),
                    argThat(bd -> bd.compareTo(BigDecimal.valueOf(100)) == 0),
                    anyList()
            )).thenReturn(BigDecimal.valueOf(20));

            Order savedOrder = mock(Order.class);
            OrderResponse response = mock(OrderResponse.class);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

            when(orderRepository.save(orderCaptor.capture()))
                    .thenReturn(savedOrder);

            when(orderMapper.toResponse(
                    eq(savedOrder),
                    argThat(bd -> bd.compareTo(BigDecimal.valueOf(20)) == 0)
            )).thenReturn(response);

            OrderResponse result = orderService.placeOrder(
                    List.of(item)
            );

            Order capturedOrder = orderCaptor.getValue();
            OrderItem orderItem = capturedOrder.getItems().get(0);

            assertThat(orderItem.getDiscountApplied())
                    .isEqualByComparingTo(BigDecimal.valueOf(20));

            assertThat(orderItem.getTotalPrice())
                    .isEqualByComparingTo(BigDecimal.valueOf(80));

            assertThat(capturedOrder.getOrderTotal())
                    .isEqualByComparingTo(BigDecimal.valueOf(80));

            assertThat(result).isSameAs(response);
        }
    }


    @Test
    void placeOrder_shouldHandleNullDiscountAsZero() {
        AuthUser authUser = new AuthUser(1L, Role.USER, "user@example.com");
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUser).thenReturn(authUser);

            OrderItemRequest item = new OrderItemRequest(301L, 1);

            when(productService.reserveStock(301L, 1))
                    .thenReturn(BigDecimal.valueOf(100));

            when(discountService.totalDiscount(any(), any(), anyList()))
                    .thenReturn(null);

            Order savedOrder = mock(Order.class);
            OrderResponse response = mock(OrderResponse.class);

            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(orderMapper.toResponse(eq(savedOrder), eq(BigDecimal.ZERO)))
                    .thenReturn(response);

            OrderResponse result = orderService.placeOrder(
                    List.of(item)
            );

            assertThat(result).isSameAs(response);
        }
    }

    @Test
    void placeOrder_shouldCapDiscountAtSubtotal() {
        AuthUser authUser = new AuthUser(1L, Role.ADMIN, "admin@example.com");
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUser).thenReturn(authUser);

            OrderItemRequest item = new OrderItemRequest(401L, 1);

            when(productService.reserveStock(401L, 1))
                    .thenReturn(BigDecimal.valueOf(100));

            doReturn(BigDecimal.valueOf(150))
                    .when(discountService)
                    .totalDiscount(
                            eq(Role.ADMIN),
                            argThat(bd -> bd.compareTo(BigDecimal.valueOf(100)) == 0),
                            anyList()
                    );

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

            when(orderRepository.save(orderCaptor.capture()))
                    .thenReturn(mock(Order.class));

            orderService.placeOrder(
                    List.of(item)
            );

            Order order = orderCaptor.getValue();
            OrderItem orderItem = order.getItems().get(0);

            assertThat(orderItem.getDiscountApplied())
                    .isEqualByComparingTo(BigDecimal.valueOf(100));

            assertThat(orderItem.getTotalPrice())
                    .isEqualByComparingTo(BigDecimal.ZERO);

            assertThat(order.getOrderTotal())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }


    @Test
    void placeOrder_shouldCreateOrderWithMultipleItemsAndDiscountSplit() {
        AuthUser authUser = new AuthUser(10L, Role.PREMIUM_USER, "p10@example.com");
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUser).thenReturn(authUser);

            OrderItemRequest item1 = new OrderItemRequest(501L, 1); // 100
            OrderItemRequest item2 = new OrderItemRequest(502L, 3); // 300

            when(productService.reserveStock(501L, 1))
                    .thenReturn(BigDecimal.valueOf(100));
            when(productService.reserveStock(502L, 3))
                    .thenReturn(BigDecimal.valueOf(100));

            when(discountService.totalDiscount(
                    eq(Role.PREMIUM_USER),
                    argThat(bd -> bd.compareTo(BigDecimal.valueOf(400)) == 0),
                    anyList()
            )).thenReturn(BigDecimal.valueOf(40));

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            when(orderRepository.save(captor.capture()))
                    .thenReturn(mock(Order.class));

            orderService.placeOrder(
                    List.of(item1, item2)
            );

            Order order = captor.getValue();

            assertThat(order.getItems()).hasSize(2);

            BigDecimal totalDiscountApplied = order.getItems().stream()
                    .map(OrderItem::getDiscountApplied)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(totalDiscountApplied)
                    .isEqualByComparingTo(BigDecimal.valueOf(40));

            assertThat(order.getOrderTotal())
                    .isEqualByComparingTo(BigDecimal.valueOf(360));
        }
    }

    @Test
    void placeOrder_userRole_shouldApplyNoDiscount() {
        AuthUser authUser = new AuthUser(1L, Role.USER, "u1@example.com");
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUser).thenReturn(authUser);

            OrderItemRequest item = new OrderItemRequest(1L, 2); // 2 × 100 = 200

            when(productService.reserveStock(1L, 2))
                    .thenReturn(BigDecimal.valueOf(100));

            when(discountService.totalDiscount(
                    eq(Role.USER),
                    any(BigDecimal.class),
                    anyList()
            )).thenReturn(BigDecimal.ZERO);

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            when(orderRepository.save(captor.capture()))
                    .thenReturn(mock(Order.class));

            orderService.placeOrder(List.of(item));

            Order order = captor.getValue();

            assertThat(order.getOrderTotal())
                    .isEqualByComparingTo(BigDecimal.valueOf(200));
        }
    }


    @Test
    void placeOrder_premiumUser_shouldApply10PercentDiscount() {
        AuthUser authUser = new AuthUser(1L, Role.PREMIUM_USER, "p1@example.com");
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUser).thenReturn(authUser);

            OrderItemRequest item = new OrderItemRequest(1L, 5); // 5 × 100 = 500

            when(productService.reserveStock(1L, 5))
                    .thenReturn(BigDecimal.valueOf(100));

            when(discountService.totalDiscount(
                    eq(Role.PREMIUM_USER),
                    any(BigDecimal.class),
                    anyList()
            )).thenReturn(BigDecimal.valueOf(50));

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            when(orderRepository.save(captor.capture()))
                    .thenReturn(mock(Order.class));

            orderService.placeOrder(List.of(item));

            Order order = captor.getValue();

            assertThat(order.getOrderTotal())
                    .isEqualByComparingTo(BigDecimal.valueOf(450));
        }
    }


    @Test
    void placeOrder_premiumUser_orderAbove500_shouldApply15PercentDiscount() {
        AuthUser authUser = new AuthUser(1L, Role.PREMIUM_USER, "p1_high@example.com");
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUser).thenReturn(authUser);

            OrderItemRequest item = new OrderItemRequest(1L, 8); // 8 × 100 = 800

            when(productService.reserveStock(1L, 8))
                    .thenReturn(BigDecimal.valueOf(100));

            when(discountService.totalDiscount(
                    eq(Role.PREMIUM_USER),
                    any(BigDecimal.class),
                    anyList()
            )).thenReturn(BigDecimal.valueOf(120));

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            when(orderRepository.save(captor.capture()))
                    .thenReturn(mock(Order.class));

            orderService.placeOrder(List.of(item));

            Order order = captor.getValue();

            assertThat(order.getOrderTotal())
                    .isEqualByComparingTo(BigDecimal.valueOf(680));
        }
    }


}
