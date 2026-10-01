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

    /*
     * =========================================================
     * LẤY DANH SÁCH PHIẾU
     *
     * ADMIN   → xem tất cả
     * MANAGER → xem tất cả
     * USER    → chỉ xem phiếu do chính mình tạo
     * =========================================================
     */
    @GetMapping
    public List<StockTransaction> list(
            @RequestAttribute("user") AppUser me
    ) {

        if (me.getRole() == Role.ADMIN
                || me.getRole() == Role.MANAGER) {

            return repo.findAllByOrderByCreatedAtDesc();
        }

        return repo.findByCreatedByOrderByCreatedAtDesc(me);
    }

    /*
     * =========================================================
     * TẠO PHIẾU
     *
     * ADMIN   → duyệt ngay + cập nhật tồn kho
     * MANAGER → PENDING
     * USER    → PENDING
     * =========================================================
     */
    @PostMapping
    @Transactional
    public StockTransaction create(
            @RequestBody TxRequest req,
            @RequestAttribute("user") AppUser me
    ) {

        /*
         * Kiểm tra dữ liệu đầu vào
         */
        if (req.productId() == null
                || req.type() == null
                || req.quantity() == null
                || req.quantity() <= 0) {

            throw new IllegalArgumentException(
                    "Cần chọn sản phẩm, loại phiếu và số lượng lớn hơn 0"
            );
        }

        /*
         * USER chỉ được yêu cầu xuất tối đa 10 sản phẩm / phiếu
         */
        if (me.getRole() == Role.USER
                && req.type() == TxType.OUT
                && req.quantity() > 10) {

            throw new IllegalArgumentException(
                    "Người dùng chỉ được yêu cầu xuất tối đa 10 sản phẩm mỗi phiếu."
            );
        }

        /*
         * Tìm sản phẩm
         */
        Product p = products.findById(req.productId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy sản phẩm"
                        )
                );

        /*
         * Kiểm tra tồn kho khi yêu cầu xuất
         */
        if (req.type() == TxType.OUT
                && p.getQuantity() < req.quantity()) {

            throw new IllegalArgumentException(
                    "Không đủ tồn kho (còn "
                            + p.getQuantity()
                            + ")"
            );
        }

        /*
         * Tạo phiếu
         */
        StockTransaction t = new StockTransaction();

        t.setProduct(p);
        t.setType(req.type());
        t.setQuantity(req.quantity());
        t.setNote(req.note());
        t.setCreatedBy(me);

        /*
         * =====================================================
         * ADMIN TẠO PHIẾU
         *
         * → duyệt ngay
         * → cập nhật tồn kho ngay
         * =====================================================
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
         * =====================================================
         * MANAGER / USER
         *
         * → tạo PENDING
         * → chưa cập nhật tồn kho
         * =====================================================
         */
        t.setStatus(TxStatus.PENDING);

        StockTransaction saved = repo.save(t);

        /*
         * Thông báo cho ADMIN và MANAGER
         */
        notifyManagers(
                "🔔 Yêu cầu kho mới",
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

    /*
     * =========================================================
     * DUYỆT PHIẾU
     *
     * ADMIN + MANAGER được duyệt
     * USER không được duyệt
     * =========================================================
     */
    @PostMapping("/{id}/approve")
    @Transactional
    public StockTransaction approve(
            @PathVariable Long id,
            @RequestAttribute("user") AppUser me
    ) {

        if (me.getRole() != Role.ADMIN
                && me.getRole() != Role.MANAGER) {

            throw new IllegalArgumentException(
                    "Chỉ ADMIN hoặc MANAGER mới được duyệt phiếu"
            );
        }

        StockTransaction t = pending(id);

        /*
         * Cập nhật tồn kho
         *
         * Nếu là OUT thì apply() sẽ kiểm tra
         * tồn kho một lần nữa tại thời điểm duyệt.
         */
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
                            + " đã được "
                            + approverText(me)
                            + " duyệt.",
                    saved
            );
        }

        return saved;
    }

    /*
     * =========================================================
     * TỪ CHỐI PHIẾU
     *
     * ADMIN + MANAGER được từ chối
     * USER không được từ chối
     * =========================================================
     */
    @PostMapping("/{id}/reject")
    @Transactional
    public StockTransaction reject(
            @PathVariable Long id,
            @RequestAttribute("user") AppUser me
    ) {

        if (me.getRole() != Role.ADMIN
                && me.getRole() != Role.MANAGER) {

            throw new IllegalArgumentException(
                    "Chỉ ADMIN hoặc MANAGER mới được từ chối phiếu"
            );
        }

        StockTransaction t = pending(id);

        /*
         * Từ chối → không thay đổi tồn kho
         */
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
                            + " đã bị "
                            + approverText(me)
                            + " từ chối.",
                    saved
            );
        }

        return saved;
    }

    /*
     * =========================================================
     * HỦY PHIẾU
     *
     * ADMIN   → được hủy phiếu PENDING của bất kỳ ai
     * MANAGER → chỉ được hủy phiếu do mình tạo
     * USER    → chỉ được hủy phiếu do mình tạo
     * =========================================================
     */
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

    /*
     * =========================================================
     * LẤY PHIẾU PENDING
     * =========================================================
     */
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

    /*
     * =========================================================
     * CẬP NHẬT TỒN KHO
     *
     * IN  → cộng tồn
     * OUT → trừ tồn
     * =========================================================
     */
    private void apply(
            StockTransaction t,
            Product p
    ) {

        if (p == null) {

            throw new IllegalArgumentException(
                    "Sản phẩm của phiếu không tồn tại"
            );
        }

        if (t.getType() == TxType.IN) {

            p.setQuantity(
                    p.getQuantity()
                            + t.getQuantity()
            );

        } else {

            /*
             * Kiểm tra lại tồn kho ngay lúc duyệt
             */
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

    /*
     * =========================================================
     * GỬI THÔNG BÁO CHO ADMIN + MANAGER
     * =========================================================
     */
    private void notifyManagers(
            String title,
            String message,
            StockTransaction transaction
    ) {

        List<AppUser> allUsers = users.findAll();

        for (AppUser manager : allUsers) {

            if (manager.getRole() != Role.ADMIN
                    && manager.getRole() != Role.MANAGER) {

                continue;
            }

            notifyUser(
                    manager,
                    title,
                    message,
                    transaction
            );
        }
    }

    /*
     * =========================================================
     * TẠO THÔNG BÁO
     * =========================================================
     */
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

    /*
     * =========================================================
     * TÊN SẢN PHẨM
     * =========================================================
     */
    private String productName(
            StockTransaction t
    ) {

        if (t.getProduct() == null) {
            return "Sản phẩm đã xóa";
        }

        return t.getProduct().getName();
    }

    /*
     * =========================================================
     * HIỂN THỊ LOẠI PHIẾU
     * =========================================================
     */
    private String typeText(TxType type) {

        return type == TxType.OUT
                ? "xuất kho"
                : "nhập kho";
    }

    /*
     * =========================================================
     * HIỂN THỊ NGƯỜI DUYỆT
     * =========================================================
     */
    private String approverText(AppUser user) {

        if (user.getRole() == Role.ADMIN) {
            return "ADMIN";
        }

        if (user.getRole() == Role.MANAGER) {
            return "MANAGER";
        }

        return "người quản lý";
    }
}