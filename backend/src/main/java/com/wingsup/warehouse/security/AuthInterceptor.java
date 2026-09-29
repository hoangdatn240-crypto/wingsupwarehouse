package com.wingsup.warehouse.security;

import com.wingsup.warehouse.model.AppUser;
import com.wingsup.warehouse.model.Role;
import com.wingsup.warehouse.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenStore tokens;
    private final AppUserRepository users;

    public AuthInterceptor(
            TokenStore tokens,
            AppUserRepository users
    ) {
        this.tokens = tokens;
        this.users = users;
    }

    public static String extractToken(HttpServletRequest req) {
        String h = req.getHeader("Authorization");

        return (h != null && h.startsWith("Bearer "))
                ? h.substring(7)
                : null;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest req,
            HttpServletResponse res,
            Object handler
    ) throws IOException {

        // Cho phép OPTIONS
        if (HttpMethod.OPTIONS.matches(req.getMethod())) {
            return true;
        }

        // Lấy user từ token
        Long userId = tokens.get(extractToken(req));

        AppUser user = userId == null
                ? null
                : users.findById(userId).orElse(null);

        // Chưa đăng nhập hoặc tài khoản không hoạt động
        if (user == null || !user.isActive()) {
            error(
                    res,
                    401,
                    "Chưa đăng nhập hoặc phiên đã hết hạn"
            );
            return false;
        }

        // Gửi user cho Controller
        req.setAttribute("user", user);

        /*
         * =========================
         * ADMIN
         * =========================
         *
         * ADMIN được toàn quyền.
         */
        if (user.getRole() == Role.ADMIN) {
            return true;
        }

        /*
         * =========================
         * MANAGER
         * =========================
         *
         * MANAGER được sử dụng toàn bộ
         * chức năng quản lý kho.
         *
         * Ngoại lệ duy nhất:
         * Không được truy cập /api/users
         */
        if (user.getRole() == Role.MANAGER) {

            if (pathIsUsers(req.getRequestURI())) {

                error(
                        res,
                        403,
                        "MANAGER không có quyền quản lý người dùng"
                );

                return false;
            }

            // Các API khác MANAGER được phép
            return true;
        }

        /*
         * =========================
         * USER
         * =========================
         *
         * USER bị giới hạn quyền.
         */
        if (user.getRole() == Role.USER) {

            if (isUserRestricted(
                    req.getRequestURI(),
                    req.getMethod()
            )) {

                error(
                        res,
                        403,
                        "Bạn không có quyền thực hiện thao tác này"
                );

                return false;
            }

            return true;
        }

        // Role không hợp lệ
        error(
                res,
                403,
                "Vai trò người dùng không hợp lệ"
        );

        return false;
    }

    /**
     * Kiểm tra API Người dùng.
     *
     * ADMIN mới được truy cập.
     */
    private boolean pathIsUsers(String path) {
        return path.startsWith("/api/users");
    }

    /**
     * Quyền hạn của USER.
     */
    private boolean isUserRestricted(
            String path,
            String method
    ) {

        boolean read = HttpMethod.GET.matches(method);

        /*
         * USER không được quản lý người dùng.
         */
        if (pathIsUsers(path)) {
            return true;
        }

        /*
         * USER không được duyệt / từ chối phiếu.
         */
        if (path.matches(
                "/api/transactions/\\d+/(approve|reject)"
        )) {
            return true;
        }

        /*
    * USER chỉ được xem:
    *
    * - Sản phẩm
    * - Danh mục
    *
    * Không được thêm / sửa / xóa.
         */
        if (path.startsWith("/api/products")
                || path.startsWith("/api/categories")) {

            return !read;
        }
        /*
         * Các API khác USER được phép
         * theo Controller hiện tại.
         */
        return false;
    }

    /**
     * Trả lỗi JSON.
     */
    private void error(
            HttpServletResponse res,
            int status,
            String message
    ) throws IOException {

        res.setStatus(status);

        res.setContentType(
                "application/json;charset=UTF-8"
        );

        res.getWriter().write(
                "{\"message\":\"" + message + "\"}"
        );
    }
}
