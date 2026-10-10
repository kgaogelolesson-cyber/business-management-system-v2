-- =====================================================================
-- Business Management System, Version 2: PostgreSQL schema
-- Converted from the SQLite schema of Version 1.
-- Safe to run more than once (CREATE ... IF NOT EXISTS, CREATE OR REPLACE).
-- Run it in pgAdmin: select the "bms" database, open Query Tool, paste, press F5.
--
-- What changed from SQLite, and why:
--   INTEGER PRIMARY KEY AUTOINCREMENT  ->  BIGINT GENERATED ALWAYS AS IDENTITY
--   is_active INTEGER (0/1)            ->  BOOLEAN (TRUE/FALSE)
--   dates stored as TEXT               ->  real types: TIMESTAMP and DATE
--   COLLATE NOCASE on username         ->  unique index on lower(username)
--   datetime('now','localtime')        ->  now()
--   INSERT OR IGNORE                   ->  INSERT ... ON CONFLICT DO NOTHING
-- Money is still stored as whole cents (BIGINT), so there are no rounding errors.
-- =====================================================================

-- ---------- EMPLOYEES ----------
CREATE TABLE IF NOT EXISTS employees (
    employee_id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name   TEXT    NOT NULL CHECK (length(trim(first_name)) > 0),
    last_name    TEXT    NOT NULL CHECK (length(trim(last_name)) > 0),
    email        TEXT,
    phone        TEXT,
    position     TEXT,
    department   TEXT,
    hire_date    DATE,
    is_active    BOOLEAN NOT NULL DEFAULT TRUE
);

-- ---------- USERS (logins) ----------
-- password_hash holds a BCrypt hash in Version 2 (Spring Security). BCrypt keeps its own
-- salt inside the hash, so Version 1's separate "salt" column is no longer needed.
CREATE TABLE IF NOT EXISTS users (
    user_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username       TEXT    NOT NULL CHECK (length(trim(username)) >= 3),
    password_hash  TEXT    NOT NULL,
    role           TEXT    NOT NULL CHECK (role IN ('ADMIN', 'EMPLOYEE')),
    employee_id    BIGINT  UNIQUE REFERENCES employees (employee_id) ON DELETE RESTRICT,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT now()
);
-- "Admin" and "admin" must count as the same username:
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username_lower ON users (lower(username));

-- ---------- CUSTOMERS ----------
CREATE TABLE IF NOT EXISTS customers (
    customer_id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name   TEXT    NOT NULL CHECK (length(trim(first_name)) > 0),
    last_name    TEXT    NOT NULL CHECK (length(trim(last_name)) > 0),
    email        TEXT,
    phone        TEXT,
    address      TEXT,
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);

-- ---------- SUPPLIERS ----------
CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    supplier_name   TEXT    NOT NULL CHECK (length(trim(supplier_name)) > 0),
    contact_person  TEXT,
    email           TEXT,
    phone           TEXT,
    address         TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

-- ---------- PRODUCTS ----------
CREATE TABLE IF NOT EXISTS products (
    product_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_name       TEXT    NOT NULL CHECK (length(trim(product_name)) > 0),
    category           TEXT,
    description        TEXT,
    price_cents        BIGINT  NOT NULL CHECK (price_cents >= 0),
    quantity_in_stock  INTEGER NOT NULL DEFAULT 0 CHECK (quantity_in_stock >= 0),
    reorder_level      INTEGER NOT NULL DEFAULT 5 CHECK (reorder_level >= 0),
    supplier_id        BIGINT  REFERENCES suppliers (supplier_id) ON DELETE RESTRICT,
    is_active          BOOLEAN NOT NULL DEFAULT TRUE
);

-- ---------- ORDERS ----------
CREATE TABLE IF NOT EXISTS orders (
    order_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id         BIGINT  NOT NULL REFERENCES customers (customer_id) ON DELETE RESTRICT,
    employee_id         BIGINT  NOT NULL REFERENCES employees (employee_id) ON DELETE RESTRICT,
    order_date          TIMESTAMP NOT NULL DEFAULT now(),
    status              TEXT    NOT NULL DEFAULT 'COMPLETED'
                                CHECK (status IN ('COMPLETED', 'CANCELLED')),
    total_amount_cents  BIGINT  NOT NULL DEFAULT 0 CHECK (total_amount_cents >= 0),
    cancelled_at        TIMESTAMP,
    cancelled_by        BIGINT  REFERENCES users (user_id) ON DELETE RESTRICT,
    CHECK (
        (status = 'COMPLETED' AND cancelled_at IS NULL     AND cancelled_by IS NULL) OR
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND cancelled_by IS NOT NULL)
    )
);

