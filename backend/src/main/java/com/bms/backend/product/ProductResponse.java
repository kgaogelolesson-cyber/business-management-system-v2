package com.bms.backend.product;

/** What the API sends to the browser. stockStatus uses the same three words as Version 1's inventory view. */
public record ProductResponse(
        Long id,
        String productName,
        String category,
        String description,
        long priceCents,
        int quantityInStock,
        int reorderLevel,
        Long supplierId,
        String stockStatus) {

    static ProductResponse from(Product p) {
        String status;
        if (p.getQuantityInStock() == 0) {
            status = "OUT OF STOCK";
        } else if (p.getQuantityInStock() <= p.getReorderLevel()) {
            status = "LOW STOCK";
        } else {
            status = "IN STOCK";
        }
        return new ProductResponse(
                p.getProductId(),
                p.getProductName(),
                p.getCategory(),
                p.getDescription(),
                p.getPriceCents(),
                p.getQuantityInStock(),
                p.getReorderLevel(),
                p.getSupplierId(),
                status);
    }
}
