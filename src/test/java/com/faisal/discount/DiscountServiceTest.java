package com.faisal.discount;

import com.faisal.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    private List<DiscountHandler> handlers;
    private DiscountService discountService;
    private DiscountProperties properties;

    @BeforeEach
    void setup() {
        properties = new DiscountProperties();
        
        // Setup default properties for tests
        properties.getHighLineItem().setThreshold(new BigDecimal("100.00"));
        properties.getHighLineItem().setAmount(new BigDecimal("50.00"));
        
        properties.getHighValueOrder().setThreshold(new BigDecimal("500.00"));
        properties.getHighValueOrder().setRate(new BigDecimal("0.05"));
        
        properties.getPremiumUser().setRate(new BigDecimal("0.10"));

        handlers = new ArrayList<>();
        discountService = new DiscountService(handlers);
    }

    @Test
    void totalDiscount_shouldApplyHighLineItemDiscount() {
        // HighLineItemDiscountHandler has @Order(3)
        // HighValueOrderDiscountHandler has @Order(2)
        handlers.add(new HighValueOrderDiscountHandler(properties));
        handlers.add(new HighLineItemDiscountHandler(properties));

        // Subtotal = 150, one item = 150.
        // HighValueOrderDiscountHandler: 150 < 500 -> 0
        // HighLineItemDiscountHandler: 150 > 100 -> 50
        BigDecimal result = discountService.totalDiscount(Role.USER, new BigDecimal("150.00"), List.of(new BigDecimal("150.00")));
        assertThat(result).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void totalDiscount_shouldApplyMultipleHighLineItemDiscounts() {
        handlers.add(new HighLineItemDiscountHandler(properties));

        // Subtotal = 250, items = 120, 130.
        // Both > 100 -> 50 + 50 = 100
        BigDecimal result = discountService.totalDiscount(Role.USER, new BigDecimal("250.00"), List.of(new BigDecimal("120.00"), new BigDecimal("130.00")));
        assertThat(result).isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
