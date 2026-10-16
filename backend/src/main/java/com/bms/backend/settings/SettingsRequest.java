package com.bms.backend.settings;

/** The JSON sent when the administrator saves the settings. */
public record SettingsRequest(String currencySymbol) {
}
