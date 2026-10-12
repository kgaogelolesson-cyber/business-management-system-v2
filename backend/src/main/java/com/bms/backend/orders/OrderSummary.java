package com.bms.backend.orders;

import java.time.LocalDateTime;

/** One line of the orders list. totalCents is in cents (R159.00 = 15900). */
public record OrderSummary(
        Long id,
        LocalDateTime orderDate,
        String status,
        long totalCents,
        Long customerId,
        String customerName,
        Long employeeId,
        String employeeName) {
}
