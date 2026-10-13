package com.bms.backend.supplier;

import com.bms.backend.common.ConflictException;
import com.bms.backend.common.NotFoundException;
import com.bms.backend.common.TextRules;
import com.bms.backend.common.ValidationException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The rules for suppliers, ported from Version 1's SupplierService (same limits, same messages).
 * "Administrator only" is enforced in SecurityConfig (every /api/suppliers URL).
 */
@Service
public class SupplierService {

    private static final int MAX_NAME = 100;
    private static final int MAX_CONTACT = 100;
    private static final int MAX_EMAIL = 100;
    private static final int MAX_ADDRESS = 200;
    private static final int MAX_SEARCH = 100;
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_CHARACTERS = Pattern.compile("^[0-9 +()-]+$");

    private static final String GONE = "This supplier no longer exists. Please refresh the list.";

    private final SupplierRepository suppliers;

    public SupplierService(SupplierRepository suppliers) {
        this.suppliers = suppliers;
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> list(String search) {
        String text = search == null ? "" : search.trim();
        if (text.length() > MAX_SEARCH) {
            throw new ValidationException("The search text may be at most " + MAX_SEARCH + " characters.");
        }
        List<Supplier> found = text.isEmpty()
                ? suppliers.findByActiveTrueOrderBySupplierNameAsc()
                : suppliers.search(text.toLowerCase(Locale.ROOT));
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : suppliers.countActiveProductsPerSupplier()) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return found.stream()
                .map(s -> SupplierResponse.from(s, counts.getOrDefault(s.getSupplierId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse get(Long id) {
        Supplier s = findActive(id);
        return SupplierResponse.from(s, suppliers.countActiveProducts(id));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        Cleaned c = clean(request);
        validate(c, 0L);
        Supplier saved = suppliers.save(
                new Supplier(c.name(), c.contact(), c.email(), c.phone(), c.address()));
        return SupplierResponse.from(saved, 0);
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Cleaned c = clean(request);
        Supplier existing = findActive(id);
        validate(c, id);
        existing.change(c.name(), c.contact(), c.email(), c.phone(), c.address());
        return SupplierResponse.from(existing, suppliers.countActiveProducts(id));
    }

    /** Hides the supplier, but not while active products still come from it. */
    @Transactional
    public void delete(Long id) {
        Supplier existing = findActive(id);
        long products = suppliers.countActiveProducts(id);
        if (products > 0) {
            throw new ConflictException("This supplier provides " + products + " product"
                    + (products == 1 ? "" : "s") + " and cannot be deleted. "
                    + "Change or delete those products first.");
        }
        existing.deactivate();
    }

    // ---------- helpers ----------

    private Supplier findActive(Long id) {
        return suppliers.findById(id)
                .filter(Supplier::isActive)
                .orElseThrow(() -> new NotFoundException(GONE));
    }

    private record Cleaned(String name, String contact, String email, String phone, String address) {
    }

    private Cleaned clean(SupplierRequest r) {
        return new Cleaned(trim(r.supplierName()), blankToNull(r.contactPerson()), blankToNull(r.email()),
                blankToNull(r.phone()), blankToNull(r.address()));
    }

    private void validate(Cleaned c, Long excludeId) {
        if (c.name().isEmpty()) {
            throw new ValidationException("The supplier name is required.");
        }
        TextRules.requireSingleLine(c.name(), "supplier name");
        TextRules.requireSingleLine(c.contact(), "contact person");
        TextRules.requireSingleLine(c.address(), "address");
        if (c.name().length() > MAX_NAME) {
            throw new ValidationException("The supplier name may be at most " + MAX_NAME + " characters.");
        }
        if (c.contact() != null && c.contact().length() > MAX_CONTACT) {
            throw new ValidationException("The contact person may be at most " + MAX_CONTACT + " characters.");
        }
        if (c.email() != null && (c.email().length() > MAX_EMAIL || !EMAIL.matcher(c.email()).matches())) {
            throw new ValidationException("The email address is not valid (example: orders@supplier.co.za).");
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
        if (suppliers.existsByActiveTrueAndSupplierNameIgnoreCaseAndSupplierIdNot(c.name(), excludeId)) {
            throw new ValidationException("A supplier called \"" + c.name() + "\" already exists.");
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
