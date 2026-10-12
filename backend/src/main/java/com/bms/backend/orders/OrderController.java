package com.bms.backend.orders;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 *   GET  /api/orders            list (?search=  &status=COMPLETED  &from=2026-10-01  &to=2026-10-31  &customerId=3)
 *   GET  /api/orders/7          one order with its lines
 *   POST /api/orders            create an order (any logged-in user with an employee record)
 *   POST /api/orders/7/cancel   cancel an order and put its stock back (administrator only)
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrderSummary> list(@RequestParam(required = false) String search,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String from,
                                   @RequestParam(required = false) String to,
                                   @RequestParam(required = false) Long customerId,
                                   Authentication authentication) {
        return service.list(search, status, from, to, customerId, authentication.getName(), isAdmin(authentication));
    }

    @GetMapping("/{id}")
    public OrderDetail get(@PathVariable Long id, Authentication authentication) {
        return service.get(id, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetail create(@RequestBody OrderRequest request, Authentication authentication) {
        return service.create(request, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/{id}/cancel")
    public OrderDetail cancel(@PathVariable Long id, Authentication authentication) {
        return service.cancel(id, authentication.getName());
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
