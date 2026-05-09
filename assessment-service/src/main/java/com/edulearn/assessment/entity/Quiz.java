package com.edulearn.assessment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Quiz {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quizId;

    @Column(nullable = false)
    private Long courseId;

    @Column
    private Long lessonId; // optional — quiz can be tied to a specific lesson

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Integer timeLimitMinutes = 30;

    @Column(nullable = false)
    @Builder.Default
    private Double passingScore = 70.0; // percentage

    @Column(nullable = false)
    @Builder.Default
    private Integer maxAttempts = 3;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    @CreationTimestamp @Column(updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
}
