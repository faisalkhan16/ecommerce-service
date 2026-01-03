package com.faisal.discount;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(2)
public class HighValueOrderDiscountHandler implements DiscountHandler {

    private static final BigDecimal THRESHOLD = new BigDecimal("500");
    private static final BigDecimal RATE = new BigDecimal("0.05");

    @Override
    public DiscountChainResult handle(DiscountContext ctx) {
        if (ctx.getSubtotal().compareTo(THRESHOLD) > 0) {
            ctx.addDiscount(ctx.getSubtotal().multiply(RATE));
        }
        return DiscountChainResult.CONTINUE;
    }
}