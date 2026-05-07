package com.edulearn.assessment.dto;

import com.edulearn.assessment.entity.Question;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AssessmentDto {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class QuizRequest {
        @NotBlank private String title;
        @NotNull  private Long courseId;
        private Long lessonId;
        private String description;
        @Builder.Default private Integer timeLimitMinutes = 30;
        @Builder.Default private Double passingScore = 70.0;
        @Builder.Default private Integer maxAttempts = 3;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class QuizResponse {
        private Long quizId;
        private Long courseId;
        private Long lessonId;
        private String title;
        private String description;
        private Integer timeLimitMinutes;
        private Double passingScore;
        private Integer maxAttempts;
        private Boolean isPublished;
        private int questionCount;
        private LocalDateTime createdAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class QuestionRequest {
        @NotBlank private String text;
        @NotNull  private Question.QuestionType type;
        private List<String> options;
        @NotBlank private String correctAnswer;
        @Builder.Default private Integer marks = 1;
        private Integer orderIndex;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class QuestionResponse {
        private Long questionId;
        private Long quizId;
        private String text;
        private String type;
        private List<String> options;
        private String correctAnswer; // hidden for students
        private Integer marks;
        private Integer orderIndex;
    }

    // For students — no correct answer exposed
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class QuestionForStudent {
        private Long questionId;
        private String text;
        private String type;
        private List<String> options;
        private Integer marks;
        private Integer orderIndex;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SubmitAttemptRequest {
        @NotNull private Long quizId;
        private Map<Long, String> answers; // questionId -> answer
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AttemptResponse {
        private Long attemptId;
        private Long quizId;
        private Long studentId;
        private Double score;
        private Boolean passed;
        private Double passingScore;
        private Map<Long, String> answers;
        private LocalDateTime startedAt;
        private LocalDateTime submittedAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;
    }
}
