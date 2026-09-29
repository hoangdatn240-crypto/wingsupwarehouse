package com.wingsup.warehouse.controller;

import com.wingsup.warehouse.model.*;
import com.wingsup.warehouse.repository.AppUserRepository;
import com.wingsup.warehouse.repository.NotificationRepository;
import com.wingsup.warehouse.repository.ProductRepository;
import com.wingsup.warehouse.repository.StockTransactionRepository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final StockTransactionRepository repo;
    private final ProductRepository products;
    private final AppUserRepository users;
    private final NotificationRepository notifications;

    public TransactionController(
            StockTransactionRepository repo,
            ProductRepository products,
            AppUserRepository users,
            NotificationRepository notifications
    ) {
        this.repo = repo;
        this.products = products;
        this.users = users;
        this.notifications = notifications;
    }

    public record TxRequest(
            Long productId,
            TxType type,
            Integer quantity,
            String note
    ) {}

    @GetMapping
    public List<StockTransaction> list(
            @RequestAttribute("user") AppUser me
    ) {
        return me.getRole() == Role.ADMIN
                ? repo.findAllByOrderByCreatedAtDesc()
                : repo.findByCreatedByOrderByCreatedAtDesc(me);
    }

    @PostMapping
    @Transactional
    public StockTransaction create(
            @RequestBody TxRequest req,
            @RequestAttribute("user") AppUser me
    ) {

        if (req.productId() == null
                || req.type() == null
                || req.quantity() == null
                || req.quantity() <= 0) {

            throw new IllegalArgumentException(
                    "Cần chọn sản phẩm, loại phiếu và số lượng lớn hơn 0"
            );
        }

        Product p = products.findById(req.productId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy sản phẩm"
                        )
                );

        // Kiểm tra tồn kho khi yêu cầu xuất
        if (req.type() == TxType.OUT
                && p.getQuantity() < req.quantity()) {

            throw new IllegalArgumentException(
                    "Không đủ tồn kho (còn "
                    + p.getQuantity()
                    + ")"
            );
        }

        StockTransaction t = new StockTransaction();

        t.setProduct(p);
        t.setType(req.type());
        t.setQuantity(req.quantity());
        t.setNote(req.note());
        t.setCreatedBy(me);

        /*
         * ADMIN tạo phiếu:
         * → duyệt ngay
         * → cập nhật tồn kho ngay
         */
        if (me.getRole() == Role.ADMIN) {

            apply(t, p);

            t.setStatus(TxStatus.APPROVED);
            t.setApprovedBy(me);
            t.setApprovedAt(LocalDateTime.now());

            StockTransaction saved = repo.save(t);

            notifyUser(
                    me,
                    "Phiếu kho đã được tạo",
                    "ADMIN đã tạo "
                    + typeText(t.getType())
                    + " "
                    + t.getQuantity()
                    + " × "
                    + productName(t),
                    saved
            );

            return saved;
        }

        /*
         * USER / MANAGER:
         * → chỉ tạo PENDING
         * → KHÔNG trừ tồn kho
         */
        t.setStatus(TxStatus.PENDING);

        StockTransaction saved = repo.save(t);

        /*
         * Chỉ gửi thông báo cho ADMIN
         */
        notifyAdmins(
                "🔔 Yêu cầu xuất kho mới",
                me.getFullName()
                        + " yêu cầu "
                        + typeText(t.getType())
                        + " "
                        + t.getQuantity()
                        + " × "
                        + productName(t),
                saved
        );

        return saved;
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public StockTransaction approve(
            @PathVariable Long id,
            @RequestAttribute("user") AppUser me
    ) {

        if (me.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException(
                    "Chỉ ADMIN mới được duyệt phiếu"
            );
        }

        StockTransaction t = pending(id);

        apply(t, t.getProduct());

        t.setStatus(TxStatus.APPROVED);
        t.setApprovedBy(me);
        t.setApprovedAt(LocalDateTime.now());

        StockTransaction saved = repo.save(t);

        /*
         * Thông báo cho người tạo phiếu
         */
        if (t.getCreatedBy() != null) {

            notifyUser(
                    t.getCreatedBy(),
                    "✅ Yêu cầu kho đã được duyệt",
                    "Yêu cầu "
                            + typeText(t.getType())
                            + " "
                            + t.getQuantity()
                            + " × "
                            + productName(t)
                            + " đã được ADMIN duyệt.",
                    saved
            );
        }

        return saved;
    }

    @PostMapping("/{id}/reject")
    @Transactional
    public StockTransaction reject(
            @PathVariable Long id,
            @RequestAttribute("user") AppUser me
    ) {

        if (me.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException(
                    "Chỉ ADMIN mới được từ chối phiếu"
            );
        }

        StockTransaction t = pending(id);

        t.setStatus(TxStatus.REJECTED);
        t.setApprovedBy(me);
        t.setApprovedAt(LocalDateTime.now());

        StockTransaction saved = repo.save(t);

        /*
         * Thông báo cho người tạo phiếu
         */
        if (t.getCreatedBy() != null) {

            notifyUser(
                    t.getCreatedBy(),
                    "❌ Yêu cầu kho bị từ chối",
                    "Yêu cầu "
                            + typeText(t.getType())
                            + " "
                            + t.getQuantity()
                            + " × "
                            + productName(t)
                            + " đã bị ADMIN từ chối.",
                    saved
            );
        }

        return saved;
    }

    @DeleteMapping("/{id}")
    public void cancel(
            @PathVariable Long id,
            @RequestAttribute("user") AppUser me
    ) {

        StockTransaction t = pending(id);

        boolean owner =
                t.getCreatedBy() != null
                && t.getCreatedBy()
                    .getId()
                    .equals(me.getId());

        if (!owner && me.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException(
                    "Chỉ được hủy phiếu do chính mình tạo"
            );
        }

        repo.delete(t);
    }

    private StockTransaction pending(Long id) {

        StockTransaction t = repo.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy phiếu"
                        )
                );

        if (t.getStatus() != TxStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Phiếu đã được xử lý"
            );
        }

        return t;
    }

    private void apply(
            StockTransaction t,
            Product p
    ) {

        if (t.getType() == TxType.IN) {

            p.setQuantity(
                    p.getQuantity()
                    + t.getQuantity()
            );

        } else {

            if (p.getQuantity() < t.getQuantity()) {

                throw new IllegalArgumentException(
                        "Không đủ tồn kho (còn "
                        + p.getQuantity()
                        + ")"
                );
            }

            p.setQuantity(
                    p.getQuantity()
                    - t.getQuantity()
            );
        }

        products.save(p);
    }

    private void notifyAdmins(
            String title,
            String message,
            StockTransaction transaction
    ) {

        List<AppUser> allUsers = users.findAll();

        for (AppUser admin : allUsers) {

            if (admin.getRole() != Role.ADMIN) {
                continue;
            }

            notifyUser(
                    admin,
                    title,
                    message,
                    transaction
            );
        }
    }

    private void notifyUser(
            AppUser user,
            String title,
            String message,
            StockTransaction transaction
    ) {

        Notification n = new Notification();

        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        n.setTransaction(transaction);

        notifications.save(n);
    }

    private String productName(
            StockTransaction t
    ) {

        if (t.getProduct() == null) {
            return "Sản phẩm đã xóa";
        }

        return t.getProduct().getName();
    }

    private String typeText(TxType type) {

        return type == TxType.OUT
                ? "xuất kho"
                : "nhập kho";
    }
}