package com.edulearn.progress.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id","course_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Certificate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long certificateId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private String courseName;

    @Column(nullable = false)
    private String studentName;

    @Column
    private String instructorName;

    /** Publicly verifiable UUID — embedded in the PDF */
    @Column(nullable = false, unique = true)
    @Builder.Default
    private String verificationCode = UUID.randomUUID().toString();

    /** URL to the generated PDF (S3 or local) */
    @Column
    private String certificateUrl;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime issuedAt;
}
