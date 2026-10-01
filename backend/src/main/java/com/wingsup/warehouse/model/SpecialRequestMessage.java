package com.wingsup.warehouse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "special_request_messages")
@Getter
@Setter
public class SpecialRequestMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "request_id")
    private SpecialRequest request;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id")
    private AppUser sender;

    @Column(nullable = false, length = 2000)
    private String message;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_read")
    private boolean read = false;
}