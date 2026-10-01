package com.wingsup.warehouse.controller;

import com.wingsup.warehouse.model.*;
import com.wingsup.warehouse.repository.ProductRepository;
import com.wingsup.warehouse.repository.StockTransactionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ProductRepository products;
    private final StockTransactionRepository txs;

    public DashboardController(
            ProductRepository products,
            StockTransactionRepository txs
    ) {
        this.products = products;
        this.txs = txs;
    }

    @GetMapping
    public Map<String, Object> stats(
            @RequestAttribute("user") AppUser me
    ) {

        /*
         * ADMIN + MANAGER:
         * → xem toàn bộ phiếu trong hệ thống
         *
         * USER:
         * → chỉ xem phiếu do chính mình tạo
         */
        boolean canViewAllTransactions =
                me.getRole() == Role.ADMIN
                || me.getRole() == Role.MANAGER;

        /*
         * =====================================================
         * SẢN PHẨM
         * =====================================================
         */
        List<Product> all = products.findAll();

        List<Product> low = products.findLowStock();

        /*
         * =====================================================
         * PHIẾU GẦN ĐÂY
         *
         * ADMIN + MANAGER → tất cả
         * USER → của mình
         * =====================================================
         */
        List<StockTransaction> recentTransactions =
                canViewAllTransactions
                        ? txs.findAllByOrderByCreatedAtDesc()
                        : txs.findByCreatedByOrderByCreatedAtDesc(me);

        Map<String, Object> m = new LinkedHashMap<>();

        /*
         * Tổng số loại sản phẩm
         */
        m.put(
                "totalProducts",
                all.size()
        );

        /*
         * Tổng số lượng tồn kho
         */
        m.put(
                "totalQuantity",
                all.stream()
                        .mapToLong(Product::getQuantity)
                        .sum()
        );

        /*
         * Số sản phẩm dưới mức tồn tối thiểu
         */
        m.put(
                "lowStockCount",
                low.size()
        );

        /*
         * =====================================================
         * SỐ PHIẾU CHỜ DUYỆT
         *
         * ADMIN + MANAGER → tất cả phiếu PENDING
         * USER → chỉ phiếu PENDING của mình
         * =====================================================
         */
        if (canViewAllTransactions) {

            m.put(
                    "pendingCount",
                    txs.countByStatus(TxStatus.PENDING)
            );

        } else {

            m.put(
                    "pendingCount",
                    txs.countByStatusAndCreatedBy(
                            TxStatus.PENDING,
                            me
                    )
            );
        }

        /*
         * Danh sách sản phẩm sắp hết
         */
        m.put(
                "lowStock",
                low
        );

        /*
         * 5 phiếu gần đây nhất
         */
        m.put(
                "recent",
                recentTransactions
                        .stream()
                        .limit(5)
                        .toList()
        );

        return m;
    }
}