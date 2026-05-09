package com.edulearn.lesson.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileType fileType;

    /** Size in kilobytes */
    @Column
    private Long sizeKb;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum FileType {
        PDF, SLIDES, CODE, IMAGE, ZIP, OTHER
    }
}
