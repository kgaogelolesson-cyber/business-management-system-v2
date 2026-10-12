package com.bms.backend.inventory;

/**
 * The JSON for a stock adjustment. change is positive to ADD stock (delivery received) and negative to
 * REMOVE stock (damaged goods). A reason is always required.
 */
public record AdjustmentRequest(Integer change, String reason) {
}
