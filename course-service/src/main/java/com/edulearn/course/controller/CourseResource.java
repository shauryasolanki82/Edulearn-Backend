package com.edulearn.course.controller;

import com.edulearn.course.dto.CourseDto;
import com.edulearn.course.entity.Course;
import com.edulearn.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course catalog, search, filtering, and publishing APIs")
public class CourseResource {

    private final CourseService courseService;

    // ── GET /api/v1/courses — list all published (paginated) ──────────────────

    @GetMapping
    @Operation(summary = "List all published courses (paginated)")
    public ResponseEntity<CourseDto.PagedResponse<CourseDto.CourseSummary>> getAllCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "newest") String sortBy) {
        return ResponseEntity.ok(courseService.getAllCourses(page, size, sortBy));
    }

    // ── GET /api/v1/courses/search — full filter search ───────────────────────

    @GetMapping("/search")
    @Operation(summary = "Search published courses with optional keyword, category, level, language, maxPrice filters")
    public ResponseEntity<CourseDto.PagedResponse<CourseDto.CourseSummary>> searchCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Course.Category category,
            @RequestParam(required = false) Course.Level level,
            @RequestParam(required = false) Course.Language language,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        CourseDto.SearchRequest req = new CourseDto.SearchRequest(
                keyword, category, level, language, maxPrice, sortBy, page, size);
        return ResponseEntity.ok(courseService.searchCourses(req));
    }

    // ── GET /api/v1/courses/featured ──────────────────────────────────────────

    @GetMapping("/featured")
    @Operation(summary = "Get featured courses for the home page")
    public ResponseEntity<List<CourseDto.CourseSummary>> getFeaturedCourses() {
        return ResponseEntity.ok(courseService.getFeaturedCourses());
    }

    // ── GET /api/v1/courses/top ───────────────────────────────────────────────

    @GetMapping("/top")
    @Operation(summary = "Get top N courses by enrollment count")
    public ResponseEntity<List<CourseDto.CourseSummary>> getTopCourses(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(courseService.getTopCourses(limit));
    }

    // ── GET /api/v1/courses/free ──────────────────────────────────────────────

    @GetMapping("/free")
    @Operation(summary = "Get all free (price=0) published courses")
    public ResponseEntity<List<CourseDto.CourseSummary>> getFreeCourses() {
        return ResponseEntity.ok(courseService.getFreeCourses());
    }

    // ── GET /api/v1/courses/category/{category} ───────────────────────────────

    @GetMapping("/category/{category}")
    @Operation(summary = "Get published courses by category")
    public ResponseEntity<CourseDto.PagedResponse<CourseDto.CourseSummary>> getCoursesByCategory(
            @PathVariable Course.Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(courseService.getCoursesByCategory(category, page, size));
    }

    // ── GET /api/v1/courses/{id} ──────────────────────────────────────────────

    @GetMapping("/{courseId}")
    @Operation(summary = "Get full course details by ID")
    public ResponseEntity<CourseDto.CourseResponse> getCourseById(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseById(courseId));
    }

    // ── GET /api/v1/courses/instructor/{instructorId} ─────────────────────────

    @GetMapping("/instructor/{instructorId}")
    @Operation(summary = "Get all courses by a specific instructor")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CourseDto.CourseResponse>> getCoursesByInstructor(
            @PathVariable Long instructorId) {
        return ResponseEntity.ok(courseService.getCoursesByInstructor(instructorId));
    }

    // ── POST /api/v1/courses — create ─────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new course (Instructor only)")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<CourseDto.CourseResponse> createCourse(
            @Valid @RequestBody CourseDto.CourseRequest request,
            HttpServletRequest httpRequest) {

        Long instructorId   = (Long)   httpRequest.getAttribute("userId");
        String role         = (String) httpRequest.getAttribute("role");
        // For admins creating on behalf of an instructor, allow passing instructorId in header
        String overrideId   = httpRequest.getHeader("X-Instructor-Id");
        String instrName    = httpRequest.getHeader("X-User-Name");

        if ("ADMIN".equals(role) && overrideId != null) {
            instructorId = Long.parseLong(overrideId);
        }

        CourseDto.CourseResponse response =
                courseService.createCourse(instructorId, instrName != null ? instrName : "Instructor", request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── PUT /api/v1/courses/{id} — update ────────────────────────────────────

    @PutMapping("/{courseId}")
    @Operation(summary = "Update course details (owning Instructor or Admin)")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<CourseDto.CourseResponse> updateCourse(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseDto.CourseRequest request,
            HttpServletRequest httpRequest) {

        Long   requesterId = (Long)   httpRequest.getAttribute("userId");
        String role        = (String) httpRequest.getAttribute("role");

        return ResponseEntity.ok(courseService.updateCourse(
                courseId, requesterId, "ADMIN".equals(role), request));
    }

    // ── PUT /api/v1/courses/{id}/publish — publish toggle ────────────────────

    @PutMapping("/{courseId}/publish")
    @Operation(summary = "Publish or unpublish a course (owning Instructor or Admin)")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<CourseDto.CourseResponse> publishCourse(
            @PathVariable Long courseId,
            @RequestBody CourseDto.PublishRequest publishRequest,
            HttpServletRequest httpRequest) {

        Long   requesterId = (Long)   httpRequest.getAttribute("userId");
        String role        = (String) httpRequest.getAttribute("role");

        return ResponseEntity.ok(courseService.publishCourse(
                courseId, requesterId, "ADMIN".equals(role), publishRequest.isPublished()));
    }

    // ── DELETE /api/v1/courses/{id} — delete ─────────────────────────────────

    @DeleteMapping("/{courseId}")
    @Operation(summary = "Delete a course (owning Instructor or Admin)")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<CourseDto.ApiResponse> deleteCourse(
            @PathVariable Long courseId,
            HttpServletRequest httpRequest) {

        Long   requesterId = (Long)   httpRequest.getAttribute("userId");
        String role        = (String) httpRequest.getAttribute("role");

        courseService.deleteCourse(courseId, requesterId, "ADMIN".equals(role));
        return ResponseEntity.ok(CourseDto.ApiResponse.builder()
                .success(true).message("Course deleted successfully").build());
    }

    // ── PUT /api/v1/courses/{id}/featured — admin toggle ─────────────────────

    @PutMapping("/{courseId}/featured")
    @Operation(summary = "Admin: toggle course featured status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseDto.CourseResponse> toggleFeatured(
            @PathVariable Long courseId,
            @RequestParam boolean featured) {
        return ResponseEntity.ok(courseService.toggleFeatured(courseId, featured));
    }

    // ── POST /api/v1/courses/internal/enrollment-count — inter-service ────────

    @PostMapping("/internal/enrollment-count")
    @Operation(summary = "Internal: update enrollment count (called by enrollment-service)")
    public ResponseEntity<Void> updateEnrollmentCount(
            @RequestBody CourseDto.EnrollmentCountUpdate update) {
        courseService.updateEnrollmentCount(update.getCourseId(), update.getDelta());
        return ResponseEntity.ok().build();
    }

    // ── PUT /api/v1/courses/{id}/duration — inter-service ────────────────────

    @PutMapping("/internal/{courseId}/duration")
    @Operation(summary = "Internal: update total duration (called by lesson-service)")
    public ResponseEntity<Void> updateTotalDuration(
            @PathVariable Long courseId,
            @RequestParam int totalMinutes) {
        courseService.updateTotalDuration(courseId, totalMinutes);
        return ResponseEntity.ok().build();
    }
}
