package com.wingsup.warehouse.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Column;
import java.time.LocalDateTime;
@Entity
@Table(name = "users")
@Getter
@Setter
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank
    @Column(nullable = false)
    private String password;

    @NotBlank
    @Column(name = "full_name", length = 255, nullable = false)
    private String fullName;

    @NotBlank
    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    private boolean active = false;
}
