package com.bms.backend.inventory;

import com.bms.backend.auth.UserAccountRepository;
import com.bms.backend.common.NotFoundException;
import com.bms.backend.common.TextRules;
import com.bms.backend.common.ValidationException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The rules for inventory, ported from Version 1's InventoryService (same limits, same messages).
 * Viewing stock and history: any logged-in user. Adjusting stock: administrator only (SecurityConfig).
 * Stock can never go below 0, and every change needs a reason and is written to the history.
 *
 * This service uses JdbcTemplate (plain SQL) instead of the Product entity on purpose: the change
 * "add N to the stock, but only if the result is not below 0" must happen in ONE SQL statement so two
 * people adjusting at the same moment can never make the stock wrong.
 */
@Service
public class InventoryService {

    static final int MAX_CHANGE = 100_000;
    static final int MAX_REASON = 200;
    private static final int MAX_SEARCH = 100;
    private static final int HISTORY_LIMIT = 200;
    private static final Set<String> STATUSES = Set.of("IN STOCK", "LOW STOCK", "OUT OF STOCK");

    private static final String GONE = "This product no longer exists. Please refresh the list.";

    private static final String SELECT_ITEM = """
            SELECT product_id, product_name, category, quantity_in_stock, reorder_level,
                   stock_status, supplier_name
            FROM v_inventory
            """;

    private final JdbcTemplate jdbc;
    private final UserAccountRepository users;

    public InventoryService(JdbcTemplate jdbc, UserAccountRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    /** Active products, optionally filtered by text (name, category or supplier) and by stock status. */
    @Transactional(readOnly = true)
    public List<InventoryItem> list(String search, String status) {
        String text = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String st = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (text.length() > MAX_SEARCH) {
            throw new ValidationException("The search text may be at most " + MAX_SEARCH + " characters.");
        }
        if (!st.isEmpty() && !STATUSES.contains(st)) {
            throw new ValidationException("The stock status must be IN STOCK, LOW STOCK or OUT OF STOCK.");
        }
        // position(a in b) is 0 when a is not inside b: "contains" without LIKE's special % and _ characters.
        return jdbc.query(SELECT_ITEM + """
                WHERE (CAST(? AS TEXT) = '' OR stock_status = ?)
                  AND (CAST(? AS TEXT) = ''
                       OR position(? in lower(product_name)) > 0
                       OR position(? in lower(coalesce(category, ''))) > 0
                       OR position(? in lower(coalesce(supplier_name, ''))) > 0)
                ORDER BY lower(product_name)
                """,
                (rs, i) -> toItem(rs),
                st, st, text, text, text, text);
    }

    /** The newest history lines of one product, newest first. */
    @Transactional(readOnly = true)
    public List<HistoryEntry> history(Long productId) {
        requireActiveProduct(productId);
        return jdbc.query("""
                SELECT a.adjusted_at, a.quantity_change, a.reason,
                       coalesce(e.first_name || ' ' || e.last_name, u.username) AS adjusted_by
                FROM stock_adjustments a
                JOIN users u ON u.user_id = a.user_id
                LEFT JOIN employees e ON e.employee_id = u.employee_id
                WHERE a.product_id = ?
                ORDER BY a.adjusted_at DESC, a.adjustment_id DESC
                LIMIT ?
                """,
                (rs, i) -> new HistoryEntry(
                        rs.getTimestamp("adjusted_at").toLocalDateTime(),
                        rs.getInt("quantity_change"),
                        rs.getString("reason"),
                        rs.getString("adjusted_by")),
                productId, HISTORY_LIMIT);
    }

    /** Adds or removes stock and records why. Saves both changes together, or neither. */
    @Transactional
    public InventoryItem adjust(Long productId, AdjustmentRequest request, String username) {
        Integer change = request.change();
        String reason = request.reason() == null ? "" : request.reason().trim();

        if (change == null || change == 0) {
            throw new ValidationException("The quantity must be at least 1.");
        }
        if (Math.abs((long) change) > MAX_CHANGE) {
            throw new ValidationException("You can change the stock by at most " + MAX_CHANGE + " in one go.");
        }
        if (reason.isEmpty()) {
            throw new ValidationException(
                    "Please give a reason (for example: Delivery received, Damaged goods, Stock count).");
        }
        TextRules.requireSingleLine(reason, "reason");
        if (reason.length() > MAX_REASON) {
            throw new ValidationException("The reason may be at most " + MAX_REASON + " characters.");
        }

        Long userId = users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Your user account could not be found."))
                .getUserId();

        // One statement: change the stock only if it will not drop below 0. 0 rows changed = it was refused.
        int changed = jdbc.update("""
                UPDATE products SET quantity_in_stock = quantity_in_stock + ?
                WHERE product_id = ? AND is_active = true AND quantity_in_stock + ? >= 0
                """, change, productId, change);

        if (changed == 0) {
            Integer current = jdbc.query(
                    "SELECT quantity_in_stock FROM products WHERE product_id = ? AND is_active = true",
                    (rs, i) -> rs.getInt(1), productId).stream().findFirst().orElse(null);
            if (current == null) {
                throw new NotFoundException(GONE);
            }
            throw new ValidationException("Only " + current + " in stock, so you cannot remove " + (-change) + ".");
        }

        jdbc.update("""
                INSERT INTO stock_adjustments (product_id, user_id, quantity_change, reason)
                VALUES (?, ?, ?, ?)
                """, productId, userId, change, reason);

        return jdbc.query(SELECT_ITEM + " WHERE product_id = ?", (rs, i) -> toItem(rs), productId)
                .stream().findFirst().orElseThrow(() -> new NotFoundException(GONE));
    }

    // ---------- helpers ----------

    private void requireActiveProduct(Long productId) {
        Integer found = jdbc.queryForObject(
                "SELECT COUNT(*) FROM products WHERE product_id = ? AND is_active = true", Integer.class, productId);
        if (found == null || found == 0) {
            throw new NotFoundException(GONE);
        }
    }

    private InventoryItem toItem(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new InventoryItem(
                rs.getLong("product_id"),
                rs.getString("product_name"),
                rs.getString("category"),
                rs.getInt("quantity_in_stock"),
                rs.getInt("reorder_level"),
                rs.getString("stock_status"),
                rs.getString("supplier_name"));
    }
}
