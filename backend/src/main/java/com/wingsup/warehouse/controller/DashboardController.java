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

    public DashboardController(ProductRepository products, StockTransactionRepository txs) {
        this.products = products;
        this.txs = txs;
    }

    @GetMapping
    public Map<String, Object> stats(@RequestAttribute("user") AppUser me) {
        boolean admin = me.getRole() == Role.ADMIN;
        List<Product> all = products.findAll();
        List<Product> low = products.findLowStock();
        List<StockTransaction> mine = admin ? txs.findAllByOrderByCreatedAtDesc()
                : txs.findByCreatedByOrderByCreatedAtDesc(me);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalProducts", all.size());
        m.put("totalQuantity", all.stream().mapToLong(Product::getQuantity).sum());
        m.put("totalValue", all.stream()
                .mapToDouble(p -> p.getPrice().doubleValue() * p.getQuantity()).sum());
        m.put("lowStockCount", low.size());
        m.put("pendingCount", admin ? txs.countByStatus(TxStatus.PENDING)
                : txs.countByStatusAndCreatedBy(TxStatus.PENDING, me));
        m.put("lowStock", low);
        m.put("recent", mine.stream().limit(5).toList());
        return m;
    }
}