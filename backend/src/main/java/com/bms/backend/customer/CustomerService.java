package com.bms.backend.customer;

import com.bms.backend.common.ConflictException;
import com.bms.backend.common.NotFoundException;
import com.bms.backend.common.TextRules;
import com.bms.backend.common.ValidationException;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The rules for customers, ported from Version 1's CustomerService (same limits, same messages).
 * Not ported yet: "who may do what" (needs login, a later step) and the duplicate e-mail/phone warning.
 */
@Service
public class CustomerService {

    private static final int MAX_NAME = 50;
    private static final int MAX_EMAIL = 100;
    private static final int MAX_ADDRESS = 200;
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_CHARACTERS = Pattern.compile("^[0-9 +()-]+$");

    private static final String GONE = "This customer no longer exists. Please refresh the list.";

    private final CustomerRepository customers;

    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> listActive() {
        return customers.findByActiveTrueOrderByLastNameAscFirstNameAsc()
                .stream()
                .map(CustomerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long id) {
        return CustomerResponse.from(findActive(id));
    }

    /** @Transactional: everything in the method is saved together, or nothing is. */
    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        Cleaned c = clean(request);
        validate(c);
        Customer saved = customers.save(
                new Customer(c.firstName(), c.lastName(), c.email(), c.phone(), c.address()));
        return CustomerResponse.from(saved);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Cleaned c = clean(request);
        validate(c);
        Customer existing = findActive(id);
        existing.change(c.firstName(), c.lastName(), c.email(), c.phone(), c.address());
        return CustomerResponse.from(existing);
    }

    /** Hides the customer, but never one that has orders: their history must be kept. */
    @Transactional
    public void delete(Long id) {
        Customer existing = findActive(id);
        long orders = customers.countOrders(id);
        if (orders > 0) {
            throw new ConflictException("This customer has " + orders + " order"
                    + (orders == 1 ? "" : "s") + " and cannot be deleted. "
                    + "Their order history must be kept.");
        }
        existing.deactivate();
    }

    // ---------- helpers ----------

    private Customer findActive(Long id) {
        return customers.findById(id)
                .filter(Customer::isActive)
                .orElseThrow(() -> new NotFoundException(GONE));
    }

    /** The cleaned-up values: trimmed, and empty optional fields become null. */
    private record Cleaned(String firstName, String lastName, String email, String phone, String address) {
    }

    private Cleaned clean(CustomerRequest r) {
        return new Cleaned(
                trim(r.firstName()),
                trim(r.lastName()),
                blankToNull(r.email()),
                blankToNull(r.phone()),
                blankToNull(r.address()));
    }

    private void validate(Cleaned c) {
        if (c.firstName().isEmpty()) {
            throw new ValidationException("First name is required.");
        }
        if (c.lastName().isEmpty()) {
            throw new ValidationException("Last name is required.");
        }
        TextRules.requireSingleLine(c.firstName(), "first name");
        TextRules.requireSingleLine(c.lastName(), "last name");
        TextRules.requireSingleLine(c.address(), "address");
        if (c.firstName().length() > MAX_NAME || c.lastName().length() > MAX_NAME) {
            throw new ValidationException("Names may be at most " + MAX_NAME + " characters.");
        }
        if (c.email() != null
                && (c.email().length() > MAX_EMAIL || !EMAIL.matcher(c.email()).matches())) {
            throw new ValidationException("The email address is not valid (example: name@example.com).");
        }
        if (c.phone() != null) {
            long digits = c.phone().chars().filter(Character::isDigit).count();
            if (!PHONE_CHARACTERS.matcher(c.phone()).matches() || digits < 7 || digits > 15) {
                throw new ValidationException(
                        "The phone number is not valid. Use 7 to 15 digits (spaces, +, - and brackets are allowed).");
            }
        }
        if (c.address() != null && c.address().length() > MAX_ADDRESS) {
            throw new ValidationException("The address may be at most " + MAX_ADDRESS + " characters.");
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
