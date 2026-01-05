package com.faisal.discount;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(2)
public class HighValueOrderDiscountHandler implements DiscountHandler {

    @Value("${app.discount.high-value-order.threshold}")
    private BigDecimal THRESHOLD;

    @Value("${app.discount.high-value-order.rate}")
    private BigDecimal RATE;

    @Override
    public DiscountChainResult handle(DiscountContext ctx) {
        if (ctx.getSubtotal().compareTo(THRESHOLD) > 0) {
            ctx.addDiscount(ctx.getSubtotal().multiply(RATE));
        }
        return DiscountChainResult.CONTINUE;
    }
}