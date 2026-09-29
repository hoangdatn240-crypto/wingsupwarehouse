package com.wingsup.warehouse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lectures")
@Getter
@Setter
public class Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
        name = "name",
        nullable = false,
        length = 255,
        columnDefinition = "NVARCHAR(255)"
    )
    private String name;

    @Column(
        name = "link",
        nullable = false,
        length = 1000,
        columnDefinition = "NVARCHAR(1000)"
    )
    private String link;
}