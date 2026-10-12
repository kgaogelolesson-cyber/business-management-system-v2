package com.bms.backend.inventory;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *   GET  /api/inventory                       stock list (?search=text  &status=LOW STOCK)
 *   GET  /api/inventory/5/history             the stock history of product 5
 *   POST /api/inventory/5/adjust              add or remove stock (administrator only)
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<InventoryItem> list(@RequestParam(required = false) String search,
                                    @RequestParam(required = false) String status) {
        return service.list(search, status);
    }

    @GetMapping("/{productId}/history")
    public List<HistoryEntry> history(@PathVariable Long productId) {
        return service.history(productId);
    }

    @PostMapping("/{productId}/adjust")
    public InventoryItem adjust(@PathVariable Long productId, @RequestBody AdjustmentRequest request,
                                Authentication authentication) {
        return service.adjust(productId, request, authentication.getName());
    }
}
