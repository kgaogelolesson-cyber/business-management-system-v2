package com.bms.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bms.backend.common.ValidationException;
import com.bms.backend.settings.SettingsRequest;
import com.bms.backend.settings.SettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** The currency symbol can be changed, with sensible limits, and a refused change leaves it as it was. */
class SettingsServiceTest extends DatabaseTestBase {

    @Autowired
    SettingsService settings;

    @AfterEach
    void putTheDefaultBack() {
        settings.update(new SettingsRequest("R"));
    }

    @Test
    void aNewSymbolIsSavedAndTrimmed() {
        settings.update(new SettingsRequest("  $ "));

        assertThat(settings.get().currencySymbol()).isEqualTo("$");
    }

    @Test
    void anEmptySymbolIsRefusedAndTheOldOneStays() {
        settings.update(new SettingsRequest("€"));

        assertThatThrownBy(() -> settings.update(new SettingsRequest("   ")))
                .isInstanceOf(ValidationException.class);

        assertThat(settings.get().currencySymbol()).isEqualTo("€");
    }

    @Test
    void aTooLongSymbolIsRefused() {
        assertThatThrownBy(() -> settings.update(new SettingsRequest("ABCDEF")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at most 5");
    }
}
