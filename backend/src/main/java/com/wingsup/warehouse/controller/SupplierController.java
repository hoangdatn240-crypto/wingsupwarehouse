package com.wingsup.warehouse.controller;

import com.wingsup.warehouse.model.Supplier;
import com.wingsup.warehouse.repository.ProductRepository;
import com.wingsup.warehouse.repository.SupplierRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.wingsup.warehouse.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierRepository repo;
    private final ProductRepository products;

    public SupplierController(
            SupplierRepository repo,
            ProductRepository products
    ) {
        this.repo = repo;
        this.products = products;
    }

    @GetMapping
    public List<Supplier> list() {
        return repo.findAll();
    }

    @PostMapping
    public Supplier create(
            @Valid @RequestBody Supplier in
    ) {

        String name = in.getName().trim();

        if (repo.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Nhà cung cấp '" + name + "' đã tồn tại"
            );
        }

        in.setId(null);
        in.setName(name);

        return repo.save(in);
    }

    @PutMapping("/{id}")
    public Supplier update(
            @PathVariable Long id,
            @Valid @RequestBody Supplier in
    ) {

        Supplier s = repo.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy nhà cung cấp"
                )
                );

        String name = in.getName().trim();

        if (repo.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Nhà cung cấp '" + name + "' đã tồn tại"
            );
        }

        s.setName(name);
        s.setPhone(in.getPhone());
        s.setEmail(in.getEmail());
        s.setAddress(in.getAddress());

        return repo.save(s);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id) {

        if (!repo.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy nhà cung cấp"
            );
        }

        // Bỏ liên kết nhà cung cấp khỏi các sản phẩm
        products.clearSupplier(id);

        // Sau đó mới xóa nhà cung cấp
        repo.deleteById(id);
    }
}
