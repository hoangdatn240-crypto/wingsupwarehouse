
package com.wingsup.warehouse.model;

import jakarta.persistence.*;

@Entity
@Table(name = "box_inventory")
public class BoxInventory {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private int totalBoxes = 0;

    @Column(nullable = false)
    private int inUseBoxes = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getTotalBoxes() {
        return totalBoxes;
    }

    public void setTotalBoxes(int totalBoxes) {
        this.totalBoxes = totalBoxes;
    }

    public int getInUseBoxes() {
        return inUseBoxes;
    }

    public void setInUseBoxes(int inUseBoxes) {
        this.inUseBoxes = inUseBoxes;
    }

    public int getUnusedBoxes() {
        return totalBoxes - inUseBoxes;
    }
}
