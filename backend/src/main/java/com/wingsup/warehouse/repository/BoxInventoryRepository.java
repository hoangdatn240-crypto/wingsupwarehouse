
package com.wingsup.warehouse.repository;

import com.wingsup.warehouse.model.BoxInventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoxInventoryRepository
        extends JpaRepository<BoxInventory, Long> {
}
