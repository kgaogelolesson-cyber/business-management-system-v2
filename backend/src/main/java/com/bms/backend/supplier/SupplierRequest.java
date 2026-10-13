package com.bms.backend.supplier;

/** The JSON a browser sends to create or change a supplier. */
public record SupplierRequest(
        String supplierName,
        String contactPerson,
        String email,
        String phone,
        String address) {
}
