package com.bms.backend.orders;

/** One product line of an order. unitPriceCents is the price AT THE TIME of the sale (later price changes do not alter old orders). */
public record OrderLineResponse(
        Long productId,
        String productName,
        int quantity,
        long unitPriceCents,
        long subtotalCents) {
}
