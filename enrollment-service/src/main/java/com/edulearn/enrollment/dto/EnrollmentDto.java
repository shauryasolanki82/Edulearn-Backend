package com.edulearn.enrollment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class EnrollmentDto {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EnrollRequest {
        @NotNull(message = "courseId is required")
        private Long courseId;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EnrollmentResponse {
        private Long enrollmentId;
        private Long studentId;
        private Long courseId;
        private String status;
        private Double progressPercent;
        private Boolean certificateIssued;
        private LocalDateTime enrolledAt;
        private LocalDateTime completedAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProgressUpdateRequest {
        @NotNull private Double progressPercent;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class IsEnrolledResponse {
        private Long studentId;
        private Long courseId;
        private boolean enrolled;
        private String status;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EnrollmentStatsResponse {
        private Long courseId;
        private long totalEnrollments;
        private long activeEnrollments;
        private long completedEnrollments;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PagedResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
    }
}
