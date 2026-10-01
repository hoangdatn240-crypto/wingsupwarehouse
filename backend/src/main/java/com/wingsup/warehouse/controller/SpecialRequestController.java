package com.wingsup.warehouse.controller;

import com.wingsup.warehouse.model.AppUser;
import com.wingsup.warehouse.model.Notification;
import com.wingsup.warehouse.model.Role;
import com.wingsup.warehouse.model.SpecialRequest;
import com.wingsup.warehouse.model.SpecialRequestMessage;
import com.wingsup.warehouse.repository.AppUserRepository;
import com.wingsup.warehouse.repository.NotificationRepository;
import com.wingsup.warehouse.repository.SpecialRequestMessageRepository;
import com.wingsup.warehouse.repository.SpecialRequestRepository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/special-requests")
public class SpecialRequestController {

    private final SpecialRequestRepository requests;
    private final SpecialRequestMessageRepository messages;
    private final AppUserRepository users;
    private final NotificationRepository notifications;

    public SpecialRequestController(
            SpecialRequestRepository requests,
            SpecialRequestMessageRepository messages,
            AppUserRepository users,
            NotificationRepository notifications
    ) {
        this.requests = requests;
        this.messages = messages;
        this.users = users;
        this.notifications = notifications;
    }

    public record CreateRequest(
            String subject,
            String message) {

    }

    public record SendMessageRequest(
            String message) {

    }

    /*
     * =========================================================
     * DANH SÁCH CUỘC TRÒ CHUYỆN
     * =========================================================
     */
    @GetMapping
    public List<SpecialRequest> list(
            @RequestAttribute("user") AppUser me
    ) {

        if (me.getRole() == Role.ADMIN
                || me.getRole() == Role.MANAGER) {

            return requests.findAllByOrderByUpdatedAtDesc();
        }

        return requests.findByUserOrderByUpdatedAtDesc(me);
    }

    /*
     * =========================================================
     * TẠO YÊU CẦU MỚI
     *
     * USER tạo yêu cầu
     * → tạo SpecialRequest
     * → tạo tin nhắn đầu tiên
     * → tạo thông báo cho ADMIN
     * → tạo thông báo cho MANAGER
     * =========================================================
     */
    @PostMapping
    @Transactional
    public SpecialRequest create(
            @RequestBody CreateRequest req,
            @RequestAttribute("user") AppUser me
    ) {

        if (req.subject() == null
                || req.subject().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Vui lòng nhập tiêu đề yêu cầu"
            );
        }

        if (req.message() == null
                || req.message().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Vui lòng nhập nội dung yêu cầu"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        /*
         * =========================
         * TẠO YÊU CẦU
         * =========================
         */
        SpecialRequest request
                = new SpecialRequest();

        request.setUser(me);

        request.setSubject(
                req.subject().trim()
        );

        request.setStatus("OPEN");

        request.setCreatedAt(now);
        request.setUpdatedAt(now);

        SpecialRequest saved
                = requests.save(request);

        /*
         * =========================
         * TẠO TIN NHẮN ĐẦU TIÊN
         * =========================
         */
        SpecialRequestMessage message
                = new SpecialRequestMessage();

        message.setRequest(saved);
        message.setSender(me);

        message.setMessage(
                req.message().trim()
        );

        message.setCreatedAt(now);
        message.setRead(false);

        messages.save(message);

        /*
 * =========================
 * TẠO THÔNG BÁO
 * CHO ADMIN + MANAGER
 * =========================
         */
        List<AppUser> managers
                = users.findAll()
                        .stream()
                        .filter(u
                                -> u.getId() != null
                        && (u.getRole() == Role.ADMIN
                        || u.getRole() == Role.MANAGER)
                        && !u.getId().equals(me.getId())
                        )
                        .toList();

        String senderName
                = me.getFullName() != null
                && !me.getFullName().trim().isEmpty()
                ? me.getFullName().trim()
                : me.getUsername();

        String content
                = senderName
                + " đã gửi yêu cầu: "
                + req.subject().trim();

        for (AppUser manager : managers) {

            /*
     * Kiểm tra xem notification giống hệt
     * đã tồn tại trong thời gian vừa tạo hay chưa.
             */
            List<Notification> existing
                    = notifications.findAll()
                            .stream()
                            .filter(n
                                    -> n.getUser() != null
                            && n.getUser().getId()
                                    .equals(manager.getId())
                            && "🔔 Yêu cầu đặc biệt mới"
                                    .equals(n.getTitle())
                            && content.equals(n.getMessage())
                            && n.getCreatedAt() != null
                            && Math.abs(
                                    java.time.Duration.between(
                                            n.getCreatedAt(),
                                            now
                                    ).toSeconds()
                            ) <= 2
                            )
                            .toList();

            /*
     * Đã có notification giống nhau
     * → không tạo thêm.
             */
            if (!existing.isEmpty()) {
                continue;
            }

            Notification notification
                    = new Notification();

            notification.setUser(manager);

            notification.setTitle(
                    "🔔 Yêu cầu đặc biệt mới"
            );

            notification.setMessage(content);

            notification.setRead(false);
            notification.setCreatedAt(now);
            notification.setTransaction(null);

            notifications.save(notification);
        }
        return saved;
    }

