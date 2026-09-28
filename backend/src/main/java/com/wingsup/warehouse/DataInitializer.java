package com.wingsup.warehouse;

import com.wingsup.warehouse.model.*;
import com.wingsup.warehouse.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Tạo dữ liệu mẫu lần chạy đầu tiên. */
@Component
public class DataInitializer implements CommandLineRunner {
    private final AppUserRepository users;
    private final CategoryRepository categories;
    private final SupplierRepository suppliers;
    private final ProductRepository products;
    private final PasswordEncoder encoder;

    public DataInitializer(AppUserRepository users, CategoryRepository categories, SupplierRepository suppliers,
                           ProductRepository products, PasswordEncoder encoder) {
        this.users = users;
        this.categories = categories;
        this.suppliers = suppliers;
        this.products = products;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;

        user("admin", "admin123", "Quản trị viên Wings Up", Role.ADMIN);
        user("staff", "staff123", "Nhân viên kho", Role.USER);

        Category books = new Category();
        books.setName("Sách & giáo trình");
        books.setDescription("Giáo trình, sách bài tập");
        categories.save(books);

        Category stationery = new Category();
        stationery.setName("Văn phòng phẩm");
        stationery.setDescription("Bút, vở, dụng cụ học tập");
        categories.save(stationery);

        Supplier s = new Supplier();
        s.setName("Công ty Sách Giáo Dục");
        s.setPhone("0900000000");
        s.setEmail("contact@example.com");
        s.setAddress("Hải Phòng");
        suppliers.save(s);

        product("BK-001", "Giáo trình Tiếng Anh A1", "cuốn", "85000", 120, 30, books, s);
        product("BK-002", "Giáo trình Tiếng Anh B1", "cuốn", "95000", 20, 30, books, s);
        product("ST-001", "Bút bi xanh", "cây", "5000", 500, 100, stationery, s);
    }

    private void user(String username, String password, String fullName, Role role) {
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPassword(encoder.encode(password));
        u.setFullName(fullName);
        u.setRole(role);
        users.save(u);
    }

    private void product(String sku, String name, String unit, String price, int qty, int min, Category c, Supplier s) {
        Product p = new Product();
        p.setName(name);
        p.setUnit(unit);
        p.setPrice(new BigDecimal(price));
        p.setQuantity(qty);
        p.setMinQuantity(min);
        p.setCategory(c);
        p.setSupplier(s);
        products.save(p);
    }
}