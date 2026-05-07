package com.edulearn.assessment.controller;

import com.edulearn.assessment.dto.AssessmentDto;
import com.edulearn.assessment.service.AssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Assessment", description = "Quiz management, question CRUD, and attempt submission")
public class AssessmentResource {

    private final AssessmentService assessmentService;

    // ── Quiz endpoints ────────────────────────────────────────────────────────

    @PostMapping("/quizzes")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Create a new quiz for a course")
    public ResponseEntity<AssessmentDto.QuizResponse> createQuiz(
            @Valid @RequestBody AssessmentDto.QuizRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentService.createQuiz(request));
    }

    @GetMapping("/quizzes/{quizId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get quiz details by ID")
    public ResponseEntity<AssessmentDto.QuizResponse> getQuiz(@PathVariable Long quizId) {
        return ResponseEntity.ok(assessmentService.getQuizById(quizId));
    }

    @GetMapping("/quizzes/course/{courseId}")
    @Operation(summary = "Get all quizzes for a course (public)")
    public ResponseEntity<List<AssessmentDto.QuizResponse>> getQuizzesByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(assessmentService.getQuizzesByCourse(courseId));
    }

    @PutMapping("/quizzes/{quizId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Update quiz metadata")
    public ResponseEntity<AssessmentDto.QuizResponse> updateQuiz(
            @PathVariable Long quizId, @Valid @RequestBody AssessmentDto.QuizRequest request) {
        return ResponseEntity.ok(assessmentService.updateQuiz(quizId, request));
    }

    @PutMapping("/quizzes/{quizId}/publish")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Publish or unpublish a quiz")
    public ResponseEntity<AssessmentDto.QuizResponse> publishQuiz(
            @PathVariable Long quizId, @RequestParam boolean publish) {
        return ResponseEntity.ok(assessmentService.publishQuiz(quizId, publish));
    }

    @DeleteMapping("/quizzes/{quizId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Delete a quiz")
    public ResponseEntity<AssessmentDto.ApiResponse> deleteQuiz(@PathVariable Long quizId) {
        assessmentService.deleteQuiz(quizId);
        return ResponseEntity.ok(AssessmentDto.ApiResponse.builder().success(true).message("Quiz deleted").build());
    }

    // ── Question endpoints ────────────────────────────────────────────────────

    @PostMapping("/quizzes/{quizId}/questions")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Add a question to a quiz")
    public ResponseEntity<AssessmentDto.QuestionResponse> addQuestion(
            @PathVariable Long quizId, @Valid @RequestBody AssessmentDto.QuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentService.addQuestion(quizId, request));
    }

    @GetMapping("/quizzes/{quizId}/questions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get questions for a quiz — students see no correct answers")
    public ResponseEntity<?> getQuestions(@PathVariable Long quizId, HttpServletRequest req) {
        String role = (String) req.getAttribute("role");
        if ("INSTRUCTOR".equals(role) || "ADMIN".equals(role)) {
            return ResponseEntity.ok(assessmentService.getQuestionsForInstructor(quizId));
        }
        return ResponseEntity.ok(assessmentService.getQuestionsForStudent(quizId));
    }

    @PutMapping("/questions/{questionId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Update a question")
    public ResponseEntity<AssessmentDto.QuestionResponse> updateQuestion(
            @PathVariable Long questionId, @Valid @RequestBody AssessmentDto.QuestionRequest request) {
        return ResponseEntity.ok(assessmentService.updateQuestion(questionId, request));
    }

    @DeleteMapping("/questions/{questionId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Delete a question")
    public ResponseEntity<AssessmentDto.ApiResponse> deleteQuestion(@PathVariable Long questionId) {
        assessmentService.deleteQuestion(questionId);
        return ResponseEntity.ok(AssessmentDto.ApiResponse.builder().success(true).message("Question deleted").build());
    }

    // ── Attempt endpoints ─────────────────────────────────────────────────────

    @PostMapping("/quizzes/{quizId}/start")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    @Operation(summary = "Start a quiz attempt — returns attempt record with timer info")
    public ResponseEntity<AssessmentDto.AttemptResponse> startAttempt(
            @PathVariable Long quizId, HttpServletRequest req) {
        Long studentId = (Long) req.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentService.startAttempt(studentId, quizId));
    }

    @PostMapping("/attempts/submit")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    @Operation(summary = "Submit a quiz attempt — auto-grades and returns score")
    public ResponseEntity<AssessmentDto.AttemptResponse> submitAttempt(
            @Valid @RequestBody AssessmentDto.SubmitAttemptRequest request, HttpServletRequest req) {
        Long studentId = (Long) req.getAttribute("userId");
        return ResponseEntity.ok(assessmentService.submitAttempt(studentId, request));
    }

    @GetMapping("/attempts/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all quiz attempts for the authenticated student")
    public ResponseEntity<List<AssessmentDto.AttemptResponse>> getMyAttempts(HttpServletRequest req) {
        Long studentId = (Long) req.getAttribute("userId");
        return ResponseEntity.ok(assessmentService.getAttemptsByStudent(studentId));
    }

    @GetMapping("/quizzes/{quizId}/attempts")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Get all attempts for a quiz (Instructor/Admin)")
    public ResponseEntity<List<AssessmentDto.AttemptResponse>> getAttemptsByQuiz(@PathVariable Long quizId) {
        return ResponseEntity.ok(assessmentService.getAttemptsByQuiz(quizId));
    }

    @GetMapping("/quizzes/{quizId}/best-score")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the student's best score for a quiz")
    public ResponseEntity<AssessmentDto.AttemptResponse> getBestScore(
            @PathVariable Long quizId, HttpServletRequest req) {
        Long studentId = (Long) req.getAttribute("userId");
        return ResponseEntity.ok(assessmentService.getBestScore(studentId, quizId));
    }
}
