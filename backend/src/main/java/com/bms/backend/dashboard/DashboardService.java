package com.bms.backend.dashboard;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Collects the dashboard figures, ported from Version 1's DashboardService. The administrator gets
 * everything; an employee gets a limited view (customers, products, low stock, and their OWN recent
 * orders). The choice is made here on the server from the logged-in user, never from anything the
 * browser sends.
 */
@Service
public class DashboardService {

    private static final int LIST_SIZE = 8;

    private final DashboardRepository dashboard;

    public DashboardService(DashboardRepository dashboard) {
        this.dashboard = dashboard;
    }

    @Transactional(readOnly = true)
    public DashboardResponse summary(String username, boolean admin) {
        String symbol = dashboard.currencySymbol();
        long customers = dashboard.countActiveCustomers();
        long products = dashboard.countActiveProducts();
        long lowStockCount = dashboard.countLowStockProducts();
        List<DashboardResponse.LowStockItem> lowStockItems = dashboard.findLowStockItems(LIST_SIZE);

        if (admin) {
            return new DashboardResponse(true, customers, products, dashboard.countActiveEmployees(),
                    dashboard.countCompletedOrders(), dashboard.sumRevenueCents(), lowStockCount, lowStockItems,
                    dashboard.findRecentOrders(LIST_SIZE), dashboard.findRecentCustomers(LIST_SIZE), symbol);
        }

        Long employeeId = dashboard.findEmployeeIdByUsername(username);
        List<DashboardResponse.RecentOrder> myOrders = employeeId == null
                ? List.of()
                : dashboard.findRecentOrdersOf(employeeId, LIST_SIZE);
        return new DashboardResponse(false, customers, products, 0, 0, 0L, lowStockCount, lowStockItems,
                myOrders, List.of(), symbol);
    }
}
