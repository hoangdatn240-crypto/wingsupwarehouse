
package com.wingsup.warehouse.controller;

import com.wingsup.warehouse.model.AppUser;
import com.wingsup.warehouse.model.BoxInventory;
import com.wingsup.warehouse.model.Role;
import com.wingsup.warehouse.repository.BoxInventoryRepository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/boxes")
public class BoxInventoryController {

    private final BoxInventoryRepository repo;

    public BoxInventoryController(BoxInventoryRepository repo) {
        this.repo = repo;
    }

    public record BoxRequest(
            Integer totalBoxes,
            Integer inUseBoxes
    ) {}

    private BoxInventory getOrCreate() {
        return repo.findById(1L).orElseGet(() -> {
            BoxInventory inventory = new BoxInventory();
            inventory.setId(1L);
            inventory.setTotalBoxes(0);
            inventory.setInUseBoxes(0);
            return repo.save(inventory);
        });
    }

    // Lấy số liệu hộp
    @GetMapping
    @Transactional
    public BoxInventory get(
            @RequestAttribute("user") AppUser me) {
        return getOrCreate();
    }

    // Cập nhật số lượng hộp
    // Chỉ ADMIN và MANAGER được phép sửa
    @PutMapping
    @Transactional
    public BoxInventory update(
            @RequestBody BoxRequest req,
            @RequestAttribute("user") AppUser me) {

        if (me.getRole() != Role.ADMIN
                && me.getRole() != Role.MANAGER) {
            throw new IllegalArgumentException(
                    "Chỉ ADMIN hoặc MANAGER được quản lý số lượng hộp.");
        }

        if (req.totalBoxes() == null
                || req.inUseBoxes() == null
                || req.totalBoxes() < 0
                || req.inUseBoxes() < 0) {
            throw new IllegalArgumentException(
                    "Số lượng hộp không được để trống hoặc âm.");
        }

        if (req.inUseBoxes() > req.totalBoxes()) {
            throw new IllegalArgumentException(
                    "Số hộp đang sử dụng không thể lớn hơn tổng số hộp.");
        }

        BoxInventory inventory = getOrCreate();
        inventory.setTotalBoxes(req.totalBoxes());
        inventory.setInUseBoxes(req.inUseBoxes());

        return repo.save(inventory);
    }
}
