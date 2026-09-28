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
    public Category create(
            @Valid @RequestBody Category in
    ) {

        String name = in.getName().trim();

        if (repo.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tên danh mục '" + name + "' đã tồn tại"
            );
        }

        in.setId(null);
        in.setName(name);

        return repo.save(in);
    }

    @PutMapping("/{id}")
    public Category update(
            @PathVariable Long id,
            @Valid @RequestBody Category in
    ) {

        Category c = repo.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy danh mục"
                        )
                );

        String name = in.getName().trim();

        if (repo.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tên danh mục '" + name + "' đã tồn tại"
            );
        }

        c.setName(name);
        c.setDescription(in.getDescription());

        return repo.save(c);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id
    ) {

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