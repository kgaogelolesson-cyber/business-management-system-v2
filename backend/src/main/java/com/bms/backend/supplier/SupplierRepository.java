package com.bms.backend.supplier;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByActiveTrueOrderBySupplierNameAsc();

    /**
     * Search by name, contact person, email or phone; capital letters do not matter.
     * locate() = "where does the text occur?" (0 = nowhere): "contains" without LIKE wildcards.
     */
    @Query("""
            SELECT s FROM Supplier s
            WHERE s.active = true
              AND (   locate(:text, lower(s.supplierName)) > 0
                   OR locate(:text, lower(coalesce(s.contactPerson, ''))) > 0
                   OR locate(:text, lower(coalesce(s.email, ''))) > 0
                   OR locate(:text, coalesce(s.phone, '')) > 0)
            ORDER BY s.supplierName
            """)
    List<Supplier> search(@Param("text") String text);

    /** Is another ACTIVE supplier already using this name? Pass excludeId 0 when creating. */
    boolean existsByActiveTrueAndSupplierNameIgnoreCaseAndSupplierIdNot(String supplierName, Long excludeId);

    /** Pairs of (supplier id, number of ACTIVE products), only for suppliers that have products. */
    @Query(value = """
            SELECT supplier_id, COUNT(*) FROM products
            WHERE is_active = true AND supplier_id IS NOT NULL
            GROUP BY supplier_id
            """, nativeQuery = true)
    List<Object[]> countActiveProductsPerSupplier();

    @Query(value = "SELECT COUNT(*) FROM products WHERE supplier_id = :supplierId AND is_active = true",
            nativeQuery = true)
    long countActiveProducts(@Param("supplierId") Long supplierId);
}
