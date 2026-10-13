package com.bms.backend.supplier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One row of the "suppliers" table. Deleting only hides a supplier (is_active = false). */
@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long supplierId;

    private String supplierName;
    private String contactPerson;
    private String email;
    private String phone;
    private String address;

    @Column(name = "is_active")
    private boolean active;

    protected Supplier() {
    }

    public Supplier(String supplierName, String contactPerson, String email, String phone, String address) {
        change(supplierName, contactPerson, email, phone, address);
        this.active = true;
    }

    public void change(String supplierName, String contactPerson, String email, String phone, String address) {
        this.supplierName = supplierName;
        this.contactPerson = contactPerson;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getSupplierId() { return supplierId; }
    public String getSupplierName() { return supplierName; }
    public String getContactPerson() { return contactPerson; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public boolean isActive() { return active; }
}
