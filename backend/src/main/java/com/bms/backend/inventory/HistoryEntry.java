package com.bms.backend.inventory;

import java.time.LocalDateTime;

/** One line of a product's stock history. */
public record HistoryEntry(
        LocalDateTime adjustedAt,
        int quantityChange,
        String reason,
        String adjustedBy) {
}
