package com.bms.backend.report;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *   GET /api/reports                           the reports on offer
 *   GET /api/reports/SALES_BY_DAY?from=2026-10-01&to=2026-10-31    run one report
 * Administrator only (see SecurityConfig). Reports only read data; they never change anything.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReportTypeInfo> types() {
        return service.types();
    }

    @GetMapping("/{type}")
    public ReportResponse run(@PathVariable String type,
                              @RequestParam(required = false) String from,
                              @RequestParam(required = false) String to) {
        return service.run(type, from, to);
    }
}
