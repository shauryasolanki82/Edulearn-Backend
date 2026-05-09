package com.edulearn.discussion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

public class DiscussionDto {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ThreadRequest {
        @NotNull  private Long courseId;
        private   Long lessonId;
        @NotBlank private String title;
        @NotBlank private String body;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ThreadResponse {
        private Long threadId;
        private Long courseId;
        private Long lessonId;
        private Long authorId;
        private String authorName;
        private String title;
        private String body;
        private Boolean isPinned;
        private Boolean isClosed;
        private int replyCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReplyRequest {
        @NotBlank private String body;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReplyResponse {
        private Long replyId;
        private Long threadId;
        private Long authorId;
        private String authorName;
        private String body;
        private Boolean isAccepted;
        private Integer upvotes;
        private LocalDateTime createdAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ThreadWithReplies {
        private ThreadResponse thread;
        private List<ReplyResponse> replies;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;
    }
}
