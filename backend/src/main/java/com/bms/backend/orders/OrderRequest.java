package com.bms.backend.orders;

import java.util.List;

/**
 * The JSON for a new order: the customer and the product lines. Only productId and quantity are used;
 * the price always comes from the products table, never from the browser.
 */
public record OrderRequest(Long customerId, List<Line> lines) {

    public record Line(Long productId, Integer quantity) {
    }
}
