package com.wingsup.warehouse.controller;

import com.wingsup.warehouse.model.AppUser;
import com.wingsup.warehouse.model.Notification;
import com.wingsup.warehouse.model.Role;
import com.wingsup.warehouse.repository.NotificationRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository repo;

    public NotificationController(
            NotificationRepository repo
    ) {
        this.repo = repo;
    }

    /*
     * =========================================================
     * DANH SÁCH THÔNG BÁO
     *
     * ADMIN / MANAGER:
     * → Chỉ xem thông báo của chính tài khoản đang đăng nhập
     *
     * USER:
     * → Chỉ xem thông báo của chính mình
     * =========================================================
     */
    @GetMapping
    public List<Notification> list(
            @RequestAttribute("user") AppUser me
    ) {

        return repo.findByUserOrderByCreatedAtDesc(me);
    }

    /*
     * =========================================================
     * SỐ THÔNG BÁO CHƯA ĐỌC
     * =========================================================
     */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(
            @RequestAttribute("user") AppUser me
    ) {

        return Map.of(
                "count",
                repo.countByUserAndReadFalse(me)
        );
    }

    /*
     * =========================================================
     * ĐÁNH DẤU 1 THÔNG BÁO ĐÃ ĐỌC
     * =========================================================
     */
    @PostMapping("/{id}/read")
    public Notification read(
            @PathVariable Long id,
            @RequestAttribute("user") AppUser me
    ) {

        Notification n =
                repo.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Không tìm thấy thông báo"
                                )
                        );

        if (n.getUser() == null) {
            throw new IllegalArgumentException(
                    "Thông báo không hợp lệ"
            );
        }

        /*
         * Chỉ tài khoản nhận notification
         * mới được đánh dấu đã đọc.
         */
        if (!n.getUser()
                .getId()
                .equals(me.getId())) {

            throw new IllegalArgumentException(
                    "Bạn không có quyền xem thông báo này"
            );
        }

        n.setRead(true);

        return repo.save(n);
    }

    /*
     * =========================================================
     * ĐÁNH DẤU TẤT CẢ ĐÃ ĐỌC
     * =========================================================
     */
    @PostMapping("/read-all")
    public Map<String, String> readAll(
            @RequestAttribute("user") AppUser me
    ) {

        List<Notification> list =
                repo.findByUserOrderByCreatedAtDesc(me);

        for (Notification n : list) {
            n.setRead(true);
        }

        repo.saveAll(list);

        return Map.of(
                "message",
                "Đã đọc tất cả thông báo"
        );
    }
}