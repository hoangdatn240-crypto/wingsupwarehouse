/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.wingsup.warehouse.controller;

import com.wingsup.warehouse.model.AppUser;
import com.wingsup.warehouse.model.Notification;
import com.wingsup.warehouse.repository.NotificationRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository repo;

    public NotificationController(NotificationRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Notification> list(
            @RequestAttribute("user") AppUser me
    ) {
        return repo.findByUserOrderByCreatedAtDesc(me);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(
            @RequestAttribute("user") AppUser me
    ) {
        return Map.of(
                "count",
                repo.countByUserAndReadFalse(me)
        );
    }

    @PostMapping("/{id}/read")
    public Notification read(
            @PathVariable Long id,
            @RequestAttribute("user") AppUser me
    ) {
        Notification n = repo.findById(id)
                .orElseThrow(()
                        -> new IllegalArgumentException(
                        "Không tìm thấy thông báo"
                )
                );

        if (n.getUser() == null
                || !n.getUser().getId().equals(me.getId())) {
            throw new IllegalArgumentException(
                    "Bạn không có quyền xem thông báo này"
            );
        }

        n.setRead(true);

        return repo.save(n);
    }

    @PostMapping("/read-all")
    public Map<String, String> readAll(
            @RequestAttribute("user") AppUser me
    ) {
        List<Notification> list
                = repo.findByUserOrderByCreatedAtDesc(me);

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
