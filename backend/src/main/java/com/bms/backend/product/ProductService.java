package com.bms.backend.product;

import com.bms.backend.common.ConflictException;
import com.bms.backend.common.NotFoundException;
import com.bms.backend.common.TextRules;
import com.bms.backend.common.ValidationException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The rules for products, ported from Version 1's ProductService (same limits, same messages).
 * "Who may do what" (only the administrator may add, change or delete) is enforced in SecurityConfig.
 * Not ported yet: the stock-status filter and the "default low-stock level" setting (5 is used).
 */
@Service
public class ProductService {

    private static final int MAX_NAME = 100;
    private static final int MAX_CATEGORY = 50;
    private static final int MAX_DESCRIPTION = 500;
    private static final long MAX_PRICE_CENTS = 1_000_000_000L;   // R10,000,000.00
    private static final int MAX_QUANTITY = 1_000_000;
    private static final int DEFAULT_REORDER_LEVEL = 5;
    private static final int MAX_SEARCH = 100;

    private static final String GONE = "This product no longer exists. Please refresh the list.";

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    /** All active products, optionally filtered by name text and/or category. */
    @Transactional(readOnly = true)
    public List<ProductResponse> list(String search, String category) {
        String text = search == null ? "" : search.trim();
        String cat = category == null ? "" : category.trim();
        if (text.length() > MAX_SEARCH || cat.length() > MAX_SEARCH) {
            throw new ValidationException("The search text may be at most " + MAX_SEARCH + " characters.");
        }
        List<Product> found = (text.isEmpty() && cat.isEmpty())
                ? products.findByActiveTrueOrderByProductNameAsc()
                : products.search(text.toLowerCase(Locale.ROOT), cat.toLowerCase(Locale.ROOT));
        return found.stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return products.findCategories();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(findActive(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Cleaned c = clean(request);
        validate(c, 0L);
        Product saved = products.save(new Product(c.name(), c.category(), c.description(),
                c.priceCents(), c.quantity(), c.reorderLevel(), c.supplierId()));
        return ProductResponse.from(saved);
    }

    /** Changes everything except the stock quantity. */
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Cleaned c = clean(request);
        Product existing = findActive(id);
        validate(c, id);
        existing.change(c.name(), c.category(), c.description(), c.priceCents(), c.reorderLevel(), c.supplierId());
        return ProductResponse.from(existing);
    }

    /** Hides the product, but never one that appears on an order: order history must be kept. */
    @Transactional
    public void delete(Long id) {
        Product existing = findActive(id);
        long lines = products.countOrderLines(id);
        if (lines > 0) {
            throw new ConflictException("This product appears on " + lines + " order line"
                    + (lines == 1 ? "" : "s") + " and cannot be deleted. Order history must be kept.");
        }
        existing.deactivate();
    }

    // ---------- helpers ----------

    private Product findActive(Long id) {
        return products.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new NotFoundException(GONE));
    }

    private record Cleaned(String name, String category, String description, Long priceCentsOrNull,
                           int quantity, int reorderLevel, Long supplierId) {
        long priceCents() {
            return priceCentsOrNull;
        }
    }

    /** Trims text, empty optional fields become null, missing numbers get their defaults. */
    private Cleaned clean(ProductRequest r) {
        return new Cleaned(
                trim(r.productName()),
                blankToNull(r.category()),
                blankToNull(r.description()),
                r.priceCents(),
                r.quantityInStock() == null ? 0 : r.quantityInStock(),
                r.reorderLevel() == null ? DEFAULT_REORDER_LEVEL : r.reorderLevel(),
                r.supplierId());
    }

    private void validate(Cleaned c, Long excludeId) {
        if (c.name().isEmpty()) {
            throw new ValidationException("The product name is required.");
        }
        TextRules.requireSingleLine(c.name(), "product name");
        TextRules.requireSingleLine(c.category(), "category");
        TextRules.requireSingleLine(c.description(), "description");
        if (c.name().length() > MAX_NAME) {
            throw new ValidationException("The product name may be at most " + MAX_NAME + " characters.");
        }
        if (c.category() != null && c.category().length() > MAX_CATEGORY) {
            throw new ValidationException("The category may be at most " + MAX_CATEGORY + " characters.");
        }
        if (c.description() != null && c.description().length() > MAX_DESCRIPTION) {
            throw new ValidationException("The description may be at most " + MAX_DESCRIPTION + " characters.");
        }
        if (c.priceCentsOrNull() == null) {
            throw new ValidationException("The price is required.");
        }
        if (c.priceCents() < 0 || c.priceCents() > MAX_PRICE_CENTS) {
            throw new ValidationException("The price must be between 0 and 10,000,000.");
        }
        if (c.quantity() < 0 || c.quantity() > MAX_QUANTITY) {
            throw new ValidationException("The stock quantity must be between 0 and " + MAX_QUANTITY + ".");
        }
        if (c.reorderLevel() < 0 || c.reorderLevel() > MAX_QUANTITY) {
            throw new ValidationException("The reorder level must be between 0 and " + MAX_QUANTITY + ".");
        }
        if (c.supplierId() != null && products.countActiveSuppliers(c.supplierId()) == 0) {
            throw new ValidationException("The chosen supplier does not exist. Please choose another.");
        }
        if (products.existsByActiveTrueAndProductNameIgnoreCaseAndProductIdNot(c.name(), excludeId)) {
            throw new ValidationException("A product called \"" + c.name() + "\" already exists.");
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
