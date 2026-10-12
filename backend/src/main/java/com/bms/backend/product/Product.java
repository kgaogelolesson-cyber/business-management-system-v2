package com.bms.backend.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One row of the "products" table. Money is stored in cents (a whole number) so there are
 * never rounding errors: R12.50 is 1250.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    private String productName;
    private String category;
    private String description;
    private long priceCents;
    private int quantityInStock;
    private int reorderLevel;

    /** The suppliers module comes later, so for now this is just the number of the supplier (or null). */
    private Long supplierId;

    @Column(name = "is_active")
    private boolean active;

    protected Product() {
    }

    /** A new product: active. */
    public Product(String productName, String category, String description, long priceCents,
                   int quantityInStock, int reorderLevel, Long supplierId) {
        this.productName = productName;
        this.category = category;
        this.description = description;
        this.priceCents = priceCents;
        this.quantityInStock = quantityInStock;
        this.reorderLevel = reorderLevel;
        this.supplierId = supplierId;
        this.active = true;
    }

    /**
     * Changes the editable fields. The stock quantity is NOT here on purpose: like Version 1, stock
     * only changes through orders and stock adjustments (later steps), so the history stays correct.
     */
    public void change(String productName, String category, String description, long priceCents,
                       int reorderLevel, Long supplierId) {
        this.productName = productName;
        this.category = category;
        this.description = description;
        this.priceCents = priceCents;
        this.reorderLevel = reorderLevel;
        this.supplierId = supplierId;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public long getPriceCents() { return priceCents; }
    public int getQuantityInStock() { return quantityInStock; }
    public int getReorderLevel() { return reorderLevel; }
    public Long getSupplierId() { return supplierId; }
    public boolean isActive() { return active; }
}
