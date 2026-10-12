package com.bms.backend.product;

/**
 * The JSON a browser sends to create or change a product.
 * priceCents is in cents (R12.50 = 1250). quantityInStock is only used when CREATING a product;
 * when changing one it is ignored. Numbers are "boxed" (Long/Integer) so a missing value arrives
 * as null and we can answer with a clear message instead of silently using 0.
 */
public record ProductRequest(
        String productName,
        String category,
        String description,
        Long priceCents,
        Integer quantityInStock,
        Integer reorderLevel,
        Long supplierId) {
}
