package com.wingsup.warehouse.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "products")
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(
        name = "name",
        nullable = false,
        length = 255,
        columnDefinition = "NVARCHAR(255)"
    )
    private String name;

    @Column(
        name = "unit",
        length = 100,
        columnDefinition = "NVARCHAR(100)"
    )
    private String unit;

    private int quantity;

    @Column(name = "min_quantity")
    private int minQuantity;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;
}