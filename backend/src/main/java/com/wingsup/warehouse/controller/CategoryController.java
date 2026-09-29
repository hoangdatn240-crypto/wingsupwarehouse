package com.wingsup.warehouse.controller;

import com.wingsup.warehouse.model.Category;
import com.wingsup.warehouse.repository.CategoryRepository;
import com.wingsup.warehouse.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository repo;
    private final ProductRepository products;

    public CategoryController(
            CategoryRepository repo,
            ProductRepository products
    ) {
        this.repo = repo;
        this.products = products;
    }

    @GetMapping
    public List<Category> list() {
        return repo.findAll();
    }

    @PostMapping
    public Category create(@RequestBody Category category) {

        if (category.getName() == null
                || category.getName().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên danh mục không được để trống"
            );
        }

        String name = category.getName().trim();

        if (repo.existsByNameIgnoreCase(name)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tên danh mục '" + name + "' đã tồn tại"
            );
        }

        category.setName(name);

        if (category.getDescription() != null) {
            category.setDescription(
                    category.getDescription().trim()
            );
        }

        return repo.save(category);
    }

    @PutMapping("/{id}")
    public Category update(
            @PathVariable Long id,
            @Valid @RequestBody Category in
    ) {

        Category c = repo.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy danh mục"
                )
                );

        if (in.getName() == null
                || in.getName().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên danh mục không được để trống"
            );
        }

        String name = in.getName().trim();

        // Kiểm tra trùng tên nhưng bỏ qua chính danh mục đang sửa
        if (repo.existsByNameIgnoreCaseAndIdNot(name, id)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tên danh mục '" + name + "' đã tồn tại"
            );
        }

        c.setName(name);

        if (in.getDescription() != null) {
            c.setDescription(
                    in.getDescription().trim()
            );
        } else {
            c.setDescription("");
        }

        return repo.save(c);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {

        if (!repo.existsById(id)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy danh mục"
            );
        }

        if (products.existsByCategoryId(id)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể xóa danh mục vì đang có sản phẩm sử dụng danh mục này"
            );
        }

        repo.deleteById(id);
    }
}
