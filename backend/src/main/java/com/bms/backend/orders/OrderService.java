package com.bms.backend.orders;

import com.bms.backend.auth.UserAccount;
import com.bms.backend.auth.UserAccountRepository;
import com.bms.backend.common.ForbiddenException;
import com.bms.backend.common.NotFoundException;
import com.bms.backend.common.ValidationException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The rules for orders, ported from Version 1's OrderService (same limits, same messages).
 *  - Any logged-in user with an employee record may create an order; it is recorded against THAT employee.
 *  - An administrator sees every order. An employee sees only their own.
 *  - Creating an order reduces the stock and writes stock-history lines, all in ONE transaction:
 *    if anything fails, nothing is saved.
 *  - Only an administrator may cancel an order. Cancelling puts the stock back (also in one transaction).
 */
@Service
public class OrderService {

    static final int MAX_LINES = 50;
    static final int MAX_QUANTITY = 10_000;
    private static final int LIST_LIMIT = 1000;
    private static final int MAX_SEARCH = 100;
    private static final Set<String> STATUSES = Set.of("COMPLETED", "CANCELLED");

    private static final String GONE = "This order no longer exists. Please refresh the list.";

    private static final String SELECT_SUMMARY = """
            SELECT order_id, order_date, status, total_amount_cents, customer_id, customer_name,
                   employee_id, employee_name
            FROM v_order_summary
            """;

    private final JdbcTemplate jdbc;
    private final UserAccountRepository users;

