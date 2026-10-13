package com.bms.backend.employee;

import com.bms.backend.common.NotFoundException;
import com.bms.backend.common.TextRules;
import com.bms.backend.common.ValidationException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The rules for employees and their login accounts, ported from Version 1's EmployeeService
 * (same limits, same messages). "Administrator only" is enforced in SecurityConfig.
 *
 * An employee is never deleted. "Deactivate" hides the employee AND blocks their login but keeps
 * the row, so old orders that mention them stay correct.
 * An administrator cannot deactivate or change the role of their OWN account, so at least one
 * active administrator always remains.
 */
@Service
public class EmployeeService {

    private static final int MAX_NAME = 50;
    private static final int MAX_EMAIL = 100;
    private static final int MAX_TEXT = 100;
    private static final int MAX_SEARCH = 100;
    private static final int MIN_USERNAME = 3;
    private static final int MAX_USERNAME = 30;
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 64;

    private static final Pattern NAME = Pattern.compile("^\\p{L}[\\p{L} .'-]*$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]+$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_CHARACTERS = Pattern.compile("^[0-9 +()-]+$");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final LocalDate EARLIEST_HIRE_DATE = LocalDate.of(1950, 1, 1);

    private static final String GONE =
            "This employee no longer exists or is deactivated. Please refresh the list.";

    private final EmployeeRepository employees;
    private final PasswordEncoder encoder;

    public EmployeeService(EmployeeRepository employees, PasswordEncoder encoder) {
        this.employees = employees;
        this.encoder = encoder;
    }

    // ---------- reading ----------

    @Transactional(readOnly = true)
    public List<EmployeeResponse> list(String search, boolean includeInactive, String department) {
        String text = search == null ? "" : search.trim();
        String dept = department == null ? "" : department.trim();
        if (text.length() > MAX_SEARCH || dept.length() > MAX_SEARCH) {
            throw new ValidationException("The search text may be at most " + MAX_SEARCH + " characters.");
        }
        return employees.search(text.toLowerCase(Locale.ROOT), includeInactive, dept.toLowerCase(Locale.ROOT));
    }

    @Transactional(readOnly = true)
    public List<String> departments() {
        return employees.findDepartments();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(long id) {
        EmployeeResponse found = employees.findById(id);
        if (found == null) {
            throw new NotFoundException(GONE);
        }
        return found;
    }

    // ---------- adding and editing ----------

    /** Adds the employee and their login together, or neither. */
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        Cleaned c = clean(request);
        checkPerson(c, 0L);
        checkUsername(c.username());
        if (employees.usernameInUse(c.username())) {
            throw new ValidationException("The username \"" + c.username() + "\" is already taken.");
        }
        checkPassword(request.password(), request.confirmPassword(), c.username());

        long id = employees.insertEmployee(c.firstName(), c.lastName(), c.email(), c.phone(),
                c.position(), c.department(), c.hireDate());
        employees.insertUser(c.username(), encoder.encode(request.password()), c.role(), id);
        return get(id);
    }

    /** Changes the person's details and role. The username and password are not changed here. */
    @Transactional
    public EmployeeResponse update(long id, EmployeeRequest request, String currentUsername) {
        Cleaned c = clean(request);
        EmployeeResponse existing = employees.findById(id);
        if (existing == null || !existing.active()) {
            throw new NotFoundException(GONE);
        }
        checkPerson(c, id);
        if (existing.username() == null) {
            throw new ValidationException("This employee has no login account, so a role cannot be set.");
        }
        if (!existing.role().equals(c.role()) && isCurrentUser(id, currentUsername)) {
            throw new ValidationException("You cannot change your own role. Ask another administrator.");
        }
        if (!employees.updateEmployee(id, c.firstName(), c.lastName(), c.email(), c.phone(),
                c.position(), c.department(), c.hireDate())) {
            throw new NotFoundException(GONE);
        }
        employees.updateRole(id, c.role());
        return get(id);
    }

    // ---------- deactivate / reactivate / password ----------

    @Transactional
    public void deactivate(long id, String currentUsername) {
        if (isCurrentUser(id, currentUsername)) {
            throw new ValidationException("You cannot deactivate your own account.");
        }
        if (!employees.setActive(id, false)) {
            throw new NotFoundException(
                    "This employee no longer exists or is already deactivated. Please refresh the list.");
        }
    }

    @Transactional
    public void reactivate(long id) {
        EmployeeResponse existing = employees.findById(id);
        if (existing == null || existing.active()) {
            throw new NotFoundException(
                    "This employee no longer exists or is already active. Please refresh the list.");
        }
        if (existing.email() != null && employees.emailInUse(existing.email(), id)) {
            throw new ValidationException("Another active employee now uses the email " + existing.email()
                    + ". Change that employee's email first.");
        }
        if (!employees.setActive(id, true)) {
            throw new NotFoundException("This employee could not be reactivated. Please refresh the list.");
        }
    }

    @Transactional
    public void resetPassword(long id, PasswordRequest request) {
        EmployeeResponse existing = employees.findById(id);
        if (existing == null || !existing.active()) {
            throw new NotFoundException(GONE);
        }
        if (existing.username() == null) {
            throw new ValidationException("This employee has no login account.");
        }
        checkPassword(request.newPassword(), request.confirmPassword(), existing.username());
        employees.updatePassword(id, encoder.encode(request.newPassword()));
    }

    // ---------- helpers ----------

    private boolean isCurrentUser(long employeeId, String currentUsername) {
        Long mine = employees.findEmployeeIdByUsername(currentUsername);
        return mine != null && mine == employeeId;
    }

    /** The cleaned-up values: trimmed, empty optional fields become null. role is "ADMIN", "EMPLOYEE" or null. */
    private record Cleaned(String firstName, String lastName, String email, String phone, String position,
                           String department, String hireDate, String username, String role) {
    }

    private Cleaned clean(EmployeeRequest r) {
        String role = trim(r.role()).toUpperCase(Locale.ROOT);
        return new Cleaned(trim(r.firstName()), trim(r.lastName()), blankToNull(r.email()), blankToNull(r.phone()),
                blankToNull(r.position()), blankToNull(r.department()), blankToNull(r.hireDate()),
                trim(r.username()), role.equals("ADMIN") || role.equals("EMPLOYEE") ? role : null);
    }

    /** The rules about the person (used for both adding and changing). */
    private void checkPerson(Cleaned c, long excludeId) {
        checkName(c.firstName(), "first name");
        checkName(c.lastName(), "last name");
        if (c.email() != null) {
            if (c.email().length() > MAX_EMAIL || !EMAIL.matcher(c.email()).matches()) {
                throw new ValidationException("The email address is not valid (example: thandi@company.co.za).");
            }
            if (employees.emailInUse(c.email(), excludeId)) {
                throw new ValidationException("Another active employee already uses the email " + c.email() + ".");
            }
        }
        if (c.phone() != null) {
            long digits = c.phone().chars().filter(Character::isDigit).count();
            if (!PHONE_CHARACTERS.matcher(c.phone()).matches() || digits < 7 || digits > 15) {
                throw new ValidationException(
                        "The phone number is not valid. Use 7 to 15 digits (spaces, +, - and brackets are allowed).");
            }
        }
        TextRules.requireSingleLine(c.position(), "position");
        TextRules.requireSingleLine(c.department(), "department");
        if (c.position() != null && c.position().length() > MAX_TEXT) {
            throw new ValidationException("The position may be at most " + MAX_TEXT + " characters.");
        }
        if (c.department() != null && c.department().length() > MAX_TEXT) {
            throw new ValidationException("The department may be at most " + MAX_TEXT + " characters.");
        }
        checkHireDate(c.hireDate());
        if (c.role() == null) {
            throw new ValidationException("Please choose a role.");
        }
    }

    private void checkName(String value, String label) {
        if (value.isEmpty()) {
            throw new ValidationException("The " + label + " is required.");
        }
        if (value.length() > MAX_NAME) {
            throw new ValidationException("The " + label + " may be at most " + MAX_NAME + " characters.");
        }
        if (!NAME.matcher(value).matches()) {
            throw new ValidationException(
                    "The " + label + " may only contain letters, spaces, apostrophes, dots and hyphens.");
        }
    }

    private void checkHireDate(String text) {
        if (text == null) {
            return;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(text, DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new ValidationException(
                    "The hire date must be a real date written as yyyy-MM-dd (example: 2026-03-15).");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException("The hire date cannot be in the future.");
        }
        if (date.isBefore(EARLIEST_HIRE_DATE)) {
            throw new ValidationException("The hire date cannot be before 1950-01-01.");
        }
    }

    private void checkUsername(String username) {
        if (username.isEmpty()) {
            throw new ValidationException("The username is required.");
        }
        if (username.length() < MIN_USERNAME || username.length() > MAX_USERNAME) {
            throw new ValidationException(
                    "The username must be " + MIN_USERNAME + " to " + MAX_USERNAME + " characters long.");
        }
        if (!USERNAME.matcher(username).matches()) {
            throw new ValidationException(
                    "The username may only contain letters, numbers, dots, underscores and hyphens (no spaces).");
        }
    }

    private void checkPassword(String password, String confirmPassword, String username) {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("The password is required.");
        }
        if (password.length() < MIN_PASSWORD || password.length() > MAX_PASSWORD) {
            throw new ValidationException(
                    "The password must be " + MIN_PASSWORD + " to " + MAX_PASSWORD + " characters long.");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new ValidationException("The password must contain at least one letter and one number.");
        }
        if (password.equalsIgnoreCase(username)) {
            throw new ValidationException("The password may not be the same as the username.");
        }
        if (!password.equals(confirmPassword)) {
            throw new ValidationException("The two passwords do not match.");
        }
    }

    private String trim(String text) {
        return text == null ? "" : text.trim();
    }

    private String blankToNull(String text) {
        String t = trim(text);
        return t.isEmpty() ? null : t;
    }
}
