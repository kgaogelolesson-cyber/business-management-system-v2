package com.bms.backend.dashboard;

import java.util.List;

/**
 * The figures on the first screen after login. The administrator gets everything; an employee gets a
 * limited view: employees, completedOrders and revenueCents are 0, recentCustomers is empty, and
 * recentOrders holds only that employee's OWN orders. Money is in cents.
 */
public record DashboardResponse(
        boolean admin,
        long customers,
        long products,
        long employees,
        long completedOrders,
        long revenueCents,
        long lowStockCount,
        List<LowStockItem> lowStockItems,
        List<RecentOrder> recentOrders,
        List<RecentCustomer> recentCustomers,
        String currencySymbol) {

    public record LowStockItem(String productName, int quantityInStock, int reorderLevel, String stockStatus) {
    }

    public record RecentOrder(long orderId, String orderDate, String customerName, String employeeName,
                              long totalCents, String status) {
    }

    public record RecentCustomer(String customerName, String email, String createdAt) {
    }
}
