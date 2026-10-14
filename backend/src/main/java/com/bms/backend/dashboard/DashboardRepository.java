package com.bms.backend.dashboard;

import com.bms.backend.dashboard.DashboardResponse.LowStockItem;
import com.bms.backend.dashboard.DashboardResponse.RecentCustomer;
import com.bms.backend.dashboard.DashboardResponse.RecentOrder;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The SQL behind the dashboard. Nothing is hard-coded: it all comes from the tables. */
@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbc;

    public DashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String currencySymbol() {
        List<String> found = jdbc.queryForList(
                "SELECT setting_value FROM settings WHERE setting_key = 'currency_symbol'", String.class);
        return found.isEmpty() || found.get(0).isBlank() ? "R" : found.get(0);
    }

    public long countActiveCustomers() {
        return count("SELECT COUNT(*) FROM customers WHERE is_active = true");
    }

    public long countActiveProducts() {
        return count("SELECT COUNT(*) FROM products WHERE is_active = true");
    }

    public long countActiveEmployees() {
        return count("SELECT COUNT(*) FROM employees WHERE is_active = true");
    }

    /** Cancelled orders are not counted. */
    public long countCompletedOrders() {
        return count("SELECT COUNT(*) FROM orders WHERE status = 'COMPLETED'");
    }

    /** Cancelled orders are not counted. Returns cents. */
    public long sumRevenueCents() {
        return count("SELECT CAST(COALESCE(SUM(total_amount_cents), 0) AS BIGINT) FROM orders WHERE status = 'COMPLETED'");
    }

    /** Products that are LOW STOCK or OUT OF STOCK (the view decides the status). */
    public long countLowStockProducts() {
        return count("SELECT COUNT(*) FROM v_inventory WHERE stock_status <> 'IN STOCK'");
    }

    public List<LowStockItem> findLowStockItems(int limit) {
        return jdbc.query("""
                SELECT product_name, quantity_in_stock, reorder_level, stock_status
                FROM v_inventory WHERE stock_status <> 'IN STOCK'
                ORDER BY quantity_in_stock ASC, lower(product_name) ASC LIMIT ?
                """,
                (rs, row) -> new LowStockItem(rs.getString("product_name"), rs.getInt("quantity_in_stock"),
                        rs.getInt("reorder_level"), rs.getString("stock_status")),
                limit);
    }

    /** Newest orders first, for everyone. */
    public List<RecentOrder> findRecentOrders(int limit) {
        return jdbc.query(RECENT_ORDERS + " ORDER BY order_date DESC, order_id DESC LIMIT ?",
                (rs, row) -> toRecentOrder(rs), limit);
    }

    /** Newest orders first, only the orders of one employee. */
    public List<RecentOrder> findRecentOrdersOf(long employeeId, int limit) {
        return jdbc.query(RECENT_ORDERS + " WHERE employee_id = ? ORDER BY order_date DESC, order_id DESC LIMIT ?",
                (rs, row) -> toRecentOrder(rs), employeeId, limit);
    }

    public List<RecentCustomer> findRecentCustomers(int limit) {
        return jdbc.query("""
                SELECT first_name || ' ' || last_name AS customer_name, email,
                       to_char(created_at, 'YYYY-MM-DD') AS created
                FROM customers WHERE is_active = true
                ORDER BY created_at DESC, customer_id DESC LIMIT ?
                """,
                (rs, row) -> new RecentCustomer(rs.getString("customer_name"), rs.getString("email"),
                        rs.getString("created")),
                limit);
    }

    /** The employee record that belongs to a login name, or null. */
    public Long findEmployeeIdByUsername(String username) {
        List<Long> ids = jdbc.queryForList(
                "SELECT employee_id FROM users WHERE lower(username) = lower(?) AND employee_id IS NOT NULL",
                Long.class, username);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private static final String RECENT_ORDERS = """
            SELECT order_id, to_char(order_date, 'YYYY-MM-DD HH24:MI') AS ordered, customer_name,
                   employee_name, total_amount_cents, status
            FROM v_order_summary
            """;

    private RecentOrder toRecentOrder(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new RecentOrder(rs.getLong("order_id"), rs.getString("ordered"), rs.getString("customer_name"),
                rs.getString("employee_name"), rs.getLong("total_amount_cents"), rs.getString("status"));
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }
}
