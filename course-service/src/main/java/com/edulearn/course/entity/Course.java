package com.edulearn.course.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long courseId;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level;

    @PositiveOrZero
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    /** References userId in auth-service — no FK across services */
    @Column(nullable = false)
    private Long instructorId;

    @Column
    private String instructorName;

    @Column
    private String thumbnailUrl;

    /** Total duration in minutes — computed from lessons */
    @Column
    @Builder.Default
    private Integer totalDuration = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Language language = Language.ENGLISH;

    @Column
    private String whatYouWillLearn;

    @Column
    private String requirements;

    @Column
    @Builder.Default
    private Integer totalEnrollments = 0;

    @Column
    @Builder.Default
    private Double averageRating = 0.0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── Enums ─────────────────────────────────────────────────────────────────

    public enum Category {
        PROGRAMMING, WEB_DEVELOPMENT, DATA_SCIENCE, MACHINE_LEARNING,
        MOBILE_DEVELOPMENT, DATABASE, DEVOPS, CLOUD, CYBERSECURITY,
        DESIGN, BUSINESS, MARKETING, PHOTOGRAPHY, MUSIC, HEALTH, OTHER
    }

    public enum Level {
        BEGINNER, INTERMEDIATE, ADVANCED, ALL_LEVELS
    }

    public enum Language {
        ENGLISH, HINDI, SPANISH, FRENCH, GERMAN, PORTUGUESE, ARABIC,
        CHINESE, JAPANESE, KOREAN, OTHER
    }
}