-- ---------- ORDER ITEMS ----------
CREATE TABLE IF NOT EXISTS order_items (
    order_item_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id          BIGINT  NOT NULL REFERENCES orders (order_id)     ON DELETE CASCADE,
    product_id        BIGINT  NOT NULL REFERENCES products (product_id) ON DELETE RESTRICT,
    quantity          INTEGER NOT NULL CHECK (quantity > 0),
    unit_price_cents  BIGINT  NOT NULL CHECK (unit_price_cents >= 0),
    subtotal_cents    BIGINT  NOT NULL,
    UNIQUE (order_id, product_id),
    CHECK (subtotal_cents = quantity * unit_price_cents)
);

-- ---------- STOCK ADJUSTMENTS (stock history) ----------
CREATE TABLE IF NOT EXISTS stock_adjustments (
    adjustment_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id       BIGINT  NOT NULL REFERENCES products (product_id) ON DELETE RESTRICT,
    user_id          BIGINT  NOT NULL REFERENCES users (user_id)       ON DELETE RESTRICT,
    quantity_change  INTEGER NOT NULL CHECK (quantity_change <> 0),
    reason           TEXT    NOT NULL CHECK (length(trim(reason)) > 0),
    adjusted_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- ---------- SETTINGS ----------
CREATE TABLE IF NOT EXISTS settings (
    setting_key    TEXT PRIMARY KEY,
    setting_value  TEXT NOT NULL
);

-- ---------- INDEXES (speed up searching and joining) ----------
CREATE INDEX IF NOT EXISTS idx_employees_last_name  ON employees (last_name);
CREATE INDEX IF NOT EXISTS idx_employees_department ON employees (department);
CREATE INDEX IF NOT EXISTS idx_customers_last_name  ON customers (last_name);
CREATE INDEX IF NOT EXISTS idx_customers_email      ON customers (email);
CREATE INDEX IF NOT EXISTS idx_customers_phone      ON customers (phone);
CREATE INDEX IF NOT EXISTS idx_suppliers_name       ON suppliers (supplier_name);
CREATE INDEX IF NOT EXISTS idx_products_name        ON products (product_name);
CREATE INDEX IF NOT EXISTS idx_products_category    ON products (category);
CREATE INDEX IF NOT EXISTS idx_products_supplier    ON products (supplier_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer      ON orders (customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_employee      ON orders (employee_id);
CREATE INDEX IF NOT EXISTS idx_orders_date          ON orders (order_date);
CREATE INDEX IF NOT EXISTS idx_orders_status        ON orders (status);
CREATE INDEX IF NOT EXISTS idx_order_items_product  ON order_items (product_id);
CREATE INDEX IF NOT EXISTS idx_stock_adj_product    ON stock_adjustments (product_id);

-- ---------- VIEWS (saved queries) ----------
CREATE OR REPLACE VIEW v_inventory AS
SELECT  p.product_id,
        p.product_name,
        p.category,
        p.quantity_in_stock,
        p.reorder_level,
        CASE
            WHEN p.quantity_in_stock = 0                 THEN 'OUT OF STOCK'
            WHEN p.quantity_in_stock <= p.reorder_level  THEN 'LOW STOCK'
            ELSE 'IN STOCK'
        END AS stock_status,
        s.supplier_name
FROM    products p
LEFT JOIN suppliers s ON s.supplier_id = p.supplier_id
WHERE   p.is_active;

CREATE OR REPLACE VIEW v_order_summary AS
SELECT  o.order_id,
        o.order_date,
        o.status,
        o.total_amount_cents,
        c.customer_id,
        c.first_name || ' ' || c.last_name AS customer_name,
        e.employee_id,
        e.first_name || ' ' || e.last_name AS employee_name
FROM    orders o
JOIN    customers c ON c.customer_id = o.customer_id
JOIN    employees e ON e.employee_id = o.employee_id;

CREATE OR REPLACE VIEW v_order_item_details AS
SELECT  oi.order_item_id,
        oi.order_id,
        oi.product_id,
        p.product_name,
        oi.quantity,
        oi.unit_price_cents,
        oi.subtotal_cents
FROM    order_items oi
JOIN    products p ON p.product_id = oi.product_id;

-- ---------- DEFAULT SETTINGS ----------
INSERT INTO settings (setting_key, setting_value) VALUES
    ('business_name', 'My Business'),
    ('currency_symbol', 'R'),
    ('default_low_stock_level', '5')
ON CONFLICT (setting_key) DO NOTHING;
