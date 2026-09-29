package com.wingsup.warehouse.controller;

import com.wingsup.warehouse.model.Category;
import com.wingsup.warehouse.model.Product;
import com.wingsup.warehouse.repository.CategoryRepository;
import com.wingsup.warehouse.repository.ProductRepository;
import com.wingsup.warehouse.repository.StockTransactionRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository repo;
    private final CategoryRepository categories;
    private final StockTransactionRepository transactions;

    public ProductController(
            ProductRepository repo,
            CategoryRepository categories,
            StockTransactionRepository transactions
    ) {
        this.repo = repo;
        this.categories = categories;
        this.transactions = transactions;
    }

    @GetMapping
    public List<Product> list(
            @RequestParam(required = false) String q
    ) {
        if (q == null || q.isBlank()) {
            return repo.findAll();
        }

        return repo.findByNameContainingIgnoreCase(q);
    }

    @GetMapping("/low-stock")
    public List<Product> lowStock() {
        return repo.findLowStock();
    }

    @PostMapping
    public Product create(@Valid @RequestBody Product in) {

        if (in.getName() == null || in.getName().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên sản phẩm không được để trống"
            );
        }

        String name = in.getName().trim();

        if (repo.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tên sản phẩm '" + name + "' đã tồn tại"
            );
        }

        Product p = new Product();

        copy(p, in);

        p.setQuantity(
                Math.max(0, in.getQuantity())
        );

        return repo.save(p);
    }

    @PutMapping("/{id}")
    public Product update(
            @PathVariable Long id,
            @Valid @RequestBody Product in
    ) {

        Product p = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm"
                ));

        if (in.getName() == null || in.getName().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên sản phẩm không được để trống"
            );
        }

        String name = in.getName().trim();

        if (repo.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tên sản phẩm '" + name + "' đã tồn tại"
            );
        }

        copy(p, in);

        return repo.save(p);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, String> delete(
            @PathVariable Long id
    ) {

        if (!repo.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy sản phẩm"
            );
        }

        transactions.clearProduct(id);

        repo.deleteById(id);

        return Map.of(
                "message",
                "Đã xóa sản phẩm thành công"
        );
    }

    private void copy(
            Product p,
            Product in
    ) {

        p.setName(in.getName().trim());

        p.setUnit(in.getUnit());

        p.setMinQuantity(
                Math.max(0, in.getMinQuantity())
        );

        Category c =
                in.getCategory() == null
                || in.getCategory().getId() == null
                ? null
                : categories.findById(
                        in.getCategory().getId()
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Danh mục không tồn tại"
                        )
                );

        p.setCategory(c);
    }
}