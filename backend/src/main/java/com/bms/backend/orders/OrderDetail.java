package com.bms.backend.orders;

import java.util.List;

/** An order with its lines. cancellation is null unless the order was cancelled (cancelling comes in the next step). */
public record OrderDetail(OrderSummary order, List<OrderLineResponse> lines, String cancellation) {
}
