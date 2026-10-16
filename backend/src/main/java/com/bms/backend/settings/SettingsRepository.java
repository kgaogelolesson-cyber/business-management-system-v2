package com.bms.backend.settings;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Reads and writes the settings table (one row per setting). */
@Repository
public class SettingsRepository {

    static final String CURRENCY_KEY = "currency_symbol";

    private final JdbcTemplate jdbc;

    public SettingsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The saved currency symbol, or null when the row does not exist. */
    public String currencySymbol() {
        return jdbc.query("SELECT setting_value FROM settings WHERE setting_key = ?",
                (rs, i) -> rs.getString(1), CURRENCY_KEY).stream().findFirst().orElse(null);
    }

    /** Inserts the row, or changes it when it already exists. */
    public void saveCurrencySymbol(String symbol) {
        jdbc.update("""
                INSERT INTO settings (setting_key, setting_value) VALUES (?, ?)
                ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value
                """, CURRENCY_KEY, symbol);
    }
}
