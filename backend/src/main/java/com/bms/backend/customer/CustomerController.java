package com.bms.backend.customer;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The web layer: it replaces the Swing panels of Version 1.
 * It receives an HTTP request, calls the service, and returns the result as JSON.
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    // GET http://localhost:8080/api/customers
    @GetMapping
    public List<CustomerResponse> list() {
        return service.listActive();
    }
}
