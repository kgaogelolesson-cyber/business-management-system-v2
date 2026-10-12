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

    /**
     * Search by name ("anna smith" works), email or phone. Case does not matter.
     * locate(a, b) answers "where does a occur inside b?" (0 = nowhere), which is a simple way to say
     * "contains" without having to escape the special characters % and _ that LIKE treats as wildcards.
     * This is the same idea as the search box in Version 1.
     */
    @Query("""
            SELECT c FROM Customer c
            WHERE c.active = true
              AND (   locate(:text, lower(concat(c.firstName, ' ', c.lastName))) > 0
                   OR locate(:text, lower(coalesce(c.email, ''))) > 0
                   OR locate(:text, coalesce(c.phone, '')) > 0)
            ORDER BY c.lastName, c.firstName
            """)
    List<Customer> search(@Param("text") String text);
}
