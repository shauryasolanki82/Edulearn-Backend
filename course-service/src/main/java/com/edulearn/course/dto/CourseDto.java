package com.edulearn.course.dto;

import com.edulearn.course.entity.Course;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CourseDto {

    // ── Create / Update Request ───────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseRequest {

        @NotBlank(message = "Title is required")
        private String title;

        private String description;

        @NotNull(message = "Category is required")
        private Course.Category category;

        @NotNull(message = "Level is required")
        private Course.Level level;

        @PositiveOrZero(message = "Price must be zero or positive")
        @Builder.Default
        private BigDecimal price = BigDecimal.ZERO;

        private String thumbnailUrl;

        @Builder.Default
        private Course.Language language = Course.Language.ENGLISH;

        private String whatYouWillLearn;
        private String requirements;
    }

    // ── Course Response ───────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseResponse {
        private Long courseId;
        private String title;
        private String description;
        private String category;
        private String level;
        private BigDecimal price;
        private Long instructorId;
        private String instructorName;
        private String thumbnailUrl;
        private Integer totalDuration;
        private Boolean isPublished;
        private Boolean isFeatured;
        private String language;
        private String whatYouWillLearn;
        private String requirements;
        private Integer totalEnrollments;
        private Double averageRating;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // ── Course Summary (for catalog listing) ─────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseSummary {
        private Long courseId;
        private String title;
        private String category;
        private String level;
        private BigDecimal price;
        private Long instructorId;
        private String instructorName;
        private String thumbnailUrl;
        private Integer totalDuration;
        private Integer totalEnrollments;
        private Double averageRating;
        private String language;
        private Boolean isFeatured;
        private LocalDateTime createdAt;
    }

    // ── Search / Filter Request ───────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRequest {
        private String keyword;
        private Course.Category category;
        private Course.Level level;
        private Course.Language language;
        private BigDecimal maxPrice;
        private String sortBy;    // "newest", "popular", "rating", "price_asc", "price_desc"
        private int page = 0;
        private int size = 12;
    }

    // ── Paginated Response ────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PagedResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }

    // ── Publish Toggle Request ────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublishRequest {
        private boolean published;
    }

    // ── Update Enrollment Count (called by enrollment-service) ────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollmentCountUpdate {
        private Long courseId;
        private int delta; // +1 for enroll, -1 for unenroll
    }

    // ── Generic API Response ──────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;
    }
}
