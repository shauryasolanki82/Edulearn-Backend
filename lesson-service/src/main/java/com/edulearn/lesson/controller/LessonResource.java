package com.edulearn.lesson.controller;

import com.edulearn.lesson.dto.LessonDto;
import com.edulearn.lesson.service.LessonService;
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
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
@Tag(name = "Lessons", description = "Lesson content management, ordering, and resource attachments")
public class LessonResource {

    private final LessonService lessonService;

    // ── GET /api/v1/lessons/course/{courseId} — list lessons for a course ─────

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get all lessons for a course (summary; contentUrl hidden for non-enrolled)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LessonDto.LessonSummary>> getLessonsByCourse(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(lessonService.getLessonsByCourse(courseId));
    }

    // ── GET /api/v1/lessons/course/{courseId}/preview — public preview lessons ─

    @GetMapping("/course/{courseId}/preview")
    @Operation(summary = "Get preview lessons for a course — public, no auth needed")
    public ResponseEntity<List<LessonDto.LessonSummary>> getPreviewLessons(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(lessonService.getPreviewLessons(courseId));
    }

    // ── GET /api/v1/lessons/{lessonId} — full lesson with contentUrl ──────────

    @GetMapping("/{lessonId}")
    @Operation(summary = "Get full lesson details including contentUrl (enrolled student, instructor, or admin)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LessonDto.LessonResponse> getLessonById(
            @PathVariable Long lessonId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return ResponseEntity.ok(lessonService.getLessonById(lessonId, userId, role));
    }

    // ── GET /api/v1/lessons/{lessonId}/preview — single preview check ────────

    @GetMapping("/{lessonId}/preview")
    @Operation(summary = "Get a single preview lesson — public")
    public ResponseEntity<LessonDto.LessonResponse> getPreviewLesson(
            @PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonService.getPreviewLesson(lessonId));
    }

    // ── POST /api/v1/lessons/course/{courseId} — add lesson ──────────────────

    @PostMapping("/course/{courseId}")
    @Operation(summary = "Add a lesson to a course (Instructor or Admin)")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<LessonDto.LessonResponse> addLesson(
            @PathVariable Long courseId,
            @Valid @RequestBody LessonDto.LessonRequest request,
            HttpServletRequest httpRequest) {
        Long instructorId = (Long) httpRequest.getAttribute("userId");
        String role       = (String) httpRequest.getAttribute("role");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonService.addLesson(courseId, instructorId, role, request));
    }

    // ── PUT /api/v1/lessons/{lessonId} — update lesson ───────────────────────

    @PutMapping("/{lessonId}")
    @Operation(summary = "Update a lesson (owning Instructor or Admin)")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<LessonDto.LessonResponse> updateLesson(
            @PathVariable Long lessonId,
            @Valid @RequestBody LessonDto.LessonRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String role = (String) httpRequest.getAttribute("role");
        return ResponseEntity.ok(lessonService.updateLesson(lessonId, userId, role, request));
    }

    // ── DELETE /api/v1/lessons/{lessonId} — delete lesson ────────────────────

    @DeleteMapping("/{lessonId}")
    @Operation(summary = "Delete a lesson (owning Instructor or Admin)")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<LessonDto.ApiResponse> deleteLesson(
            @PathVariable Long lessonId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String role = (String) httpRequest.getAttribute("role");
        lessonService.deleteLesson(lessonId, userId, role);
        return ResponseEntity.ok(LessonDto.ApiResponse.builder()
                .success(true).message("Lesson deleted successfully").build());
    }

    // ── PUT /api/v1/lessons/course/{courseId}/reorder — reorder lessons ───────

    @PutMapping("/course/{courseId}/reorder")
    @Operation(summary = "Reorder lessons in a course by supplying an ordered list of lessonIds")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<LessonDto.LessonSummary>> reorderLessons(
            @PathVariable Long courseId,
            @RequestBody LessonDto.ReorderRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String role = (String) httpRequest.getAttribute("role");
        return ResponseEntity.ok(lessonService.reorderLessons(courseId, userId, role, request));
    }

    // ── POST /api/v1/lessons/{lessonId}/resources — add resource ─────────────

    @PostMapping("/{lessonId}/resources")
    @Operation(summary = "Add a supplementary resource to a lesson (Instructor or Admin)")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<LessonDto.ResourceResponse> addResource(
            @PathVariable Long lessonId,
            @Valid @RequestBody LessonDto.ResourceRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String role = (String) httpRequest.getAttribute("role");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonService.addResource(lessonId, userId, role, request));
    }

    // ── DELETE /api/v1/lessons/{lessonId}/resources/{resourceId} — remove ────

    @DeleteMapping("/{lessonId}/resources/{resourceId}")
    @Operation(summary = "Remove a resource from a lesson (Instructor or Admin)")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<LessonDto.ApiResponse> removeResource(
            @PathVariable Long lessonId,
            @PathVariable Long resourceId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String role = (String) httpRequest.getAttribute("role");
        lessonService.removeResource(lessonId, resourceId, userId, role);
        return ResponseEntity.ok(LessonDto.ApiResponse.builder()
                .success(true).message("Resource removed successfully").build());
    }

    // ── GET /api/v1/lessons/{lessonId}/resources — list resources ─────────────

    @GetMapping("/{lessonId}/resources")
    @Operation(summary = "List all resources attached to a lesson (enrolled student, instructor, or admin)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LessonDto.ResourceResponse>> getResources(
            @PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonService.getResourcesByLesson(lessonId));
    }

    // ── GET /api/v1/lessons/internal/course/{courseId}/count — inter-service ──

    @GetMapping("/internal/course/{courseId}/count")
    @Operation(summary = "Internal: count lessons in a course (used by other services)")
    public ResponseEntity<Long> countLessonsByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(lessonService.countLessonsByCourse(courseId));
    }
}
