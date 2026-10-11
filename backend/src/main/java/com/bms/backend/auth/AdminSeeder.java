package com.bms.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs once each time the server starts. If there are no users yet, it creates the same two
 * accounts as Version 1 (a default administrator and a demo employee), each with an employee record.
 * If users already exist it does nothing.
 *
 * DEMO PASSWORDS: Admin@123 and Employee@123 are written in the project's documentation.
 * Change them before real use. The administrator password can be set with the environment
 * variable BMS_ADMIN_PASSWORD before the first start.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserAccountRepository users;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final String adminPassword;

    public AdminSeeder(UserAccountRepository users,
                       JdbcTemplate jdbc,
                       PasswordEncoder encoder,
                       @Value("${BMS_ADMIN_PASSWORD:Admin@123}") String adminPassword) {
        this.users = users;
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        createUser("System", "Administrator", "Administrator", "Management", "admin", adminPassword, "ADMIN");
        createUser("Thandi", "Mokoena", "Sales Assistant", "Sales", "thandi", "Employee@123", "EMPLOYEE");
        LOG.warn("No users existed, so the default accounts 'admin' and 'thandi' were created. "
                + "Change their passwords before real use.");
    }

    /** Each login belongs to an employee record, so we create the employee first and link it. */
    private void createUser(String firstName, String lastName, String position, String department,
                            String username, String password, String role) {
        Long employeeId = jdbc.queryForObject(
                "INSERT INTO employees (first_name, last_name, position, department) "
                        + "VALUES (?, ?, ?, ?) RETURNING employee_id",
                Long.class, firstName, lastName, position, department);
        jdbc.update(
                "INSERT INTO users (username, password_hash, role, employee_id) VALUES (?, ?, ?, ?)",
                username, encoder.encode(password), role, employeeId);
    }
}
