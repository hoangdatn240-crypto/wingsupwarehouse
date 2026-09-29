package com.wingsup.warehouse.controller;

import com.wingsup.warehouse.model.AppUser;
import com.wingsup.warehouse.model.Role;
import com.wingsup.warehouse.repository.AppUserRepository;
import com.wingsup.warehouse.security.AuthInterceptor;
import com.wingsup.warehouse.security.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserRepository users;
    private final TokenStore tokens;
    private final PasswordEncoder encoder;

    public AuthController(
            AppUserRepository users,
            TokenStore tokens,
            PasswordEncoder encoder
    ) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
    }

    public record LoginRequest(
            String username,
            String password) {

    }

    public record RegisterRequest(
            String username,
            String password,
            String fullName,
            String email) {

    }

    public record PasswordRequest(
            String oldPassword,
            String newPassword) {

    }


@PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req) {

        if (req.username() == null || req.username().isBlank()) {
            throw new IllegalArgumentException(
                    "Tên đăng nhập không được để trống"
            );
        }

        if (req.password() == null || req.password().isBlank()) {
            throw new IllegalArgumentException(
                    "Mật khẩu không được để trống"
            );
        }

        AppUser user = users.findByUsername(req.username().trim())
                .orElseThrow(() -> new IllegalArgumentException(
                "Tài khoản không tồn tại"
        ));

        if (!encoder.matches(req.password(), user.getPassword())) {
            throw new IllegalArgumentException(
                    "Mật khẩu không đúng"
            );
        }

        user.setActive(true);
        users.save(user);

        return Map.of(
                "token", tokens.create(user.getId()),
                "user", user
        );
    }


    @PostMapping(

    "/register")
    public Map<String, String> register(
            @RequestBody RegisterRequest req
    ) {

        if (req.username() == null || req.username().isBlank()) {
            throw new IllegalArgumentException(
                    "Tên đăng nhập không được để trống"
            );
        }

        if (!req.username().matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException(
                    "Tên đăng nhập không được có dấu hoặc ký tự đặc biệt"
            );
        }

        if (req.password() == null || req.password().isBlank()) {
            throw new IllegalArgumentException(
                    "Mật khẩu không được để trống"
            );
        }

        if (req.password().length() < 6) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải từ 6 ký tự"
            );
        }

        if (!req.password().matches(".*[A-Z].*")) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải có ít nhất 1 chữ hoa"
            );
        }

        if (!req.password().matches(".*[0-9].*")) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải có ít nhất 1 chữ số"
            );
        }

        if (!req.password().matches(".*[^a-zA-Z0-9].*")) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải có ít nhất 1 ký tự đặc biệt"
            );
        }

        if (req.password().length() < 6) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải từ 6 ký tự"
            );
        }

        if (req.fullName() == null || req.fullName().isBlank()) {
            throw new IllegalArgumentException(
                    "Họ tên không được để trống"
            );
        }

        if (req.email() == null || req.email().isBlank()) {
            throw new IllegalArgumentException(
                    "Email không được để trống"
            );
        }

        if (!req.email().contains("@")) {
            throw new IllegalArgumentException(
                    "Email không hợp lệ"
            );
        }

        if (users.findByUsername(req.username().trim()).isPresent()) {
            throw new IllegalArgumentException(
                    "Tên đăng nhập đã tồn tại"
            );
        }

        AppUser user = new AppUser();

        user.setUsername(req.username().trim());
        user.setPassword(encoder.encode(req.password()));
        user.setFullName(req.fullName().trim());
        user.setEmail(req.email().trim());

        // Người tự đăng ký chỉ được tạo USER
        user.setRole(Role.USER);

        // Tài khoản mới chưa đăng nhập
        user.setActive(false);

        users.save(user);

        return Map.of(
                "message",
                "Tạo tài khoản thành công"
        );
    }

    @PostMapping("/logout")
    public Map<String, String> logout(
            HttpServletRequest req,
            @RequestAttribute("user") AppUser me
    ) {

        me.setActive(false);
        users.save(me);

        tokens.remove(
                AuthInterceptor.extractToken(req)
        );

        return Map.of(
                "message",
                "Đã đăng xuất"
        );
    }

    @GetMapping("/me")
    public AppUser me(
            @RequestAttribute("user") AppUser me
    ) {
        return me;
    }

    @PutMapping("/password")
    public Map<String, String> changePassword(
            @RequestAttribute("user") AppUser me,
            @RequestBody PasswordRequest req
    ) {

        if (!encoder.matches(
                req.oldPassword(),
                me.getPassword()
        )) {
            throw new IllegalArgumentException(
                    "Mật khẩu cũ không đúng"
            );
        }

        if (req.newPassword() == null
                || req.newPassword().length() < 6) {

            throw new IllegalArgumentException(
                    "Mật khẩu mới phải từ 6 ký tự"
            );
        }

        me.setPassword(
                encoder.encode(req.newPassword())
        );

        users.save(me);

        return Map.of(
                "message",
                "Đã đổi mật khẩu"
        );
    }
}
