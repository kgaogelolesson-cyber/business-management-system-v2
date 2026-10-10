package com.bms.backend.customer;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The service layer, as in Version 1: business rules live here, not in the controller.
 * For now there are no rules yet, only "list the active customers".
 */
@Service
public class CustomerService {

    private final CustomerRepository customers;

    // Spring sees this constructor and passes in the repository by itself ("dependency injection").
    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> listActive() {
        return customers.findByActiveTrueOrderByLastNameAscFirstNameAsc()
                .stream()
                .map(CustomerResponse::from)
                .toList();
    }
}
