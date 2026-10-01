package com.wingsup.warehouse.controller;

import com.wingsup.warehouse.model.AppUser;
import com.wingsup.warehouse.model.Notification;
import com.wingsup.warehouse.model.Role;
import com.wingsup.warehouse.repository.NotificationRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * ADMIN + MANAGER:
     * → Cùng xem toàn bộ thông báo quản lý kho
     *
     * USER:
     * → Chỉ xem thông báo của chính mình
     * =========================================================
     */
    @GetMapping
    public List<Notification> list(
            @RequestAttribute("user") AppUser me
    ) {

        List<Notification> all =
                repo.findAll();

        /*
         * ADMIN và MANAGER dùng chung danh sách
         */
        if (me.getRole() == Role.ADMIN
                || me.getRole() == Role.MANAGER) {

            return all.stream()
                    .filter(n ->
                            n.getUser() != null
                    )
                    .filter(n -> {

                        Role role =
                                n.getUser().getRole();

                        return role == Role.ADMIN
                                || role == Role.MANAGER;

                    })
                    .sorted((a, b) -> {

                        if (a.getCreatedAt() == null) {
                            return 1;
                        }

                        if (b.getCreatedAt() == null) {
                            return -1;
                        }

                        return b.getCreatedAt()
                                .compareTo(
                                        a.getCreatedAt()
                                );
                    })
                    .collect(Collectors.toList());
        }

        /*
         * USER chỉ xem thông báo của mình
         */
        return repo.findByUserOrderByCreatedAtDesc(me);
    }

    /*
     * =========================================================
     * SỐ THÔNG BÁO CHƯA ĐỌC
     *
     * ADMIN + MANAGER:
     * → Đếm toàn bộ thông báo chưa đọc của ADMIN/MANAGER
     *
     * USER:
     * → Đếm thông báo chưa đọc của chính mình
     * =========================================================
     */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(
            @RequestAttribute("user") AppUser me
    ) {

        if (me.getRole() == Role.ADMIN
                || me.getRole() == Role.MANAGER) {

            long count =
                    repo.findAll()
                            .stream()
                            .filter(n ->
                                    n.getUser() != null
                            )
                            .filter(n -> {

                                Role role =
                                        n.getUser().getRole();

                                return role == Role.ADMIN
                                        || role == Role.MANAGER;

                            })
                            .filter(n ->
                                    !n.isRead()
                            )
                            .count();

            return Map.of(
                    "count",
                    count
            );
        }

        return Map.of(
                "count",
                repo.countByUserAndReadFalse(me)
        );
    }

    /*
     * =========================================================
     * ĐÁNH DẤU 1 THÔNG BÁO ĐÃ ĐỌC
     *
     * ADMIN + MANAGER:
     * → Có thể đánh dấu thông báo quản lý kho
     *
     * USER:
     * → Chỉ được đánh dấu thông báo của mình
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
         * ADMIN + MANAGER có thể đọc
         * thông báo quản lý kho.
         */
        if (me.getRole() == Role.ADMIN
                || me.getRole() == Role.MANAGER) {

            Role notificationRole =
                    n.getUser().getRole();

            if (notificationRole != Role.ADMIN
                    && notificationRole != Role.MANAGER) {

                throw new IllegalArgumentException(
                        "Bạn không có quyền xem thông báo này"
                );
            }

        } else {

            /*
             * USER chỉ được đọc thông báo của mình
             */
            if (!n.getUser()
                    .getId()
                    .equals(me.getId())) {

                throw new IllegalArgumentException(
                        "Bạn không có quyền xem thông báo này"
                );
            }
        }

        n.setRead(true);

        return repo.save(n);
    }

    /*
     * =========================================================
     * ĐÁNH DẤU TẤT CẢ ĐÃ ĐỌC
     *
     * ADMIN + MANAGER:
     * → Đánh dấu toàn bộ thông báo quản lý kho
     *
     * USER:
     * → Đánh dấu thông báo của mình
     * =========================================================
     */
    @PostMapping("/read-all")
    public Map<String, String> readAll(
            @RequestAttribute("user") AppUser me
    ) {

        List<Notification> list;

        if (me.getRole() == Role.ADMIN
                || me.getRole() == Role.MANAGER) {

            list = repo.findAll()
                    .stream()
                    .filter(n ->
                            n.getUser() != null
                    )
                    .filter(n -> {

                        Role role =
                                n.getUser().getRole();

                        return role == Role.ADMIN
                                || role == Role.MANAGER;

                    })
                    .collect(Collectors.toList());

        } else {

            list =
                    repo.findByUserOrderByCreatedAtDesc(
                            me
                    );
        }

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