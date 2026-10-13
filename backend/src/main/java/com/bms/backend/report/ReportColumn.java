package com.bms.backend.report;

/** One column heading. type is TEXT (words), NUMBER (a whole number) or MONEY (whole cents: 68498 = 684.98). */
public record ReportColumn(String header, String type) {
}
