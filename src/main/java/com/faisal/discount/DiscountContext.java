package com.faisal.discount;

import com.faisal.enums.Role;

import java.math.BigDecimal;
import java.util.Objects;

public class DiscountContext {
    private final Role role;
    private final BigDecimal subtotal;
    private BigDecimal discountTotal = BigDecimal.ZERO;

    public DiscountContext(Role role, BigDecimal subtotal) {
        this.role = Objects.requireNonNull(role, "role");
        this.subtotal = Objects.requireNonNull(subtotal, "subtotal");
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
}