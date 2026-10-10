package com.bms.backend.customer;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Version 1 called this a DAO and we wrote the SQL by hand.
 * Here we only declare an interface. Spring Data JPA writes the class and the SQL for us.
 * JpaRepository already gives us findAll, findById, save and deleteById.
 *
 * The long method name is itself the query:
 *   findBy Active True  OrderBy LastName Asc, FirstName Asc
 * means: WHERE is_active = true ORDER BY last_name, first_name
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByActiveTrueOrderByLastNameAscFirstNameAsc();
}