    /*
     * =========================================================
     * XEM TIN NHẮN
     * =========================================================
     */
    @GetMapping("/{id}/messages")
    public List<SpecialRequestMessage> getMessages(
            @PathVariable Long id,
            @RequestAttribute("user") AppUser me
    ) {

        SpecialRequest request
                = getRequest(id);

        checkPermission(request, me);

        return messages
                .findByRequestOrderByCreatedAtAsc(request);
    }

    /*
     * =========================================================
     * GỬI TIN NHẮN
     * =========================================================
     */
    @PostMapping("/{id}/messages")
    @Transactional
    public SpecialRequestMessage sendMessage(
            @PathVariable Long id,
            @RequestBody SendMessageRequest req,
            @RequestAttribute("user") AppUser me
    ) {

        if (req.message() == null
                || req.message().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Nội dung tin nhắn không được để trống"
            );
        }

        SpecialRequest request
                = getRequest(id);

        checkPermission(request, me);

        LocalDateTime now
                = LocalDateTime.now();

        SpecialRequestMessage message
                = new SpecialRequestMessage();

        message.setRequest(request);
        message.setSender(me);

        message.setMessage(
                req.message().trim()
        );

        message.setCreatedAt(now);
        message.setRead(false);

        request.setUpdatedAt(now);

        requests.save(request);

        return messages.save(message);
    }

    /*
     * =========================================================
     * ĐÁNH DẤU TIN NHẮN ĐÃ ĐỌC
     * =========================================================
     */
    @PostMapping("/{id}/read")
    @Transactional
    public Map<String, String> read(
            @PathVariable Long id,
            @RequestAttribute("user") AppUser me
    ) {

        SpecialRequest request
                = getRequest(id);

        checkPermission(request, me);

        List<SpecialRequestMessage> list
                = messages.findByRequestOrderByCreatedAtAsc(request);

        for (SpecialRequestMessage message : list) {

            if (message.getSender() == null) {
                continue;
            }

            if (!message.getSender()
                    .getId()
                    .equals(me.getId())) {

                message.setRead(true);
            }
        }

        messages.saveAll(list);

        return Map.of(
                "message",
                "Đã đọc tin nhắn"
        );
    }

    /*
     * =========================================================
     * ĐÓNG YÊU CẦU
     * =========================================================
     */
    @PostMapping("/{id}/close")
    @Transactional
    public SpecialRequest close(
            @PathVariable Long id,
            @RequestAttribute("user") AppUser me
    ) {

        if (me.getRole() != Role.ADMIN
                && me.getRole() != Role.MANAGER) {

            throw new IllegalArgumentException(
                    "Chỉ ADMIN hoặc MANAGER mới được đóng yêu cầu"
            );
        }

        SpecialRequest request
                = getRequest(id);

        request.setStatus("CLOSED");
        request.setUpdatedAt(
                LocalDateTime.now()
        );

        return requests.save(request);
    }

    /*
 * =========================================================
 * XÓA YÊU CẦU / CHAT
 *
 * ADMIN / MANAGER mới được xóa.
 *
 * → Xóa toàn bộ tin nhắn
 * → Xóa yêu cầu
 * =========================================================
     */
    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, String> delete(
            @PathVariable Long id,
            @RequestAttribute("user") AppUser me
    ) {

        if (me.getRole() != Role.ADMIN
                && me.getRole() != Role.MANAGER) {

            throw new IllegalArgumentException(
                    "Chỉ ADMIN hoặc MANAGER mới được xóa yêu cầu"
            );
        }

        SpecialRequest request
                = getRequest(id);

        /*
     * Xóa toàn bộ tin nhắn trước
         */
        List<SpecialRequestMessage> list
                = messages.findByRequestOrderByCreatedAtAsc(
                        request
                );

        if (!list.isEmpty()) {
            messages.deleteAll(list);
        }

        /*
     * Xóa yêu cầu
         */
        requests.delete(request);

        return Map.of(
                "message",
                "Đã xóa cuộc trò chuyện"
        );
    }

    /*
     * =========================================================
     * KIỂM TRA QUYỀN
     * =========================================================
     */
    private void checkPermission(
            SpecialRequest request,
            AppUser me
    ) {

        boolean manager
                = me.getRole() == Role.ADMIN
                || me.getRole() == Role.MANAGER;

        boolean owner
                = request.getUser() != null
                && request.getUser()
                        .getId()
                        .equals(me.getId());

        if (!manager && !owner) {

            throw new IllegalArgumentException(
                    "Bạn không có quyền truy cập yêu cầu này"
            );
        }
    }

    private SpecialRequest getRequest(Long id) {

        return requests.findById(id)
                .orElseThrow(()
                        -> new IllegalArgumentException(
                        "Không tìm thấy yêu cầu"
                )
                );
    }
}
