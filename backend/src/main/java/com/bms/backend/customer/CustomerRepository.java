package com.bms.backend.customer;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring writes the implementation. The long method name is itself a query:
 *   findBy Active True  OrderBy LastName Asc, FirstName Asc
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByActiveTrueOrderByLastNameAscFirstNameAsc();

    /**
     * How many orders belong to this customer. We have no Order class yet, so this asks the
     * "orders" table directly with plain SQL (a "native" query).
     */
    @Query(value = "SELECT COUNT(*) FROM orders WHERE customer_id = :customerId", nativeQuery = true)
    long countOrders(@Param("customerId") Long customerId);
}
