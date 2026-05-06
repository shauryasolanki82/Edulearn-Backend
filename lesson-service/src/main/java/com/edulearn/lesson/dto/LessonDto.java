package com.edulearn.lesson.dto;

import com.edulearn.lesson.entity.Lesson;
import com.edulearn.lesson.entity.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class LessonDto {

    // ── Lesson Request ────────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LessonRequest {

        @NotBlank(message = "Lesson title is required")
        private String title;

        @NotNull(message = "Content type is required")
        private Lesson.ContentType contentType;

        private String contentUrl;

        @Builder.Default
        private Integer durationMinutes = 0;

        private String description;

        @Builder.Default
        private Boolean isPreview = false;

        /** If null, lesson is appended at the end */
        private Integer orderIndex;
    }

    // ── Lesson Response ───────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LessonResponse {
        private Long lessonId;
        private Long courseId;
        private String title;
        private String contentType;
        private String contentUrl;
        private Integer durationMinutes;
        private Integer orderIndex;
        private String description;
        private Boolean isPreview;
        private List<ResourceResponse> resources;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // ── Lesson Summary (no contentUrl for unenrolled users) ──────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LessonSummary {
        private Long lessonId;
        private Long courseId;
        private String title;
        private String contentType;
        private Integer durationMinutes;
        private Integer orderIndex;
        private Boolean isPreview;
        private int resourceCount;
    }

    // ── Resource Request ──────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResourceRequest {

        @NotBlank(message = "Resource name is required")
        private String name;

        @NotBlank(message = "File URL is required")
        private String fileUrl;

        @NotNull(message = "File type is required")
        private Resource.FileType fileType;

        private Long sizeKb;
    }

    // ── Resource Response ─────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResourceResponse {
        private Long resourceId;
        private Long lessonId;
        private String name;
        private String fileUrl;
        private String fileType;
        private Long sizeKb;
        private LocalDateTime createdAt;
    }

    // ── Reorder Request ───────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReorderRequest {
        /** Ordered list of lessonIds representing the new sequence */
        private List<Long> lessonIds;
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
