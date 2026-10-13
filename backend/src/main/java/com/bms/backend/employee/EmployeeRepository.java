package com.bms.backend.employee;

import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Plain SQL (JdbcTemplate) because every employee screen row joins two tables:
 * employees (the person) and users (their login).
 * We never pass null into a comparison (PostgreSQL cannot always guess its type), so the search
 * builds its SQL from only the filters that are actually used.
 */
@Repository
public class EmployeeRepository {

    private static final String SELECT = """
            SELECT e.employee_id, e.first_name, e.last_name, e.email, e.phone, e.position, e.department,
                   to_char(e.hire_date, 'YYYY-MM-DD') AS hire_date, e.is_active, u.username, u.role
            FROM employees e
            LEFT JOIN users u ON u.employee_id = e.employee_id
            """;

    private static final RowMapper<EmployeeResponse> MAPPER = (rs, row) -> new EmployeeResponse(
            rs.getLong("employee_id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("position"),
            rs.getString("department"),
            rs.getString("hire_date"),
            rs.getBoolean("is_active"),
            rs.getString("username"),
            rs.getString("role"));

    private final JdbcTemplate jdbc;

    public EmployeeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** text and department are already lower-case; empty text / department mean "no filter". */
    public List<EmployeeResponse> search(String text, boolean includeInactive, String department) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE true");
        List<Object> args = new ArrayList<>();
        if (!includeInactive) {
            sql.append(" AND e.is_active = true");
        }
        if (!department.isEmpty()) {
            sql.append(" AND lower(coalesce(e.department, '')) = ?");
            args.add(department);
        }
        if (!text.isEmpty()) {
            // position(a in b) = "where does a occur inside b?" (0 = nowhere): "contains" without LIKE wildcards.
            sql.append("""
                     AND (   position(? in lower(e.first_name || ' ' || e.last_name)) > 0
                          OR position(? in lower(coalesce(u.username, ''))) > 0
                          OR position(? in lower(coalesce(e.email, ''))) > 0
                          OR position(? in coalesce(e.phone, '')) > 0
                          OR position(? in lower(coalesce(e.position, ''))) > 0
                          OR position(? in lower(coalesce(e.department, ''))) > 0)
                    """);
            for (int i = 0; i < 6; i++) {
                args.add(text);
            }
        }
        sql.append(" ORDER BY e.is_active DESC, lower(e.last_name), lower(e.first_name), e.employee_id");
        return jdbc.query(sql.toString(), MAPPER, args.toArray());
    }

    public EmployeeResponse findById(long id) {
        List<EmployeeResponse> found = jdbc.query(SELECT + " WHERE e.employee_id = ?", MAPPER, id);
        return found.isEmpty() ? null : found.get(0);
    }

    public List<String> findDepartments() {
        return jdbc.queryForList("""
                SELECT DISTINCT department FROM employees
                WHERE is_active = true AND department IS NOT NULL ORDER BY department
                """, String.class);
    }

    /** Is another ACTIVE employee already using this email? Pass excludeId 0 when adding. */
    public boolean emailInUse(String email, long excludeId) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM employees
                WHERE is_active = true AND lower(email) = lower(?) AND employee_id <> ?
                """, Integer.class, email, excludeId);
        return n != null && n > 0;
    }

    /** "Admin" and "admin" count as the same username, and deactivated logins still hold their name. */
    public boolean usernameInUse(String username) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE lower(username) = lower(?)", Integer.class, username);
        return n != null && n > 0;
    }

    /** The employee id that belongs to a login name, or null (a login with no employee record). */
    public Long findEmployeeIdByUsername(String username) {
        List<Long> ids = jdbc.queryForList(
                "SELECT employee_id FROM users WHERE lower(username) = lower(?) AND employee_id IS NOT NULL",
                Long.class, username);
        return ids.isEmpty() ? null : ids.get(0);
    }

    /** Adds the employee row and returns its new id. hireDate is yyyy-MM-dd text or null. */
    public long insertEmployee(String firstName, String lastName, String email, String phone,
                               String position, String department, String hireDate) {
        Long id = jdbc.queryForObject("""
                INSERT INTO employees (first_name, last_name, email, phone, position, department, hire_date)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS DATE))
                RETURNING employee_id
                """, Long.class, firstName, lastName, email, phone, position, department, hireDate);
        return id;
    }

    public void insertUser(String username, String passwordHash, String role, long employeeId) {
        jdbc.update("INSERT INTO users (username, password_hash, role, employee_id) VALUES (?, ?, ?, ?)",
                username, passwordHash, role, employeeId);
    }

    /** Changes an ACTIVE employee; false if no such active employee. */
    public boolean updateEmployee(long id, String firstName, String lastName, String email, String phone,
                                  String position, String department, String hireDate) {
        return jdbc.update("""
                UPDATE employees SET first_name = ?, last_name = ?, email = ?, phone = ?,
                       position = ?, department = ?, hire_date = CAST(? AS DATE)
                WHERE employee_id = ? AND is_active = true
                """, firstName, lastName, email, phone, position, department, hireDate, id) > 0;
    }

    public void updateRole(long employeeId, String role) {
        jdbc.update("UPDATE users SET role = ? WHERE employee_id = ?", role, employeeId);
    }

    /** Switches the employee AND their login on or off together. false if the employee was not in the other state. */
    public boolean setActive(long employeeId, boolean active) {
        int changed = jdbc.update("UPDATE employees SET is_active = ? WHERE employee_id = ? AND is_active = ?",
                active, employeeId, !active);
        if (changed > 0) {
            jdbc.update("UPDATE users SET is_active = ? WHERE employee_id = ?", active, employeeId);
        }
        return changed > 0;
    }

    public void updatePassword(long employeeId, String passwordHash) {
        jdbc.update("UPDATE users SET password_hash = ? WHERE employee_id = ?", passwordHash, employeeId);
    }
}
