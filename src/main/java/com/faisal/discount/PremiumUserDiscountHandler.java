package com.faisal.discount;

import com.faisal.enums.Role;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(1)
public class PremiumUserDiscountHandler implements DiscountHandler {

    private static final BigDecimal RATE = new BigDecimal("0.10");

    @Override
    public DiscountChainResult handle(DiscountContext ctx) {
        if (ctx.getRole() == Role.PREMIUM_USER) {
            ctx.addDiscount(ctx.getSubtotal().multiply(RATE));
        }
        return DiscountChainResult.CONTINUE;
    }
}