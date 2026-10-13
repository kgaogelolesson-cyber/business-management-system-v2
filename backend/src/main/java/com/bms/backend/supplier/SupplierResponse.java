package com.bms.backend.supplier;

/** What the API sends to the browser. productCount = how many ACTIVE products this supplier provides. */
public record SupplierResponse(
        Long id,
        String supplierName,
        String contactPerson,
        String email,
        String phone,
        String address,
        long productCount) {

    static SupplierResponse from(Supplier s, long productCount) {
        return new SupplierResponse(s.getSupplierId(), s.getSupplierName(), s.getContactPerson(),
                s.getEmail(), s.getPhone(), s.getAddress(), productCount);
    }
}
