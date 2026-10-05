package com.wingsup.warehouse.controller;

import com.wingsup.warehouse.model.AppUser;
import com.wingsup.warehouse.model.Role;
import com.wingsup.warehouse.repository.AppUserRepository;
import com.wingsup.warehouse.security.AuthInterceptor;
import com.wingsup.warehouse.security.TokenStore;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserRepository users;
    private final TokenStore tokens;
    private final PasswordEncoder encoder;
    private final JavaMailSender mailSender;

    /*
     * =========================================================
     * LƯU OTP TẠM THỜI TRONG RAM
     *
     * email -> thông tin OTP
     *
     * OTP:
     * - Hết hạn sau 5 phút
     * - Chỉ gửi lại sau 60 giây
     * - Tối đa 5 lần trong 15 phút
     * =========================================================
     */
    private final Map<String, OtpData> otpStore =
            new ConcurrentHashMap<>();

    private final Random random = new Random();

    public AuthController(
            AppUserRepository users,
            TokenStore tokens,
            PasswordEncoder encoder,
            JavaMailSender mailSender
    ) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.mailSender = mailSender;
    }

    public record LoginRequest(
            String username,
            String password
    ) {
    }

    public record RegisterRequest(
            String username,
            String password,
            String fullName,
            String email
    ) {
    }

    public record PasswordRequest(
            String oldPassword,
            String newPassword
    ) {
    }

    public record ForgotPasswordRequest(
            String email
    ) {
    }

    public record ResetPasswordRequest(
            String email,
            String code,
            String newPassword
    ) {
    }

    /*
     * =========================================================
     * DỮ LIỆU OTP
     * =========================================================
     */
    private record OtpData(
            String code,
            LocalDateTime expiresAt,
            LocalDateTime lastSentAt,
            int sendCount,
            LocalDateTime windowStartedAt
    ) {
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @PostMapping("/login")
    public Map<String, Object> login(
            @RequestBody LoginRequest req
    ) {

        System.out.println("========== LOGIN ==========");

        System.out.println(
                "Username nhận được: "
                + req.username()
        );

        System.out.println(
                "Password có dữ liệu: "
                + (
                        req.password() != null
                        && !req.password().isBlank()
                )
        );

        if (req.username() == null
                || req.username().isBlank()) {

            throw new IllegalArgumentException(
                    "Tên đăng nhập không được để trống"
            );
        }

        if (req.password() == null
                || req.password().isBlank()) {

            throw new IllegalArgumentException(
                    "Mật khẩu không được để trống"
            );
        }

        String username =
                req.username().trim();

        System.out.println(
                "Đang tìm user: "
                + username
        );

        AppUser user =
                users.findByUsername(username)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Tài khoản không tồn tại: "
                                        + username
                                )
                        );

        System.out.println(
                "Đã tìm thấy user ID: "
                + user.getId()
        );

        if (user.getPassword() == null
                || user.getPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "Tài khoản chưa có mật khẩu trong database"
            );
        }

        boolean passwordCorrect =
                encoder.matches(
                        req.password(),
                        user.getPassword()
                );

        System.out.println(
                "Mật khẩu đúng: "
                + passwordCorrect
        );

        if (!passwordCorrect) {

            throw new IllegalArgumentException(
                    "Mật khẩu không đúng"
            );
        }

        /*
         * =====================================================
         * TẠO PHIÊN ĐĂNG NHẬP MỚI
         *
         * Mỗi lần đăng nhập:
         * - Tạo sessionId mới
         * - sessionId cũ bị vô hiệu hóa
         * - Token cũ sẽ không còn hợp lệ
         *
         * Vì vậy:
         * 1 tài khoản = 1 phiên đăng nhập
         * =====================================================
         */

        String sessionId =
                UUID.randomUUID().toString();

        user.setSessionId(sessionId);

        user.setActive(true);

        user.setLastActiveAt(
                LocalDateTime.now()
        );

        users.save(user);

        /*
         * Token được gắn với sessionId hiện tại
         */
        String token =
                tokens.create(
                        user.getId(),
                        sessionId
                );

        System.out.println(
                "LOGIN THÀNH CÔNG - User ID: "
                + user.getId()
        );

        System.out.println(
                "SESSION ID: "
                + sessionId
        );

        return Map.of(
                "token",
                token,
                "user",
                user
        );
    }

    // =========================================================
    // HEARTBEAT
    // =========================================================

    @PostMapping("/heartbeat")
    public Map<String, String> heartbeat(
            @RequestAttribute("user") AppUser me
    ) {

        me.setActive(true);

        me.setLastActiveAt(
                LocalDateTime.now()
        );

        users.save(me);

        return Map.of(
                "message",
                "OK"
        );
    }

    // =========================================================
    // REGISTER
    // =========================================================

    @PostMapping("/register")
    public Map<String, String> register(
            @RequestBody RegisterRequest req
    ) {

        if (req.username() == null
                || req.username().isBlank()) {

            throw new IllegalArgumentException(
                    "Tên đăng nhập không được để trống"
            );
        }

        if (!req.username().matches(
                "^[a-zA-Z0-9_]+$"
        )) {

            throw new IllegalArgumentException(
                    "Tên đăng nhập không được có dấu hoặc ký tự đặc biệt"
            );
        }

        if (req.password() == null
                || req.password().isBlank()) {

            throw new IllegalArgumentException(
                    "Mật khẩu không được để trống"
            );
        }

        validatePassword(
                req.password()
        );

        if (req.fullName() == null
                || req.fullName().isBlank()) {

            throw new IllegalArgumentException(
                    "Họ tên không được để trống"
            );
        }

        if (req.email() == null
                || req.email().isBlank()) {

            throw new IllegalArgumentException(
                    "Email không được để trống"
            );
        }

        String email =
                req.email()
                        .trim()
                        .toLowerCase();

        if (!isValidEmail(email)) {

            throw new IllegalArgumentException(
                    "Email không hợp lệ"
            );
        }

        if (users.findByUsername(
                req.username().trim()
        ).isPresent()) {

            throw new IllegalArgumentException(
                    "Tên đăng nhập đã tồn tại"
            );
        }

        /*
         * Không cho 2 tài khoản dùng cùng email
         */
        if (users.findByEmail(email).isPresent()) {

            throw new IllegalArgumentException(
                    "Email đã được sử dụng"
            );
        }

        AppUser user =
                new AppUser();

        user.setUsername(
                req.username().trim()
        );

        user.setPassword(
                encoder.encode(
                        req.password()
                )
        );

        user.setFullName(
                req.fullName().trim()
        );

        user.setEmail(email);

        /*
         * Người tự đăng ký chỉ được tạo USER
         */
        user.setRole(Role.USER);

        /*
         * Tài khoản mới chưa đăng nhập
         */
        user.setActive(false);

        user.setLastActiveAt(null);

        /*
         * Chưa có phiên đăng nhập
         */
        user.setSessionId(null);

        users.save(user);

        return Map.of(
                "message",
                "Tạo tài khoản thành công"
        );
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    @PostMapping("/logout")
    public Map<String, String> logout(
            HttpServletRequest req,
            @RequestAttribute("user") AppUser me
    ) {

        /*
         * Xóa phiên đăng nhập hiện tại
         */
        me.setSessionId(null);

        me.setActive(false);

        me.setLastActiveAt(null);

        users.save(me);

        /*
         * Xóa token hiện tại khỏi TokenStore
         */
        tokens.remove(
                AuthInterceptor.extractToken(req)
        );

        return Map.of(
                "message",
                "Đã đăng xuất"
        );
    }

    // =========================================================
    // ME
    // =========================================================

    @GetMapping("/me")
    public AppUser me(
            @RequestAttribute("user") AppUser me
    ) {
        return me;
    }

    // =========================================================
    // ĐỔI MẬT KHẨU KHI ĐÃ ĐĂNG NHẬP
    // =========================================================

    @PutMapping("/password")
    public Map<String, String> changePassword(
            @RequestAttribute("user") AppUser me,
            @RequestBody PasswordRequest req
    ) {

        if (req == null
                || req.oldPassword() == null
                || req.oldPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "Mật khẩu cũ không được để trống"
            );
        }

        if (!encoder.matches(
                req.oldPassword(),
                me.getPassword()
        )) {

            throw new IllegalArgumentException(
                    "Mật khẩu cũ không đúng"
            );
        }

        if (req.newPassword() == null
                || req.newPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "Mật khẩu mới không được để trống"
            );
        }

        validatePassword(
                req.newPassword()
        );

        if (encoder.matches(
                req.newPassword(),
                me.getPassword()
        )) {

            throw new IllegalArgumentException(
                    "Mật khẩu mới phải khác mật khẩu cũ"
            );
        }

        me.setPassword(
                encoder.encode(
                        req.newPassword()
                )
        );

        users.save(me);

        return Map.of(
                "message",
                "Đổi mật khẩu thành công"
        );
    }

    // =========================================================
    // QUÊN MẬT KHẨU - GỬI OTP
    // =========================================================

    @PostMapping("/forgot-password")
    public synchronized Map<String, String> forgotPassword(
            @RequestBody ForgotPasswordRequest req
    ) {

        if (req == null
                || req.email() == null
                || req.email().isBlank()) {

            throw new IllegalArgumentException(
                    "Email không được để trống"
            );
        }

        String email =
                req.email()
                        .trim()
                        .toLowerCase();

        if (!isValidEmail(email)) {

            throw new IllegalArgumentException(
                    "Email không hợp lệ"
            );
        }

        /*
         * Tìm tài khoản theo email
         */
        AppUser user =
                users.findByEmail(email)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Email không tồn tại trong hệ thống"
                                )
                        );

        LocalDateTime now =
                LocalDateTime.now();

        OtpData oldOtp =
                otpStore.get(email);

        if (oldOtp != null) {

            /*
             * Chặn gửi liên tục
             */
            if (oldOtp.lastSentAt() != null) {

                long seconds =
                        Duration.between(
                                oldOtp.lastSentAt(),
                                now
                        ).getSeconds();

                if (seconds < 60) {

                    long remaining =
                            60 - seconds;

                    throw new IllegalArgumentException(
                            "Vui lòng chờ "
                            + remaining
                            + " giây trước khi yêu cầu OTP mới"
                    );
                }
            }

            /*
             * Kiểm tra cửa sổ 15 phút
             */
            LocalDateTime windowStart =
                    oldOtp.windowStartedAt();

            int sendCount =
                    oldOtp.sendCount();

            if (windowStart == null
                    || Duration.between(
                            windowStart,
                            now
                    ).toMinutes() >= 15) {

                windowStart = now;
                sendCount = 0;
            }

            /*
             * Tối đa 5 lần / 15 phút
             */
            if (sendCount >= 5) {

                long elapsedMinutes =
                        Duration.between(
                                windowStart,
                                now
                        ).toMinutes();

                long remainingMinutes =
                        15 - elapsedMinutes;

                if (remainingMinutes < 1) {
                    remainingMinutes = 1;
                }

                throw new IllegalArgumentException(
                        "Bạn đã yêu cầu OTP quá nhiều lần. "
                        + "Vui lòng thử lại sau "
                        + remainingMinutes
                        + " phút"
                );
            }

            String code =
                    String.format(
                            "%06d",
                            random.nextInt(1_000_000)
                    );

            LocalDateTime expiresAt =
                    now.plusMinutes(5);

            try {

                sendOtpEmail(
                        user,
                        code
                );

            } catch (Exception e) {

                throw new IllegalArgumentException(
                        "Không thể gửi email OTP. "
                        + "Vui lòng thử lại sau."
                );
            }

            sendCount++;

            otpStore.put(
                    email,
                    new OtpData(
                            code,
                            expiresAt,
                            now,
                            sendCount,
                            windowStart
                    )
            );

            return Map.of(
                    "message",
                    "Mã OTP đã được gửi đến email của bạn"
            );
        }

        /*
         * Lần đầu yêu cầu OTP
         */
        String code =
                String.format(
                        "%06d",
                        random.nextInt(1_000_000)
                );

        LocalDateTime expiresAt =
                now.plusMinutes(5);

        try {

            sendOtpEmail(
                    user,
                    code
            );

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Không thể gửi email OTP. "
                    + "Vui lòng thử lại sau."
            );
        }

        otpStore.put(
                email,
                new OtpData(
                        code,
                        expiresAt,
                        now,
                        1,
                        now
                )
        );

        return Map.of(
                "message",
                "Mã OTP đã được gửi đến email của bạn"
        );
    }

    // =========================================================
    // GỬI EMAIL OTP
    // =========================================================

    private void sendOtpEmail(
            AppUser user,
            String code
    ) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(
                user.getEmail()
        );

        message.setSubject(
                "Wings Up - Mã OTP đặt lại mật khẩu"
        );

        message.setText(
                "Xin chào "
                + user.getFullName()
                + ",\n\n"

                + "Bạn vừa yêu cầu đặt lại mật khẩu "
                + "cho tài khoản Wings Up.\n\n"

                + "Mã OTP của bạn là:\n\n"

                + code
                + "\n\n"

                + "Mã OTP có hiệu lực trong 5 phút.\n\n"

                + "Nếu bạn không thực hiện yêu cầu này, "
                + "vui lòng bỏ qua email.\n\n"

                + "Wings Up - Edu Success"
        );

        mailSender.send(message);
    }

    // =========================================================
    // QUÊN MẬT KHẨU - ĐẶT LẠI
    // =========================================================

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(
            @RequestBody ResetPasswordRequest req
    ) {

        if (req == null) {

            throw new IllegalArgumentException(
                    "Dữ liệu không hợp lệ"
            );
        }

        if (req.email() == null
                || req.email().isBlank()) {

            throw new IllegalArgumentException(
                    "Email không được để trống"
            );
        }

        if (req.code() == null
                || req.code().isBlank()) {

            throw new IllegalArgumentException(
                    "Mã OTP không được để trống"
            );
        }

        if (req.newPassword() == null
                || req.newPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "Mật khẩu mới không được để trống"
            );
        }

        String email =
                req.email()
                        .trim()
                        .toLowerCase();

        String code =
                req.code().trim();

        if (!code.matches("\\d{6}")) {

            throw new IllegalArgumentException(
                    "Mã OTP phải gồm 6 chữ số"
            );
        }

        validatePassword(
                req.newPassword()
        );

        OtpData otp =
                otpStore.get(email);

        if (otp == null) {

            throw new IllegalArgumentException(
                    "Bạn chưa yêu cầu mã OTP"
            );
        }

        if (LocalDateTime.now()
                .isAfter(otp.expiresAt())) {

            otpStore.remove(email);

            throw new IllegalArgumentException(
                    "Mã OTP đã hết hạn. Vui lòng gửi mã mới"
            );
        }

        if (!otp.code().equals(code)) {

            throw new IllegalArgumentException(
                    "Mã OTP không đúng"
            );
        }

        AppUser user =
                users.findByEmail(email)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Email không tồn tại trong hệ thống"
                                )
                        );

        if (encoder.matches(
                req.newPassword(),
                user.getPassword()
        )) {

            throw new IllegalArgumentException(
                    "Mật khẩu mới phải khác mật khẩu cũ"
            );
        }

        user.setPassword(
                encoder.encode(
                        req.newPassword()
                )
        );

        /*
         * =====================================================
         * RESET PASSWORD = HỦY PHIÊN ĐĂNG NHẬP CŨ
         *
         * Nếu tài khoản đang đăng nhập ở trình duyệt khác,
         * sau khi reset mật khẩu phiên đó sẽ bị đăng xuất.
         * =====================================================
         */
        user.setSessionId(null);
        user.setActive(false);
        user.setLastActiveAt(null);

        users.save(user);

        otpStore.remove(email);

        return Map.of(
                "message",
                "Đặt lại mật khẩu thành công"
        );
    }

    // =========================================================
    // KIỂM TRA MẬT KHẨU
    // =========================================================

    private void validatePassword(
            String password
    ) {

        if (password.length() < 6) {

            throw new IllegalArgumentException(
                    "Mật khẩu phải từ 6 ký tự"
            );
        }

        if (!password.matches(
                ".*[A-Z].*"
        )) {

            throw new IllegalArgumentException(
                    "Mật khẩu phải có ít nhất 1 chữ hoa"
            );
        }

        if (!password.matches(
                ".*[0-9].*"
        )) {

            throw new IllegalArgumentException(
                    "Mật khẩu phải có ít nhất 1 chữ số"
            );
        }

        if (!password.matches(
                ".*[^a-zA-Z0-9].*"
        )) {

            throw new IllegalArgumentException(
                    "Mật khẩu phải có ít nhất 1 ký tự đặc biệt"
            );
        }
    }

    // =========================================================
    // KIỂM TRA EMAIL
    // =========================================================

    private boolean isValidEmail(
            String email
    ) {

        return email.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        );
    }
}