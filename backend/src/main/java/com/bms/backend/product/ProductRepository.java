package com.bms.backend.product;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueOrderByProductNameAsc();

    /**
     * Search by name (or part of it) and/or an exact category; capital letters do not matter.
     * Both parameters are always text: an empty text means "do not filter on this".
     * (We avoid passing null into a query because PostgreSQL cannot always guess its type.)
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
              AND (:text = '' OR locate(:text, lower(p.productName)) > 0)
              AND (:category = '' OR lower(coalesce(p.category, '')) = :category)
            ORDER BY p.productName
            """)
    List<Product> search(@Param("text") String text, @Param("category") String category);

    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.active = true AND p.category IS NOT NULL ORDER BY p.category")
    List<String> findCategories();

    /** Is another ACTIVE product already using this name? Pass excludeId 0 when creating. */
    boolean existsByActiveTrueAndProductNameIgnoreCaseAndProductIdNot(String productName, Long excludeId);

    /** How many order lines (on any order) contain this product. No OrderItem class yet, so plain SQL. */
    @Query(value = "SELECT COUNT(*) FROM order_items WHERE product_id = :productId", nativeQuery = true)
    long countOrderLines(@Param("productId") Long productId);

    @Query(value = "SELECT COUNT(*) FROM suppliers WHERE supplier_id = :supplierId AND is_active = true",
            nativeQuery = true)
    long countActiveSuppliers(@Param("supplierId") Long supplierId);
}
