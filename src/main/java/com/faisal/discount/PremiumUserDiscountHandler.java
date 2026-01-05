package com.faisal.discount;

import com.faisal.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(1)
public class PremiumUserDiscountHandler implements DiscountHandler {

    @Value("${app.discount.premium-user.rate}")
    private BigDecimal RATE;
    @Override
    public DiscountChainResult handle(DiscountContext ctx) {
        if (ctx.getRole() == Role.PREMIUM_USER) {
            ctx.addDiscount(ctx.getSubtotal().multiply(RATE));
        }
        return DiscountChainResult.CONTINUE;
    }
}