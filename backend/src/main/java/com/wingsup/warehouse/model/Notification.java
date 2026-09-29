/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.wingsup.warehouse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Column;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 1000)
    private String message;

    @Column(name = "is_read")
    private boolean read = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(optional = true)
    @JoinColumn(
            name = "transaction_id",
            nullable = true,
            foreignKey = @ForeignKey(
                    name = "FKtmlckuj5alugrcu7yhd0x18p1",
                    value = ConstraintMode.NO_CONSTRAINT
            )
    )
    private StockTransaction transaction;
}
