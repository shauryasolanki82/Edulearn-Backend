package com.edulearn.course.service.impl;

import com.edulearn.course.dto.CourseDto;
import com.edulearn.course.entity.Course;
import com.edulearn.course.exception.CourseNotFoundException;
import com.edulearn.course.exception.ForbiddenException;
import com.edulearn.course.repository.CourseRepository;
import com.edulearn.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    // ── Create Course ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CourseDto.CourseResponse createCourse(Long instructorId, String instructorName,
                                                  CourseDto.CourseRequest request) {
        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .level(request.getLevel())
                .price(request.getPrice())
                .instructorId(instructorId)
                .instructorName(instructorName)
                .thumbnailUrl(request.getThumbnailUrl())
                .language(request.getLanguage() != null ? request.getLanguage() : Course.Language.ENGLISH)
                .whatYouWillLearn(request.getWhatYouWillLearn())
                .requirements(request.getRequirements())
                .isPublished(false)
                .isFeatured(false)
                .totalEnrollments(0)
                .totalDuration(0)
                .averageRating(0.0)
                .build();

        Course saved = courseRepository.save(course);
        log.info("Course created: id={} title='{}' by instructor={}", saved.getCourseId(),
                saved.getTitle(), instructorId);
        return toCourseResponse(saved);
    }

    // ── Get Course By ID ──────────────────────────────────────────────────────

    @Override
    public CourseDto.CourseResponse getCourseById(Long courseId) {
        Course course = findOrThrow(courseId);
        return toCourseResponse(course);
    }

    // ── Get All Published Courses (paginated) ─────────────────────────────────

    @Override
    public CourseDto.PagedResponse<CourseDto.CourseSummary> getAllCourses(int page, int size,
                                                                           String sortBy) {
        Pageable pageable = buildPageable(page, size, sortBy);
        Page<Course> coursePage = courseRepository.findByIsPublishedTrue(pageable);
        return toPagedSummary(coursePage);
    }

    // ── Get Courses By Category ───────────────────────────────────────────────

    @Override
    public CourseDto.PagedResponse<CourseDto.CourseSummary> getCoursesByCategory(
            Course.Category category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Course> coursePage = courseRepository.findByCategory(category, pageable);
        // Filter published only
        List<CourseDto.CourseSummary> content = coursePage.getContent().stream()
                .filter(Course::getIsPublished)
                .map(this::toCourseSummary)
                .collect(Collectors.toList());
        return CourseDto.PagedResponse.<CourseDto.CourseSummary>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(coursePage.getTotalElements())
                .totalPages(coursePage.getTotalPages())
                .last(coursePage.isLast())
                .build();
    }

    // ── Get Courses By Instructor ─────────────────────────────────────────────

    @Override
    public List<CourseDto.CourseResponse> getCoursesByInstructor(Long instructorId) {
        return courseRepository.findByInstructorId(instructorId).stream()
                .map(this::toCourseResponse)
                .collect(Collectors.toList());
    }

    // ── Search Courses ────────────────────────────────────────────────────────

    @Override
    public CourseDto.PagedResponse<CourseDto.CourseSummary> searchCourses(
            CourseDto.SearchRequest request) {
        Pageable pageable = buildPageable(request.getPage(), request.getSize(), request.getSortBy());

        Page<Course> result = courseRepository.searchWithFilters(
                request.getKeyword(),
                request.getCategory(),
                request.getLevel(),
                request.getLanguage(),
                request.getMaxPrice(),
                pageable);

        return toPagedSummary(result);
    }

    // ── Update Course ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CourseDto.CourseResponse updateCourse(Long courseId, Long requesterId,
                                                  boolean isAdmin, CourseDto.CourseRequest request) {
        Course course = findOrThrow(courseId);
        assertOwnerOrAdmin(course, requesterId, isAdmin, "update");

        course.setTitle(request.getTitle());
        if (request.getDescription() != null) course.setDescription(request.getDescription());
        if (request.getCategory() != null) course.setCategory(request.getCategory());
        if (request.getLevel() != null) course.setLevel(request.getLevel());
        if (request.getPrice() != null) course.setPrice(request.getPrice());
        if (request.getThumbnailUrl() != null) course.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getLanguage() != null) course.setLanguage(request.getLanguage());
        if (request.getWhatYouWillLearn() != null) course.setWhatYouWillLearn(request.getWhatYouWillLearn());
        if (request.getRequirements() != null) course.setRequirements(request.getRequirements());

        Course saved = courseRepository.save(course);
        log.info("Course updated: id={}", courseId);
        return toCourseResponse(saved);
    }

    // ── Publish / Unpublish ───────────────────────────────────────────────────

    @Override
    @Transactional
    public CourseDto.CourseResponse publishCourse(Long courseId, Long requesterId,
                                                   boolean isAdmin, boolean publish) {
        Course course = findOrThrow(courseId);
        assertOwnerOrAdmin(course, requesterId, isAdmin, publish ? "publish" : "unpublish");

        course.setIsPublished(publish);
        Course saved = courseRepository.save(course);
        log.info("Course {}: id={}", publish ? "published" : "unpublished", courseId);
        return toCourseResponse(saved);
    }

    // ── Delete Course ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteCourse(Long courseId, Long requesterId, boolean isAdmin) {
        Course course = findOrThrow(courseId);
        assertOwnerOrAdmin(course, requesterId, isAdmin, "delete");
        courseRepository.delete(course);
        log.info("Course deleted: id={}", courseId);
    }

    // ── Featured Courses ──────────────────────────────────────────────────────

    @Override
    public List<CourseDto.CourseSummary> getFeaturedCourses() {
        return courseRepository.findByIsFeaturedTrueAndIsPublishedTrue()
                .stream().map(this::toCourseSummary).collect(Collectors.toList());
    }

    // ── Top Courses ───────────────────────────────────────────────────────────

    @Override
    public List<CourseDto.CourseSummary> getTopCourses(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return courseRepository.findTopByEnrollments(pageable)
                .stream().map(this::toCourseSummary).collect(Collectors.toList());
    }

    // ── Free Courses ──────────────────────────────────────────────────────────

    @Override
    public List<CourseDto.CourseSummary> getFreeCourses() {
        return courseRepository.findFreeCourses()
                .stream().map(this::toCourseSummary).collect(Collectors.toList());
    }

    // ── Toggle Featured ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public CourseDto.CourseResponse toggleFeatured(Long courseId, boolean featured) {
        Course course = findOrThrow(courseId);
        course.setIsFeatured(featured);
        return toCourseResponse(courseRepository.save(course));
    }

    // ── Update Enrollment Count (called by enrollment-service) ────────────────

    @Override
    @Transactional
    public void updateEnrollmentCount(Long courseId, int delta) {
        Course course = findOrThrow(courseId);
        int updated = Math.max(0, course.getTotalEnrollments() + delta);
        course.setTotalEnrollments(updated);
        courseRepository.save(course);
    }

    // ── Update Total Duration (called by lesson-service) ──────────────────────

    @Override
    @Transactional
    public void updateTotalDuration(Long courseId, int totalMinutes) {
        Course course = findOrThrow(courseId);
        course.setTotalDuration(totalMinutes);
        courseRepository.save(course);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Course findOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found: " + courseId));
    }

    private void assertOwnerOrAdmin(Course course, Long requesterId,
                                    boolean isAdmin, String action) {
        if (!isAdmin && !course.getInstructorId().equals(requesterId)) {
            throw new ForbiddenException("You are not allowed to " + action + " this course");
        }
    }

    private Pageable buildPageable(int page, int size, String sortBy) {
        Sort sort = switch (sortBy != null ? sortBy : "newest") {
            case "popular"    -> Sort.by("totalEnrollments").descending();
            case "rating"     -> Sort.by("averageRating").descending();
            case "price_asc"  -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            default           -> Sort.by("createdAt").descending();  // "newest"
        };
        return PageRequest.of(page, size, sort);
    }

    private CourseDto.PagedResponse<CourseDto.CourseSummary> toPagedSummary(Page<Course> page) {
        List<CourseDto.CourseSummary> content = page.getContent()
                .stream().map(this::toCourseSummary).collect(Collectors.toList());
        return CourseDto.PagedResponse.<CourseDto.CourseSummary>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    public CourseDto.CourseResponse toCourseResponse(Course c) {
        return CourseDto.CourseResponse.builder()
                .courseId(c.getCourseId())
                .title(c.getTitle())
                .description(c.getDescription())
                .category(c.getCategory() != null ? c.getCategory().name() : null)
                .level(c.getLevel() != null ? c.getLevel().name() : null)
                .price(c.getPrice())
                .instructorId(c.getInstructorId())
                .instructorName(c.getInstructorName())
                .thumbnailUrl(c.getThumbnailUrl())
                .totalDuration(c.getTotalDuration())
                .isPublished(c.getIsPublished())
                .isFeatured(c.getIsFeatured())
                .language(c.getLanguage() != null ? c.getLanguage().name() : null)
                .whatYouWillLearn(c.getWhatYouWillLearn())
                .requirements(c.getRequirements())
                .totalEnrollments(c.getTotalEnrollments())
                .averageRating(c.getAverageRating())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    public CourseDto.CourseSummary toCourseSummary(Course c) {
        return CourseDto.CourseSummary.builder()
                .courseId(c.getCourseId())
                .title(c.getTitle())
                .category(c.getCategory() != null ? c.getCategory().name() : null)
                .level(c.getLevel() != null ? c.getLevel().name() : null)
                .price(c.getPrice())
                .instructorId(c.getInstructorId())
                .instructorName(c.getInstructorName())
                .thumbnailUrl(c.getThumbnailUrl())
                .totalDuration(c.getTotalDuration())
                .totalEnrollments(c.getTotalEnrollments())
                .averageRating(c.getAverageRating())
                .language(c.getLanguage() != null ? c.getLanguage().name() : null)
                .isFeatured(c.getIsFeatured())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