    public OrderService(JdbcTemplate jdbc, UserAccountRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    // ---------- reading ----------

    /**
     * Orders matching the optional filters, newest first (at most 1000). An employee only gets their own
     * orders, whatever filters are used. from/to are yyyy-MM-dd and both ends are included.
     */
    @Transactional(readOnly = true)
    public List<OrderSummary> list(String search, String status, String from, String to, Long customerId,
                                   String username, boolean admin) {
        String text = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String st = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (text.length() > MAX_SEARCH) {
            throw new ValidationException("The search text may be at most " + MAX_SEARCH + " characters.");
        }
        if (!st.isEmpty() && !STATUSES.contains(st)) {
            throw new ValidationException("The order status must be COMPLETED or CANCELLED.");
        }
        LocalDate fromDate = parseOptionalDate(from, "From");
        LocalDate toDate = parseOptionalDate(to, "To");
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ValidationException("The From date cannot be after the To date.");
        }

        // The query is built piece by piece, adding a condition only when a filter is used. That way we never
        // pass an empty (null) value into SQL, which PostgreSQL cannot always type. Values are still bound as ?.
        StringBuilder sql = new StringBuilder(SELECT_SUMMARY).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (!admin) {
            Long myEmployeeId = employeeIdOf(username);
            if (myEmployeeId == null) {
                return List.of(); // an account without an employee record has no orders
            }
            sql.append(" AND employee_id = ?");
            args.add(myEmployeeId);
        }
        if (!st.isEmpty()) {
            sql.append(" AND status = ?");
            args.add(st);
        }
        if (customerId != null) {
            sql.append(" AND customer_id = ?");
            args.add(customerId);
        }
        if (fromDate != null) {
            sql.append(" AND order_date >= ?");
            args.add(java.sql.Timestamp.valueOf(fromDate.atStartOfDay()));
        }
        if (toDate != null) {
            sql.append(" AND order_date < ?");
            args.add(java.sql.Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
        }
        if (!text.isEmpty()) {
            // position(a in b) is 0 when a is not inside b: "contains" without LIKE's special % and _ characters.
            sql.append("""
                     AND (position(? in cast(order_id as text)) > 0
                          OR position(? in to_char(order_date, 'YYYY-MM-DD HH24:MI')) > 0
                          OR position(? in lower(customer_name)) > 0
                          OR position(? in lower(employee_name)) > 0)
                    """);
            for (int i = 0; i < 4; i++) {
                args.add(text);
            }
        }
        sql.append(" ORDER BY order_date DESC, order_id DESC LIMIT ").append(LIST_LIMIT);
        return jdbc.query(sql.toString(), (rs, i) -> toSummary(rs), args.toArray());
    }

    /** One order with its lines. An employee may only open their own orders. */
    @Transactional(readOnly = true)
    public OrderDetail get(Long orderId, String username, boolean admin) {
        OrderSummary order = jdbc.query(SELECT_SUMMARY + " WHERE order_id = ?", (rs, i) -> toSummary(rs), orderId)
                .stream().findFirst().orElseThrow(() -> new NotFoundException(GONE));
        requireMayView(order, username, admin);

        List<OrderLineResponse> lines = jdbc.query("""
                SELECT product_id, product_name, quantity, unit_price_cents, subtotal_cents
                FROM v_order_item_details WHERE order_id = ? ORDER BY order_item_id
                """,
                (rs, i) -> new OrderLineResponse(rs.getLong("product_id"), rs.getString("product_name"),
                        rs.getInt("quantity"), rs.getLong("unit_price_cents"), rs.getLong("subtotal_cents")),
                orderId);

        String cancellation = jdbc.query("""
                SELECT to_char(o.cancelled_at, 'YYYY-MM-DD HH24:MI') AS at,
                       coalesce(e.first_name || ' ' || e.last_name, u.username) AS who
                FROM orders o
                JOIN users u ON u.user_id = o.cancelled_by
                LEFT JOIN employees e ON e.employee_id = u.employee_id
                WHERE o.order_id = ? AND o.status = 'CANCELLED'
                """,
                (rs, i) -> "Cancelled " + rs.getString("at") + " by " + rs.getString("who"), orderId)
                .stream().findFirst().orElse(null);

        return new OrderDetail(order, lines, cancellation);
    }

    // ---------- creating ----------

    /** Validates and saves a NEW order. The whole method is one transaction: all of it is saved, or none. */
    @Transactional
    public OrderDetail create(OrderRequest request, String username, boolean admin) {
        Long employeeId = employeeIdOf(username);
        if (employeeId == null) {
            throw new ValidationException(
                    "Your account is not linked to an employee record, so you cannot create orders.");
        }
        Long userId = users.findByUsernameIgnoreCase(username).map(UserAccount::getUserId).orElseThrow();

        Long customerId = request.customerId();
        Integer customers = customerId == null ? 0 : jdbc.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE customer_id = ? AND is_active = true",
                Integer.class, customerId);
        if (customers == null || customers == 0) {
            throw new ValidationException("Please choose a customer. (The customer may have been deleted.)");
        }

        List<OrderRequest.Line> lines = request.lines();
        if (lines == null || lines.isEmpty()) {
            throw new ValidationException("Add at least one product to the order.");
        }
        if (lines.size() > MAX_LINES) {
            throw new ValidationException("An order may have at most " + MAX_LINES + " different products.");
        }
        Set<Long> seen = new HashSet<>();
        for (OrderRequest.Line line : lines) {
            if (line == null || line.productId() == null || !seen.add(line.productId())) {
                throw new ValidationException(
                        "The same product is on the order twice. Combine them into one line.");
            }
            if (line.quantity() == null || line.quantity() < 1) {
                throw new ValidationException("Every quantity must be at least 1.");
            }
            if (line.quantity() > MAX_QUANTITY) {
                throw new ValidationException("A quantity may be at most " + MAX_QUANTITY + ".");
            }
        }

        Long orderId = jdbc.queryForObject(
                "INSERT INTO orders (customer_id, employee_id) VALUES (?, ?) RETURNING order_id",
                Long.class, customerId, employeeId);

        long total = 0;
        for (OrderRequest.Line line : lines) {
            Long productId = line.productId();
            int quantity = line.quantity();

            // The price and name come from the database, never from the browser.
            var product = jdbc.query(
                    "SELECT product_name, price_cents FROM products WHERE product_id = ? AND is_active = true",
                    (rs, i) -> new Object[] {rs.getString(1), rs.getLong(2)}, productId)
                    .stream().findFirst().orElse(null);
            if (product == null) {
                throw new ValidationException(
                        "A product on the order no longer exists. Please remove it and try again.");
            }
            String name = (String) product[0];
            long price = (Long) product[1];

            // One statement: take the stock only if enough is there. 0 rows = not enough (or two people at once).
            int taken = jdbc.update("""
                    UPDATE products SET quantity_in_stock = quantity_in_stock - ?
                    WHERE product_id = ? AND is_active = true AND quantity_in_stock >= ?
                    """, quantity, productId, quantity);
            if (taken != 1) {
                Integer stock = jdbc.queryForObject(
                        "SELECT quantity_in_stock FROM products WHERE product_id = ?", Integer.class, productId);
                throw new ValidationException("Only " + stock + " in stock for \"" + name
                        + "\" (you asked for " + quantity + ").");
            }

            long subtotal = quantity * price;
            jdbc.update("""
                    INSERT INTO order_items (order_id, product_id, quantity, unit_price_cents, subtotal_cents)
                    VALUES (?, ?, ?, ?, ?)
                    """, orderId, productId, quantity, price, subtotal);
            jdbc.update("""
                    INSERT INTO stock_adjustments (product_id, user_id, quantity_change, reason)
                    VALUES (?, ?, ?, ?)
                    """, productId, userId, -quantity, "Sold on order #" + orderId);
            total += subtotal;
        }

        jdbc.update("UPDATE orders SET total_amount_cents = ? WHERE order_id = ?", total, orderId);
        return get(orderId, username, admin);
    }

