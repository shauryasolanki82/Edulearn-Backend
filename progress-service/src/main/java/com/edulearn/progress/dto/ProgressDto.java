package com.edulearn.progress.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class ProgressDto {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TrackRequest {
        @NotNull private Long lessonId;
        @NotNull private Long courseId;
        @Min(0) private Integer watchedSeconds;
        private Boolean markComplete;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProgressResponse {
        private Long progressId;
        private Long studentId;
        private Long courseId;
        private Long lessonId;
        private Integer watchedSeconds;
        private Boolean isCompleted;
        private LocalDateTime completedAt;
        private LocalDateTime lastAccessedAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CourseProgressResponse {
        private Long studentId;
        private Long courseId;
        private double completionPercent;
        private long completedLessons;
        private long totalLessons;
        private List<ProgressResponse> lessonProgresses;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CertificateResponse {
        private Long certificateId;
        private Long studentId;
        private Long courseId;
        private String courseName;
        private String studentName;
        private String instructorName;
        private String verificationCode;
        private String certificateUrl;
        private LocalDateTime issuedAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class VerifyResponse {
        private boolean valid;
        private String courseName;
        private String studentName;
        private String instructorName;
        private LocalDateTime issuedAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class IssueCertificateRequest {
        @NotNull private Long courseId;
        @NotNull private String courseName;
        @NotNull private String studentName;
        private String instructorName;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;
    }
}
