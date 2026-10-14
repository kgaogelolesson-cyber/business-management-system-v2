package com.bms.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base class for tests that use the real PostgreSQL test database (bms_test).
 *
 * Before EVERY test it empties the tables, so each test starts from a clean, known state.
 * Emptying tables is dangerous, so the first thing it does is check it is connected to bms_test
 * and stops immediately if not.
 */
@SpringBootTest
public abstract class DatabaseTestBase {

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        String database = jdbc.queryForObject("SELECT current_database()", String.class);
        assertThat(database).as("Tests may only run against the bms_test database").isEqualTo("bms_test");

        jdbc.execute("TRUNCATE TABLE stock_adjustments, order_items, orders, products, suppliers, "
                + "customers, users, employees RESTART IDENTITY CASCADE");
    }

    // ---------- helpers that create small test data ----------

    /** Creates an employee and a login for them. Returns the employee id. */
    protected long createUser(String username, String role) {
        Long employeeId = jdbc.queryForObject(
                "INSERT INTO employees (first_name, last_name) VALUES (?, ?) RETURNING employee_id",
                Long.class, username, "Test");
        jdbc.update("INSERT INTO users (username, password_hash, role, employee_id) VALUES (?, ?, ?, ?)",
                username, "not-a-real-hash", role, employeeId);
        return employeeId;
    }

    /** Creates a customer. Returns the customer id. */
    protected long createCustomer() {
        return jdbc.queryForObject(
                "INSERT INTO customers (first_name, last_name) VALUES ('Test', 'Customer') RETURNING customer_id",
                Long.class);
    }

    /** Creates a product with the given price (in cents) and stock. Returns the product id. */
    protected long createProduct(String name, long priceCents, int stock) {
        return jdbc.queryForObject(
                "INSERT INTO products (product_name, price_cents, quantity_in_stock) VALUES (?, ?, ?) "
                        + "RETURNING product_id",
                Long.class, name, priceCents, stock);
    }

    protected int stockOf(long productId) {
        return jdbc.queryForObject("SELECT quantity_in_stock FROM products WHERE product_id = ?",
                Integer.class, productId);
    }

    protected int countRows(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
