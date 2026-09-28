package com.wingsup.warehouse.repository;

import com.wingsup.warehouse.model.AppUser;
import com.wingsup.warehouse.model.StockTransaction;
import com.wingsup.warehouse.model.TxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface StockTransactionRepository
        extends JpaRepository<StockTransaction, Long> {

    boolean existsByProductId(Long productId);

    List<StockTransaction> findAllByOrderByCreatedAtDesc();

    List<StockTransaction> findByCreatedByOrderByCreatedAtDesc(
            AppUser user
    );

    long countByStatus(TxStatus status);

    long countByStatusAndCreatedBy(
            TxStatus status,
            AppUser user
    );

    // Xóa liên kết người tạo phiếu
    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
        UPDATE StockTransaction s
        SET s.createdBy = NULL
        WHERE s.createdBy.id = :userId
    """)
    void clearCreatedBy(
            @Param("userId") Long userId
    );

    // Xóa liên kết người duyệt phiếu
    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
        UPDATE StockTransaction s
        SET s.approvedBy = NULL
        WHERE s.approvedBy.id = :userId
    """)
    void clearApprovedBy(
            @Param("userId") Long userId
    );

    @Modifying
    @Query("""
    UPDATE StockTransaction s
    SET s.product = NULL
    WHERE s.product.id = :productId
""")
    void clearProduct(@Param("productId") Long productId);
}
