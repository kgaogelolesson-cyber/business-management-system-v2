package com.bms.backend.settings;

import com.bms.backend.common.TextRules;
import com.bms.backend.common.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The rules for the settings. Only the currency symbol can be changed for now. */
@Service
public class SettingsService {

    static final int MAX_SYMBOL = 5;
    private static final String DEFAULT_SYMBOL = "R";

    private final SettingsRepository settings;

    public SettingsService(SettingsRepository settings) {
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public SettingsResponse get() {
        String symbol = settings.currencySymbol();
        return new SettingsResponse(symbol == null || symbol.isBlank() ? DEFAULT_SYMBOL : symbol);
    }

    @Transactional
    public SettingsResponse update(SettingsRequest request) {
        String symbol = request.currencySymbol() == null ? "" : request.currencySymbol().trim();
        if (symbol.isEmpty()) {
            throw new ValidationException("Please enter a currency symbol (for example R).");
        }
        TextRules.requireSingleLine(symbol, "currency symbol");
        if (symbol.length() > MAX_SYMBOL) {
            throw new ValidationException("The currency symbol may be at most " + MAX_SYMBOL + " characters.");
        }
        settings.saveCurrencySymbol(symbol);
        return new SettingsResponse(symbol);
    }
}
