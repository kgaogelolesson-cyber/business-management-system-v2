package com.bms.backend.inventory;

/** One line of the inventory screen: a product with its stock and stock status (from the v_inventory view). */
public record InventoryItem(
        Long id,
        String productName,
        String category,
        int quantityInStock,
        int reorderLevel,
        String stockStatus,
        String supplierName) {
}
