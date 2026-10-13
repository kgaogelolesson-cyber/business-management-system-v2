package com.bms.backend.report;

/** One report the screen can offer. needsDates = whether the from/to dates are used. */
public record ReportTypeInfo(String key, String label, boolean needsDates) {
}
