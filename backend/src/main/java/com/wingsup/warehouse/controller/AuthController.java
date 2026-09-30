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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserRepository users;
    private final TokenStore tokens;
    private final PasswordEncoder encoder;
    private final JavaMailSender mailSender;

    /*
     * Lưu OTP tạm thời trong RAM
     *
     * email -> OTP
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

    private record OtpData(
            String code,
            LocalDateTime expiresAt
    ) {
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @PostMapping("/login")
    public Map<String, Object> login(
            @RequestBody LoginRequest req
    ) {

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

        AppUser user =
                users.findByUsername(
                        req.username().trim()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Tài khoản không tồn tại"
                        )
                );

        if (!encoder.matches(
                req.password(),
                user.getPassword()
        )) {

            throw new IllegalArgumentException(
                    "Mật khẩu không đúng"
            );
        }

        user.setActive(true);

        user.setLastActiveAt(
                LocalDateTime.now()
        );

        users.save(user);

        return Map.of(
                "token",
                tokens.create(user.getId()),

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

        AppUser user = new AppUser();

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

        // Người tự đăng ký chỉ được tạo USER
        user.setRole(Role.USER);

        // Tài khoản mới chưa đăng nhập
        user.setActive(false);

        user.setLastActiveAt(null);

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

        me.setActive(false);

        me.setLastActiveAt(null);

        users.save(me);

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
    public Map<String, String> forgotPassword(
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

        /*
         * Tạo OTP 6 số
         */
        String code =
                String.format(
                        "%06d",
                        random.nextInt(1_000_000)
                );

        /*
         * OTP có hiệu lực 5 phút
         */
        LocalDateTime expiresAt =
                LocalDateTime.now()
                        .plusMinutes(5);

        otpStore.put(
                email,
                new OtpData(
                        code,
                        expiresAt
                )
        );

        /*
         * Tạo email
         *
         * Gmail trong spring.mail.username
         * sẽ là Gmail hệ thống gửi mail.
         *
         * user.getEmail()
         * là email của người đang quên mật khẩu.
         */
        SimpleMailMessage message =
                new SimpleMailMessage();

        /*
         * Không bắt buộc setFrom.
         *
         * Spring Mail sẽ sử dụng
         * spring.mail.username làm tài khoản gửi.
         */
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

        /*
         * Gửi email
         */
        mailSender.send(message);

        return Map.of(
                "message",
                "Mã OTP đã được gửi đến email của bạn"
        );
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

        /*
         * OTP phải đúng 6 số
         */
        if (!code.matches("\\d{6}")) {

            throw new IllegalArgumentException(
                    "Mã OTP phải gồm 6 chữ số"
            );
        }

        /*
         * Kiểm tra mật khẩu mới
         */
        validatePassword(
                req.newPassword()
        );

        /*
         * Lấy OTP
         */
        OtpData otp =
                otpStore.get(email);

        if (otp == null) {

            throw new IllegalArgumentException(
                    "Bạn chưa yêu cầu mã OTP"
            );
        }

        /*
         * Kiểm tra hết hạn
         */
        if (LocalDateTime.now()
                .isAfter(otp.expiresAt())) {

            otpStore.remove(email);

            throw new IllegalArgumentException(
                    "Mã OTP đã hết hạn. Vui lòng gửi mã mới"
            );
        }

        /*
         * Kiểm tra OTP
         */
        if (!otp.code().equals(code)) {

            throw new IllegalArgumentException(
                    "Mã OTP không đúng"
            );
        }

        /*
         * Tìm user
         */
        AppUser user =
                users.findByEmail(email)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Email không tồn tại trong hệ thống"
                                )
                        );

        /*
         * Không cho dùng lại mật khẩu cũ
         */
        if (encoder.matches(
                req.newPassword(),
                user.getPassword()
        )) {

            throw new IllegalArgumentException(
                    "Mật khẩu mới phải khác mật khẩu cũ"
            );
        }

        /*
         * Đặt mật khẩu mới
         */
        user.setPassword(
                encoder.encode(
                        req.newPassword()
                )
        );

        users.save(user);

        /*
         * Xóa OTP sau khi sử dụng thành công
         */
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