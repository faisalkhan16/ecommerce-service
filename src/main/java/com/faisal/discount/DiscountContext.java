package com.faisal.discount;

import com.faisal.enums.Role;

import java.math.BigDecimal;
import java.util.List;

public class DiscountContext {
    private final Role role;
    private final BigDecimal subtotal;
    private final List<BigDecimal> lineTotals;

    private BigDecimal discountTotal = BigDecimal.ZERO;

    public DiscountContext(Role role, BigDecimal subtotal, List<BigDecimal> lineTotals) {
        this.role = role;
        this.subtotal = subtotal;
        this.lineTotals = lineTotals;
    }

    public Role getRole() {
        return role;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountTotal() {
        return discountTotal;
    }

    public void addDiscount(BigDecimal amount) {
        if (amount == null) return;
        if (amount.signum() <= 0) return;
        this.discountTotal = this.discountTotal.add(amount);
    }

    public List<BigDecimal> getLineTotals() {
        return lineTotals;
    }
}