    // ---------- cancelling (administrator only: enforced in SecurityConfig) ----------

    /**
     * Cancels a COMPLETED order and puts its products back into stock, writing a stock-history line for
     * each. A cancelled order stays in the list for the records. One transaction: all of it, or none.
     */
    @Transactional
    public OrderDetail cancel(Long orderId, String username) {
        String status = jdbc.query("SELECT status FROM orders WHERE order_id = ?",
                        (rs, i) -> rs.getString(1), orderId)
                .stream().findFirst().orElseThrow(() -> new NotFoundException(GONE));
        if ("CANCELLED".equals(status)) {
            throw new ValidationException("Order #" + orderId + " is already cancelled.");
        }
        Long userId = users.findByUsernameIgnoreCase(username)
                .map(UserAccount::getUserId)
                .orElseThrow(() -> new NotFoundException("Your user account could not be found."));

        // One statement that only works while the order is still COMPLETED, so two people cancelling at the
        // same moment cannot both put the stock back.
        int changed = jdbc.update("""
                UPDATE orders SET status = 'CANCELLED', cancelled_at = now(), cancelled_by = ?
                WHERE order_id = ? AND status = 'COMPLETED'
                """, userId, orderId);
        if (changed != 1) {
            throw new ValidationException(
                    "Order #" + orderId + " could not be cancelled. Please refresh the list.");
        }

        List<long[]> lines = jdbc.query(
                "SELECT product_id, quantity FROM order_items WHERE order_id = ? ORDER BY order_item_id",
                (rs, i) -> new long[] {rs.getLong(1), rs.getLong(2)}, orderId);
        for (long[] line : lines) {
            jdbc.update("UPDATE products SET quantity_in_stock = quantity_in_stock + ? WHERE product_id = ?",
                    line[1], line[0]);
            jdbc.update("""
                    INSERT INTO stock_adjustments (product_id, user_id, quantity_change, reason)
                    VALUES (?, ?, ?, ?)
                    """, line[0], userId, line[1], "Order #" + orderId + " cancelled");
        }
        return get(orderId, username, true);
    }

    // ---------- helpers ----------

    private void requireMayView(OrderSummary order, String username, boolean admin) {
        if (admin) {
            return;
        }
        Long mine = employeeIdOf(username);
        if (mine == null || !mine.equals(order.employeeId())) {
            throw new ForbiddenException("You can only view your own orders.");
        }
    }

    private Long employeeIdOf(String username) {
        return users.findByUsernameIgnoreCase(username).map(UserAccount::getEmployeeId).orElse(null);
    }

    private LocalDate parseOptionalDate(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("The " + fieldName
                    + " date must be a real date written as yyyy-MM-dd (example: 2026-10-01).");
        }
    }

    private OrderSummary toSummary(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new OrderSummary(
                rs.getLong("order_id"),
                rs.getTimestamp("order_date").toLocalDateTime(),
                rs.getString("status"),
                rs.getLong("total_amount_cents"),
                rs.getLong("customer_id"),
                rs.getString("customer_name"),
                rs.getLong("employee_id"),
                rs.getString("employee_name"));
    }
}
