package com.edulearn.lesson.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lessonId;

    /** Foreign key reference to course-service (no DB-level FK across services) */
    @Column(nullable = false)
    private Long courseId;

    /** Denormalized from course — used for ownership checks without cross-service call */
    @Column
    private Long instructorId;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType contentType;

    /** URL to video, article, or PDF content */
    @Column(columnDefinition = "TEXT")
    private String contentUrl;

    @Column
    @Builder.Default
    private Integer durationMinutes = 0;

    /** 1-based display order within the course */
    @Column(nullable = false)
    @Builder.Default
    private Integer orderIndex = 1;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** If true, unenrolled users can preview this lesson */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isPreview = false;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Resource> resources = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ContentType {
        VIDEO, ARTICLE, PDF
    }
}
