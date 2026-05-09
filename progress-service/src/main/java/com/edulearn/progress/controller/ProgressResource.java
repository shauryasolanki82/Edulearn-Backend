package com.edulearn.progress.controller;

import com.edulearn.progress.dto.ProgressDto;
import com.edulearn.progress.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Progress & Certificates", description = "Lesson progress tracking and certificate management")
public class ProgressResource {

    private final ProgressService progressService;

    // ── POST /api/v1/progress — track lesson watch progress ──────────────────
    @PostMapping("/api/v1/progress")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    @Operation(summary = "Track watch time and completion for a lesson")
    public ResponseEntity<ProgressDto.ProgressResponse> trackProgress(
            @Valid @RequestBody ProgressDto.TrackRequest request,
            HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(progressService.trackProgress(studentId, request));
    }

    // ── PUT /api/v1/progress/lesson/{lessonId}/complete — mark complete ───────
    @PutMapping("/api/v1/progress/lesson/{lessonId}/complete")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    @Operation(summary = "Mark a specific lesson as completed")
    public ResponseEntity<ProgressDto.ProgressResponse> markLessonComplete(
            @PathVariable Long lessonId,
            @RequestParam Long courseId,
            HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(progressService.markLessonComplete(studentId, lessonId, courseId));
    }

    // ── GET /api/v1/progress/lesson/{lessonId} — single lesson progress ───────
    @GetMapping("/api/v1/progress/lesson/{lessonId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get progress for a specific lesson")
    public ResponseEntity<ProgressDto.ProgressResponse> getLessonProgress(
            @PathVariable Long lessonId,
            HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(progressService.getLessonProgress(studentId, lessonId));
    }

    // ── GET /api/v1/progress/course/{courseId} — course completion % ──────────
    @GetMapping("/api/v1/progress/course/{courseId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get overall course progress with completion percentage")
    public ResponseEntity<ProgressDto.CourseProgressResponse> getCourseProgress(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") long totalLessons,
            HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(progressService.getCourseProgress(studentId, courseId, totalLessons));
    }

    // ── GET /api/v1/progress/my — all progress for authenticated student ──────
    @GetMapping("/api/v1/progress/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all lesson progress records for the authenticated student")
    public ResponseEntity<List<ProgressDto.ProgressResponse>> getAllMyProgress(HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(progressService.getAllProgressByStudent(studentId));
    }

    // ── GET /api/v1/progress/student/{studentId} — instructor/admin view ──────
    @GetMapping("/api/v1/progress/student/{studentId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Get all progress records for a specific student (Instructor/Admin)")
    public ResponseEntity<List<ProgressDto.ProgressResponse>> getProgressByStudent(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(progressService.getAllProgressByStudent(studentId));
    }

    // ── POST /api/v1/certificates — issue a certificate ──────────────────────
    @PostMapping("/api/v1/certificates")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    @Operation(summary = "Issue a completion certificate for a course (requires 100% progress)")
    public ResponseEntity<ProgressDto.CertificateResponse> issueCertificate(
            @Valid @RequestBody ProgressDto.IssueCertificateRequest request,
            HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(progressService.issueCertificate(studentId, request));
    }

    // ── GET /api/v1/certificates/my — student's certificates ─────────────────
    @GetMapping("/api/v1/certificates/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all certificates earned by the authenticated student")
    public ResponseEntity<List<ProgressDto.CertificateResponse>> getMyCertificates(
            HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(progressService.getCertificatesByStudent(studentId));
    }

    // ── GET /api/v1/certificates/course/{courseId} — specific certificate ─────
    @GetMapping("/api/v1/certificates/course/{courseId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the certificate for a specific course")
    public ResponseEntity<ProgressDto.CertificateResponse> getCertificate(
            @PathVariable Long courseId,
            HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(progressService.getCertificate(studentId, courseId));
    }

    // ── GET /api/v1/certificates/verify/{code} — public verification ──────────
    @GetMapping("/api/v1/certificates/verify/{verificationCode}")
    @Operation(summary = "Publicly verify a certificate by its verification code (no auth required)")
    public ResponseEntity<ProgressDto.VerifyResponse> verifyCertificate(
            @PathVariable String verificationCode) {
        return ResponseEntity.ok(progressService.verifyCertificate(verificationCode));
    }

    // ── GET /api/v1/certificates/student/{studentId} — admin view ────────────
    @GetMapping("/api/v1/certificates/student/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: get all certificates for a specific student")
    public ResponseEntity<List<ProgressDto.CertificateResponse>> getCertificatesByStudent(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(progressService.getCertificatesByStudent(studentId));
    }
}
