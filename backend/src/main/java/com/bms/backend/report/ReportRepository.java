package com.bms.backend.report;

import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * All SQL for reports lives here. Every report is a read-only SELECT.
 * Dates arrive as yyyy-MM-dd text and are turned into dates inside SQL with CAST, and the whole
 * day counts: an order at 23:59 on the "to" date is included.
 * The columns argument says how to read each column: 'T' = text, 'N' = whole number (also money cents).
 */
@Repository
public class ReportRepository {

    private static final String DATE_RANGE =
            "CAST(o.order_date AS DATE) BETWEEN CAST(? AS DATE) AND CAST(? AS DATE)";

    private final JdbcTemplate jdbc;

    public ReportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String currencySymbol() {
        List<String> found = jdbc.queryForList(
                "SELECT setting_value FROM settings WHERE setting_key = 'currency_symbol'", String.class);
        return found.isEmpty() || found.get(0).isBlank() ? "R" : found.get(0);
    }

    public List<List<Object>> salesByProduct(String from, String to) {
        return run("""
                SELECT p.product_name, SUM(oi.quantity) AS units, SUM(oi.subtotal_cents) AS revenue
                FROM order_items oi
                JOIN orders o ON o.order_id = oi.order_id
                JOIN products p ON p.product_id = oi.product_id
                WHERE o.status = 'COMPLETED' AND %s
                GROUP BY p.product_id, p.product_name
                ORDER BY revenue DESC, lower(p.product_name)
                """.formatted(DATE_RANGE), "TNN", from, to);
    }

    public List<List<Object>> salesByEmployee(String from, String to) {
        return run("""
                SELECT e.first_name || ' ' || e.last_name AS employee, COUNT(*) AS orders,
                       SUM(o.total_amount_cents) AS revenue
                FROM orders o JOIN employees e ON e.employee_id = o.employee_id
                WHERE o.status = 'COMPLETED' AND %s
                GROUP BY e.employee_id, e.first_name, e.last_name
                ORDER BY revenue DESC, lower(e.first_name || ' ' || e.last_name)
                """.formatted(DATE_RANGE), "TNN", from, to);
    }

    public List<List<Object>> salesByCustomer(String from, String to) {
        return run("""
                SELECT c.first_name || ' ' || c.last_name AS customer, COUNT(*) AS orders,
                       SUM(o.total_amount_cents) AS revenue
                FROM orders o JOIN customers c ON c.customer_id = o.customer_id
                WHERE o.status = 'COMPLETED' AND %s
                GROUP BY c.customer_id, c.first_name, c.last_name
                ORDER BY revenue DESC, lower(c.first_name || ' ' || c.last_name)
                """.formatted(DATE_RANGE), "TNN", from, to);
    }

    public List<List<Object>> salesByDay(String from, String to) {
        return run("""
                SELECT to_char(CAST(o.order_date AS DATE), 'YYYY-MM-DD') AS day, COUNT(*) AS orders,
                       SUM(o.total_amount_cents) AS revenue
                FROM orders o
                WHERE o.status = 'COMPLETED' AND %s
                GROUP BY 1
                ORDER BY 1
                """.formatted(DATE_RANGE), "TNN", from, to);
    }

    /** { completed orders, revenue in cents, cancelled orders } inside the date range. */
    public long[] salesTotals(String from, String to) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(CASE WHEN o.status = 'COMPLETED' THEN 1 ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN o.status = 'COMPLETED' THEN o.total_amount_cents ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN o.status = 'CANCELLED' THEN 1 ELSE 0 END), 0)
                FROM orders o WHERE %s
                """.formatted(DATE_RANGE),
                (rs, row) -> new long[]{rs.getLong(1), rs.getLong(2), rs.getLong(3)}, from, to);
    }

    /** Every active product with its stock and the value of that stock (quantity x price). */
    public List<List<Object>> stockLevels() {
        return run("""
                SELECT p.product_name, coalesce(p.category, ''), p.quantity_in_stock, p.reorder_level,
                       v.stock_status, p.price_cents, p.quantity_in_stock * p.price_cents AS stock_value
                FROM products p JOIN v_inventory v ON v.product_id = p.product_id
                ORDER BY lower(p.product_name)
                """, "TTNNTNN");
    }

    /** Active products at or below their reorder level (including 0), lowest stock first, with the supplier. */
    public List<List<Object>> lowStock() {
        return run("""
                SELECT product_name, coalesce(category, ''), quantity_in_stock, reorder_level,
                       stock_status, coalesce(supplier_name, '')
                FROM v_inventory
                WHERE stock_status IN ('LOW STOCK', 'OUT OF STOCK')
                ORDER BY quantity_in_stock ASC, lower(product_name)
                """, "TTNNTT");
    }

    private List<List<Object>> run(String sql, String columns, Object... args) {
        return jdbc.query(sql, (rs, rowNumber) -> {
            List<Object> row = new ArrayList<>();
            for (int i = 0; i < columns.length(); i++) {
                row.add(columns.charAt(i) == 'T' ? rs.getString(i + 1) : (Object) rs.getLong(i + 1));
            }
            return row;
        }, args);
    }
}
