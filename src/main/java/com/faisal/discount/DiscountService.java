package com.faisal.discount;

import com.faisal.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final List<DiscountHandler> handlers;

    public BigDecimal totalDiscount(Role role, BigDecimal subtotal, List<BigDecimal> lineTotals) {
        if (subtotal == null || subtotal.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        DiscountContext ctx = new DiscountContext(role, subtotal, lineTotals);

        for (DiscountHandler handler : handlers) {
            if (handler.handle(ctx) == DiscountChainResult.STOP) {
                break;
            }
        }

        return ctx.getDiscountTotal();
    }
}