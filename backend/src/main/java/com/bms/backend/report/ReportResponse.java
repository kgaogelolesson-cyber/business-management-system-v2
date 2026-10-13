package com.bms.backend.report;

import java.util.List;

/**
 * The finished answer of one report. Every row holds one value per column: text for TEXT columns,
 * a whole number for NUMBER and MONEY columns (money in cents).
 */
public record ReportResponse(
        String title,
        String summary,
        String currencySymbol,
        List<ReportColumn> columns,
        List<List<Object>> rows) {
}
