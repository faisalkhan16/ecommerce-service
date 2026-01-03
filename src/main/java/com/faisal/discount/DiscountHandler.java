package com.faisal.discount;

public interface DiscountHandler {
    DiscountChainResult handle(DiscountContext ctx);
}