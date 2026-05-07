package com.edulearn.assessment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "attempts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attempt {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attemptId;

    @Column(nullable = false) private Long quizId;
    @Column(nullable = false) private Long studentId;

    @Column(nullable = false) @Builder.Default private Double score = 0.0;
    @Column(nullable = false) @Builder.Default private Boolean passed = false;

    // Student's answers: questionId -> answer
    @ElementCollection
    @CollectionTable(name = "attempt_answers", joinColumns = @JoinColumn(name = "attempt_id"))
    @MapKeyColumn(name = "question_id")
    @Column(name = "answer")
    @Builder.Default
    private Map<Long, String> answers = new HashMap<>();

    @CreationTimestamp @Column(updatable = false) private LocalDateTime startedAt;
    @Column private LocalDateTime submittedAt;

    public enum AttemptStatus { IN_PROGRESS, SUBMITTED }
}
