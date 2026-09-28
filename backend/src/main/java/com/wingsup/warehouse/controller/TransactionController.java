package com.wingsup.warehouse.controller;

import com.wingsup.warehouse.model.*;
import com.wingsup.warehouse.repository.ProductRepository;
import com.wingsup.warehouse.repository.StockTransactionRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phiếu nhập/xuất kho.
 * USER tạo phiếu (PENDING) và hủy phiếu của mình; ADMIN tạo phiếu (tự duyệt), duyệt/từ chối phiếu.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final StockTransactionRepository repo;
    private final ProductRepository products;

    public TransactionController(StockTransactionRepository repo, ProductRepository products) {
        this.repo = repo;
        this.products = products;
    }

    public record TxRequest(Long productId, TxType type, Integer quantity, String note) {}

    @GetMapping
    public List<StockTransaction> list(@RequestAttribute("user") AppUser me) {
        return me.getRole() == Role.ADMIN ? repo.findAllByOrderByCreatedAtDesc()
                : repo.findByCreatedByOrderByCreatedAtDesc(me);
    }

    @PostMapping
    @Transactional
    public StockTransaction create(@RequestBody TxRequest req, @RequestAttribute("user") AppUser me) {
        if (req.productId() == null || req.type() == null || req.quantity() == null || req.quantity() <= 0)
            throw new IllegalArgumentException("Cần chọn sản phẩm, loại phiếu và số lượng lớn hơn 0");
        Product p = products.findById(req.productId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
        if (req.type() == TxType.OUT && p.getQuantity() < req.quantity())
            throw new IllegalArgumentException("Không đủ tồn kho (còn " + p.getQuantity() + ")");

        StockTransaction t = new StockTransaction();
        t.setProduct(p);
        t.setType(req.type());
        t.setQuantity(req.quantity());
        t.setNote(req.note());
        t.setCreatedBy(me);
        if (me.getRole() == Role.ADMIN) {
            apply(t, p);
            t.setStatus(TxStatus.APPROVED);
            t.setApprovedBy(me);
            t.setApprovedAt(LocalDateTime.now());
        }
        return repo.save(t);
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public StockTransaction approve(@PathVariable Long id, @RequestAttribute("user") AppUser me) {
        StockTransaction t = pending(id);
        apply(t, t.getProduct());
        t.setStatus(TxStatus.APPROVED);
        t.setApprovedBy(me);
        t.setApprovedAt(LocalDateTime.now());
        return repo.save(t);
    }

    @PostMapping("/{id}/reject")
    @Transactional
    public StockTransaction reject(@PathVariable Long id, @RequestAttribute("user") AppUser me) {
        StockTransaction t = pending(id);
        t.setStatus(TxStatus.REJECTED);
        t.setApprovedBy(me);
        t.setApprovedAt(LocalDateTime.now());
        return repo.save(t);
    }

    @DeleteMapping("/{id}")
    public void cancel(@PathVariable Long id, @RequestAttribute("user") AppUser me) {
        StockTransaction t = pending(id);
        boolean owner = t.getCreatedBy() != null && t.getCreatedBy().getId().equals(me.getId());
        if (!owner && me.getRole() != Role.ADMIN)
            throw new IllegalArgumentException("Chỉ được hủy phiếu do chính mình tạo");
        repo.delete(t);
    }

    private StockTransaction pending(Long id) {
        StockTransaction t = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu"));
        if (t.getStatus() != TxStatus.PENDING) throw new IllegalArgumentException("Phiếu đã được xử lý");
        return t;
    }

    private void apply(StockTransaction t, Product p) {
        if (t.getType() == TxType.IN) {
            p.setQuantity(p.getQuantity() + t.getQuantity());
        } else {
            if (p.getQuantity() < t.getQuantity())
                throw new IllegalArgumentException("Không đủ tồn kho (còn " + p.getQuantity() + ")");
            p.setQuantity(p.getQuantity() - t.getQuantity());
        }
        products.save(p);
    }
}