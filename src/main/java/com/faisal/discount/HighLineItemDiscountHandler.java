package com.faisal.discount;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Order(3)
@RequiredArgsConstructor
public class HighLineItemDiscountHandler implements DiscountHandler {

    @Value("${app.discount.high-line-item.threshold}")
    private BigDecimal THRESHOLD;

    @Value("${app.discount.high-line-item.amount}")
    private BigDecimal AMOUNT;


    @Override
    public DiscountChainResult handle(DiscountContext ctx) {

        List<BigDecimal> lineTotals = ctx.getLineTotals();
        if (lineTotals == null || lineTotals.isEmpty()) {
            return DiscountChainResult.CONTINUE;
        }

        BigDecimal totalLineItemDiscount = BigDecimal.ZERO;

        for (BigDecimal lineTotal : lineTotals) {
            if (lineTotal != null && lineTotal.compareTo(THRESHOLD) > 0) {
                totalLineItemDiscount =
                        totalLineItemDiscount.add(AMOUNT);
            }
        }

        if (totalLineItemDiscount.compareTo(BigDecimal.ZERO) > 0) {
            ctx.addDiscount(totalLineItemDiscount);
        }

        return DiscountChainResult.CONTINUE;
    }
}
