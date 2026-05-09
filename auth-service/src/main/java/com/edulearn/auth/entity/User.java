package com.edulearn.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @NotBlank
    @Column(nullable = false)
    private String fullName;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** OAuth2 provider name: "local", "github", "google" */
    @Column(nullable = false)
    @Builder.Default
    private String provider = "local";

    /** Provider-specific user ID (for OAuth2 users) */
    @Column
    private String providerId;

    @Column(length = 20)
    private String mobile;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column
    private String profilePicUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Role {
        STUDENT, INSTRUCTOR, ADMIN
    }
}
