package com.wingsup.warehouse.repository;

import com.wingsup.warehouse.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCase(String name);

    boolean existsByCategoryId(Long categoryId);

    boolean existsBySupplierId(Long supplierId);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("""
        select p
        from Product p
        where p.quantity <= p.minQuantity
        order by p.quantity
    """)
    List<Product> findLowStock();

    @Modifying
    @Query("""
        UPDATE Product p
        SET p.supplier = NULL
        WHERE p.supplier.id = :supplierId
    """)
    void clearSupplier(@Param("supplierId") Long supplierId);
}
