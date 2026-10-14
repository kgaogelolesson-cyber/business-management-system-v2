package com.bms.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bms.backend.common.ForbiddenException;
import com.bms.backend.common.ValidationException;
import com.bms.backend.orders.OrderDetail;
import com.bms.backend.orders.OrderRequest;
import com.bms.backend.orders.OrderService;
import com.bms.backend.orders.OrderSummary;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Rules for orders: all-or-nothing saving, stock never negative, employees only see their own orders. */
class OrderRulesTest extends DatabaseTestBase {

    @Autowired
    OrderService orders;

    private OrderRequest oneLine(long customer, long product, int quantity) {
        return new OrderRequest(customer, List.of(new OrderRequest.Line(product, quantity)));
    }

    @Test
    void anOrderForMoreThanInStockIsRefusedAndEverythingIsRolledBack() {
        createUser("thandi", "EMPLOYEE");
        long customer = createCustomer();
        long plenty = createProduct("Plenty", 1000, 10);
        long scarce = createProduct("Scarce", 2000, 2);

        // The first line is fine, the second asks for more than exists. The first line's stock must come back.
        OrderRequest request = new OrderRequest(customer, List.of(
                new OrderRequest.Line(plenty, 3),
                new OrderRequest.Line(scarce, 5)));

        assertThatThrownBy(() -> orders.create(request, "thandi", false))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Only 2 in stock");

        assertThat(stockOf(plenty)).isEqualTo(10);
        assertThat(stockOf(scarce)).isEqualTo(2);
        assertThat(countRows("orders")).isZero();
        assertThat(countRows("order_items")).isZero();
        assertThat(countRows("stock_adjustments")).isZero();
    }

    @Test
    void aGoodOrderTakesStockAndUsesThePriceFromTheDatabase() {
        createUser("thandi", "EMPLOYEE");
        long customer = createCustomer();
        long product = createProduct("Pen", 1500, 10);

        OrderDetail detail = orders.create(oneLine(customer, product, 4), "thandi", false);

        assertThat(detail.order().totalCents()).isEqualTo(6000);
        assertThat(stockOf(product)).isEqualTo(6);
        assertThat(countRows("stock_adjustments")).isEqualTo(1);
    }

    @Test
    void anEmployeeCannotOpenAnotherEmployeesOrder() {
        createUser("thandi", "EMPLOYEE");
        createUser("sipho", "EMPLOYEE");
        long customer = createCustomer();
        long product = createProduct("Pen", 1000, 20);

        OrderDetail siphosOrder = orders.create(oneLine(customer, product, 1), "sipho", false);
        long siphosOrderId = siphosOrder.order().id();

        assertThatThrownBy(() -> orders.get(siphosOrderId, "thandi", false))
                .isInstanceOf(ForbiddenException.class);

        // Sipho can open his own order.
        assertThat(orders.get(siphosOrderId, "sipho", false).order().id()).isEqualTo(siphosOrderId);
    }

    @Test
    void anEmployeesListOnlyContainsTheirOwnOrdersButAnAdminSeesAll() {
        createUser("admin1", "ADMIN");
        long thandiId = createUser("thandi", "EMPLOYEE");
        createUser("sipho", "EMPLOYEE");
        long customer = createCustomer();
        long product = createProduct("Pen", 1000, 20);

        orders.create(oneLine(customer, product, 1), "thandi", false);
        orders.create(oneLine(customer, product, 1), "sipho", false);
        orders.create(oneLine(customer, product, 1), "sipho", false);

        List<OrderSummary> thandisView = orders.list(null, null, null, null, null, "thandi", false);
        assertThat(thandisView).hasSize(1);
        assertThat(thandisView.get(0).employeeId()).isEqualTo(thandiId);

        List<OrderSummary> adminView = orders.list(null, null, null, null, null, "admin1", true);
        assertThat(adminView).hasSize(3);
    }

    @Test
    void cancellingPutsTheStockBackAndCannotBeDoneTwice() {
        createUser("admin1", "ADMIN");
        createUser("thandi", "EMPLOYEE");
        long customer = createCustomer();
        long product = createProduct("Pen", 1000, 10);

        long orderId = orders.create(oneLine(customer, product, 4), "thandi", false).order().id();
        assertThat(stockOf(product)).isEqualTo(6);

        orders.cancel(orderId, "admin1");
        assertThat(stockOf(product)).isEqualTo(10);

        assertThatThrownBy(() -> orders.cancel(orderId, "admin1"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already cancelled");
        assertThat(stockOf(product)).isEqualTo(10);
    }
}
