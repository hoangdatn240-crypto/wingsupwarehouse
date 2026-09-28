package com.wingsup.warehouse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transactions")
@Getter
@Setter
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TxType type;

    @ManyToOne(optional = true)
    @JoinColumn(name = "product_id", nullable = true)
    private Product product;

    private int quantity;
    private String note;

    @Enumerated(EnumType.STRING)
    private TxStatus status = TxStatus.PENDING;

    // Cho phép NULL khi xóa người tạo phiếu
    @ManyToOne(optional = true)
    @JoinColumn(name = "created_by_id", nullable = true)
    private AppUser createdBy;

    @ManyToOne(optional = true)
    @JoinColumn(name = "approved_by_id", nullable = true)
    private AppUser approvedBy;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime approvedAt;
}
