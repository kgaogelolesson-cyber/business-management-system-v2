package com.bms.backend.report;

import com.bms.backend.common.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the reports, ported from Version 1's ReportService (same reports, same summary lines).
 * "Administrator only" is enforced in SecurityConfig.
 * Sales reports count COMPLETED orders only, inside a date range (both days included).
 */
@Service
public class ReportService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    /** The reports on offer. needsDates = whether the date range is used. */
    private enum Type {
        SALES_BY_PRODUCT("Sales by product", true),
        SALES_BY_EMPLOYEE("Sales by employee", true),
        SALES_BY_CUSTOMER("Sales by customer", true),
        SALES_BY_DAY("Sales by day", true),
        STOCK_LEVELS("Stock levels and value", false),
        LOW_STOCK("Low stock (to reorder)", false);

        final String label;
        final boolean needsDates;

        Type(String label, boolean needsDates) {
            this.label = label;
            this.needsDates = needsDates;
        }
    }

    private static final String TEXT = "TEXT";
    private static final String NUMBER = "NUMBER";
    private static final String MONEY = "MONEY";

    private final ReportRepository reports;

    public ReportService(ReportRepository reports) {
        this.reports = reports;
    }

    public List<ReportTypeInfo> types() {
        return Arrays.stream(Type.values()).map(t -> new ReportTypeInfo(t.name(), t.label, t.needsDates)).toList();
    }

    /** Runs one report. For stock reports the dates are ignored (they may be missing). */
    @Transactional(readOnly = true)
    public ReportResponse run(String typeName, String fromText, String toText) {
        Type type = parseType(typeName);
        LocalDate from = null;
        LocalDate to = null;
        if (type.needsDates) {
            from = parseDate(fromText, "from");
            to = parseDate(toText, "to");
            if (from.isAfter(to)) {
                throw new ValidationException("The 'from' date must not be after the 'to' date.");
            }
        }
        String symbol = reports.currencySymbol();
        return switch (type) {
            case SALES_BY_PRODUCT -> sales(type, from, to, symbol, "Product", "Units sold",
                    reports.salesByProduct(from.toString(), to.toString()));
            case SALES_BY_EMPLOYEE -> sales(type, from, to, symbol, "Employee", "Orders",
                    reports.salesByEmployee(from.toString(), to.toString()));
            case SALES_BY_CUSTOMER -> sales(type, from, to, symbol, "Customer", "Orders",
                    reports.salesByCustomer(from.toString(), to.toString()));
            case SALES_BY_DAY -> sales(type, from, to, symbol, "Date", "Orders",
                    reports.salesByDay(from.toString(), to.toString()));
            case STOCK_LEVELS -> stockLevels(symbol);
            case LOW_STOCK -> lowStock(symbol);
        };
    }

    // ---------- building each report ----------

    /** Adds the summary line (orders, revenue, average, cancelled) that every sales report shows. */
    private ReportResponse sales(Type type, LocalDate from, LocalDate to, String symbol,
                                 String firstHeader, String countHeader, List<List<Object>> rows) {
        long[] totals = reports.salesTotals(from.toString(), to.toString());
        long orders = totals[0];
        long revenue = totals[1];
        long cancelled = totals[2];
        long average = orders == 0 ? 0 : Math.round((double) revenue / orders);
        String summary = orders + " completed order(s)   |   Revenue " + money(revenue, symbol)
                + "   |   Average order " + money(average, symbol)
                + "   |   " + cancelled + " cancelled (not counted)";
        return new ReportResponse(type.label + ", " + from + " to " + to, summary, symbol,
                List.of(new ReportColumn(firstHeader, TEXT), new ReportColumn(countHeader, NUMBER),
                        new ReportColumn("Revenue", MONEY)),
                rows);
    }

    private ReportResponse stockLevels(String symbol) {
        List<List<Object>> rows = reports.stockLevels();
        long units = 0;
        long value = 0;
        for (List<Object> row : rows) {
            units += (Long) row.get(2);
            value += (Long) row.get(6);
        }
        String summary = rows.size() + " product(s)   |   " + units + " unit(s) in stock   |   Stock value "
                + money(value, symbol) + " (at selling price)";
        return new ReportResponse(Type.STOCK_LEVELS.label, summary, symbol,
                List.of(new ReportColumn("Product", TEXT), new ReportColumn("Category", TEXT),
                        new ReportColumn("In stock", NUMBER), new ReportColumn("Reorder at", NUMBER),
                        new ReportColumn("Status", TEXT), new ReportColumn("Unit price", MONEY),
                        new ReportColumn("Stock value", MONEY)),
                rows);
    }

    private ReportResponse lowStock(String symbol) {
        List<List<Object>> rows = reports.lowStock();
        long outOfStock = rows.stream().filter(r -> (Long) r.get(2) == 0).count();
        String summary = rows.size() + " product(s) need reordering   |   " + outOfStock + " out of stock";
        return new ReportResponse(Type.LOW_STOCK.label, summary, symbol,
                List.of(new ReportColumn("Product", TEXT), new ReportColumn("Category", TEXT),
                        new ReportColumn("In stock", NUMBER), new ReportColumn("Reorder at", NUMBER),
                        new ReportColumn("Status", TEXT), new ReportColumn("Supplier", TEXT)),
                rows);
    }

    // ---------- helpers ----------

    private Type parseType(String name) {
        if (name != null) {
            for (Type t : Type.values()) {
                if (t.name().equalsIgnoreCase(name.trim())) {
                    return t;
                }
            }
        }
        throw new ValidationException("Please choose a report.");
    }

    private LocalDate parseDate(String text, String fieldName) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            throw new ValidationException("The " + fieldName + " date is required (example: 2026-10-01).");
        }
        try {
            return LocalDate.parse(t, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ValidationException("The " + fieldName
                    + " date must be a real date written as yyyy-MM-dd (example: 2026-10-01).");
        }
    }

    /** 68498 cents -> "R684.98" */
    private String money(long cents, String symbol) {
        return String.format(Locale.US, "%s%,.2f", symbol, BigDecimal.valueOf(cents, 2));
    }
}
