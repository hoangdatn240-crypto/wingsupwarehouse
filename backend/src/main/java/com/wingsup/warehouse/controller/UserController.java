package com.wingsup.warehouse.controller;

import com.wingsup.warehouse.model.AppUser;
import com.wingsup.warehouse.repository.AppUserRepository;
import com.wingsup.warehouse.repository.StockTransactionRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Chỉ ADMIN (được chặn ở AuthInterceptor).
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository repo;
    private final PasswordEncoder encoder;
    private final StockTransactionRepository transactions;

    public UserController(
            AppUserRepository repo,
            PasswordEncoder encoder,
            StockTransactionRepository transactions
    ) {
        this.repo = repo;
        this.encoder = encoder;
        this.transactions = transactions;
    }

    @GetMapping
    public List<AppUser> list() {
        return repo.findAll();
    }

    @PostMapping
    public AppUser create(@Valid @RequestBody AppUser in) {

        try {
            if (in.getUsername() == null || in.getUsername().isBlank()) {
                throw new IllegalArgumentException(
                        "Tên đăng nhập không được để trống"
                );
            }

            if (in.getPassword() == null || in.getPassword().length() < 6) {
                throw new IllegalArgumentException(
                        "Mật khẩu phải từ 6 ký tự"
                );
            }

            // Kiểm tra username đã tồn tại
            if (repo.findByUsername(in.getUsername().trim()).isPresent()) {
                throw new IllegalArgumentException(
                        "Tên đăng nhập đã tồn tại: " + in.getUsername()
                );
            }

            in.setId(null);
            in.setUsername(in.getUsername().trim());
            in.setPassword(encoder.encode(in.getPassword()));

            // Nếu không chọn role thì mặc định USER
            if (in.getRole() == null) {
                in.setRole(com.wingsup.warehouse.model.Role.USER);
            }

            return repo.save(in);

        } catch (Exception e) {
            e.printStackTrace();

            throw new IllegalArgumentException(
                    "Lỗi tạo người dùng: "
                    + e.getClass().getSimpleName()
                    + " - "
                    + e.getMessage()
            );
        }
    }

    @PutMapping("/{id}")
    public AppUser update(
            @PathVariable Long id,
            @RequestBody AppUser in,
            @RequestAttribute("user") AppUser me
    ) {
        AppUser u = repo.findById(id)
                .orElseThrow(()
                        -> new IllegalArgumentException(
                        "Không tìm thấy người dùng"
                )
                );

        if (u.getId().equals(me.getId())
                && (!in.isActive() || in.getRole() != me.getRole())) {

            throw new IllegalArgumentException(
                    "Không thể khóa hoặc đổi quyền chính mình"
            );
        }

        u.setFullName(in.getFullName());
        u.setEmail(in.getEmail());
        u.setRole(in.getRole());
        u.setActive(in.isActive());

        if (in.getPassword() != null
                && !in.getPassword().isBlank()) {

            if (in.getPassword().length() < 6) {
                throw new IllegalArgumentException(
                        "Mật khẩu phải từ 6 ký tự"
                );
            }

            u.setPassword(
                    encoder.encode(in.getPassword())
            );
        }

        return repo.save(u);
    }

    @Transactional
    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id,
            @RequestAttribute("user") AppUser me
    ) {
        if (id.equals(me.getId())) {
            throw new IllegalArgumentException("Không thể xóa chính mình");
        }

        AppUser u = repo.findById(id)
                .orElseThrow(()
                        -> new IllegalArgumentException(
                        "Không tìm thấy người dùng"
                )
                );

        try {
            // Bỏ liên kết với các phiếu nhập/xuất
            transactions.clearCreatedBy(id);
            transactions.clearApprovedBy(id);

            transactions.flush();

            // Xóa user
            repo.delete(u);
            repo.flush();

        } catch (Exception e) {

            e.printStackTrace();

            throw new IllegalArgumentException(
                    "LỖI THẬT: " + e.getClass().getName()
                    + " - " + e.getMessage()
            );
        }
    }
}
