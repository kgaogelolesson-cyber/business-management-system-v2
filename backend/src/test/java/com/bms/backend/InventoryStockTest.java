package com.bms.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bms.backend.common.ValidationException;
import com.bms.backend.inventory.AdjustmentRequest;
import com.bms.backend.inventory.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Rule: stock can never go below 0, and a refused change leaves no trace. */
class InventoryStockTest extends DatabaseTestBase {

    @Autowired
    InventoryService inventory;

    @Test
    void removingMoreThanInStockIsRefusedAndNothingChanges() {
        createUser("admin1", "ADMIN");
        long product = createProduct("Pen", 1000, 5);

        assertThatThrownBy(() -> inventory.adjust(product, new AdjustmentRequest(-6, "Damaged goods"), "admin1"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Only 5 in stock");

        assertThat(stockOf(product)).isEqualTo(5);
        assertThat(countRows("stock_adjustments")).isZero();
    }

    @Test
    void removingExactlyAllTheStockIsAllowed() {
        createUser("admin1", "ADMIN");
        long product = createProduct("Pen", 1000, 5);

        inventory.adjust(product, new AdjustmentRequest(-5, "Stock count"), "admin1");

        assertThat(stockOf(product)).isZero();
        assertThat(countRows("stock_adjustments")).isEqualTo(1);
    }

    @Test
    void addingStockWorksAndIsRecordedInTheHistory() {
        createUser("admin1", "ADMIN");
        long product = createProduct("Pen", 1000, 5);

        inventory.adjust(product, new AdjustmentRequest(10, "Delivery received"), "admin1");

        assertThat(stockOf(product)).isEqualTo(15);
        assertThat(countRows("stock_adjustments")).isEqualTo(1);
    }

    @Test
    void aReasonIsRequired() {
        createUser("admin1", "ADMIN");
        long product = createProduct("Pen", 1000, 5);

        assertThatThrownBy(() -> inventory.adjust(product, new AdjustmentRequest(3, "  "), "admin1"))
                .isInstanceOf(ValidationException.class);

        assertThat(stockOf(product)).isEqualTo(5);
    }
}
