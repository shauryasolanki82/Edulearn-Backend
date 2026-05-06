package com.edulearn.enrollment.controller;

import com.edulearn.enrollment.dto.EnrollmentDto;
import com.edulearn.enrollment.service.EnrollmentService;
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
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Course enrollment lifecycle management")
public class EnrollmentResource {

    private final EnrollmentService enrollmentService;

    // POST /api/v1/enrollments — enroll self
    @PostMapping
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    @Operation(summary = "Enroll the authenticated student in a course")
    public ResponseEntity<EnrollmentDto.EnrollmentResponse> enroll(
            @Valid @RequestBody EnrollmentDto.EnrollRequest request,
            HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.enroll(studentId, request.getCourseId()));
    }

    // DELETE /api/v1/enrollments/{courseId} — unenroll self
    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    @Operation(summary = "Unenroll the authenticated student from a course")
    public ResponseEntity<EnrollmentDto.ApiResponse> unenroll(
            @PathVariable Long courseId, HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        enrollmentService.unenroll(studentId, courseId);
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.builder()
                .success(true).message("Unenrolled successfully").build());
    }

    // GET /api/v1/enrollments/my — student's own enrollments
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all enrollments for the authenticated student")
    public ResponseEntity<List<EnrollmentDto.EnrollmentResponse>> getMyEnrollments(HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByStudent(studentId));
    }

    // GET /api/v1/enrollments/student/{studentId} — admin view
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
    @Operation(summary = "Get all enrollments for a specific student (Admin/Instructor)")
    public ResponseEntity<List<EnrollmentDto.EnrollmentResponse>> getByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByStudent(studentId));
    }

    // GET /api/v1/enrollments/course/{courseId} — instructor/admin view
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Get all enrollments for a course (Instructor/Admin)")
    public ResponseEntity<List<EnrollmentDto.EnrollmentResponse>> getByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByCourse(courseId));
    }

    // GET /api/v1/enrollments/course/{courseId}/count — public (for catalog display)
    @GetMapping("/course/{courseId}/count")
    @Operation(summary = "Get total enrollment count for a course (public)")
    public ResponseEntity<Long> getEnrollmentCount(@PathVariable Long courseId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentCount(courseId));
    }

    // GET /api/v1/enrollments/course/{courseId}/stats — analytics
    @GetMapping("/course/{courseId}/stats")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Get enrollment statistics for a course")
    public ResponseEntity<EnrollmentDto.EnrollmentStatsResponse> getStats(@PathVariable Long courseId) {
        return ResponseEntity.ok(enrollmentService.getStats(courseId));
    }

    // GET /api/v1/enrollments/check?studentId=&courseId= — inter-service gate check
    @GetMapping("/check")
    @Operation(summary = "Check if a student is enrolled in a course")
    public ResponseEntity<EnrollmentDto.IsEnrolledResponse> isEnrolled(
            @RequestParam Long studentId, @RequestParam Long courseId) {
        return ResponseEntity.ok(enrollmentService.isEnrolled(studentId, courseId));
    }

    // PUT /api/v1/enrollments/{courseId}/progress — update progress
    @PutMapping("/{courseId}/progress")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update lesson progress percentage for the authenticated student")
    public ResponseEntity<EnrollmentDto.EnrollmentResponse> updateProgress(
            @PathVariable Long courseId,
            @Valid @RequestBody EnrollmentDto.ProgressUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(enrollmentService.updateProgress(studentId, courseId, request.getProgressPercent()));
    }

    // PUT /api/v1/enrollments/{courseId}/complete — mark complete
    @PutMapping("/{courseId}/complete")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark a course as completed for the authenticated student")
    public ResponseEntity<EnrollmentDto.EnrollmentResponse> markComplete(
            @PathVariable Long courseId, HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(enrollmentService.markComplete(studentId, courseId));
    }

    // POST /api/v1/enrollments/{courseId}/certificate — issue certificate
    @PostMapping("/{courseId}/certificate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Issue a completion certificate once course is 100% complete")
    public ResponseEntity<EnrollmentDto.EnrollmentResponse> issueCertificate(
            @PathVariable Long courseId, HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(enrollmentService.issueCertificate(studentId, courseId));
    }

    // Internal — called by payment-service after successful purchase
    @PostMapping("/internal/enroll")
    @Operation(summary = "Internal: enroll student after payment (called by payment-service)")
    public ResponseEntity<EnrollmentDto.EnrollmentResponse> internalEnroll(
            @RequestParam Long studentId, @RequestParam Long courseId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enroll(studentId, courseId));
    }
}
