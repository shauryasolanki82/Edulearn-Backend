package com.edulearn.lesson.service.impl;

import com.edulearn.lesson.dto.LessonDto;
import com.edulearn.lesson.entity.Lesson;
import com.edulearn.lesson.entity.Resource;
import com.edulearn.lesson.exception.ForbiddenException;
import com.edulearn.lesson.exception.LessonNotFoundException;
import com.edulearn.lesson.exception.ResourceNotFoundException;
import com.edulearn.lesson.repository.LessonRepository;
import com.edulearn.lesson.repository.ResourceRepository;
import com.edulearn.lesson.service.LessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final ResourceRepository resourceRepository;
    private final RestTemplate restTemplate;

    @Value("${app.course-service.url}")
    private String courseServiceUrl;

    // ── Add Lesson ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LessonDto.LessonResponse addLesson(Long courseId, Long requesterId,
                                               String role, LessonDto.LessonRequest request) {
        // Instructors can only add to their own courses — verified via courseId ownership.
        // ADMIN may add to any course. For simplicity we trust the JWT role here;
        // full ownership check would call course-service via RestTemplate.
        if (!"ADMIN".equals(role)) {
            verifyInstructorOwnsCourse(courseId, requesterId);
        }

        int nextOrder = request.getOrderIndex() != null
                ? request.getOrderIndex()
                : lessonRepository.maxOrderIndexByCourseId(courseId) + 1;

        Lesson lesson = Lesson.builder()
                .courseId(courseId)
                .instructorId(requesterId)
                .title(request.getTitle())
                .contentType(request.getContentType())
                .contentUrl(request.getContentUrl())
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 0)
                .orderIndex(nextOrder)
                .description(request.getDescription())
                .isPreview(request.getIsPreview() != null ? request.getIsPreview() : false)
                .resources(new ArrayList<>())
                .build();

        Lesson saved = lessonRepository.save(lesson);
        log.info("Lesson added: id={} courseId={} order={}", saved.getLessonId(), courseId, nextOrder);
        notifyCourseDurationUpdate(courseId);
        return toLessonResponse(saved);
    }

    // ── Get Lessons By Course ─────────────────────────────────────────────────

    @Override
    public List<LessonDto.LessonSummary> getLessonsByCourse(Long courseId) {
        return lessonRepository.findByCourseIdOrderByOrderIndex(courseId)
                .stream().map(this::toLessonSummary).collect(Collectors.toList());
    }

    // ── Get Lesson By Id (with access control) ────────────────────────────────

    @Override
    public LessonDto.LessonResponse getLessonById(Long lessonId, Long requesterId, String role) {
        Lesson lesson = findLessonOrThrow(lessonId);
        // ADMIN and INSTRUCTOR always have full access
        // STUDENT can only access if enrolled (enrollment check done by frontend/gateway for now)
        // Preview lessons are accessible to everyone
        return toLessonResponse(lesson);
    }

    // ── Get Single Preview Lesson (no auth) ───────────────────────────────────

    @Override
    public LessonDto.LessonResponse getPreviewLesson(Long lessonId) {
        Lesson lesson = findLessonOrThrow(lessonId);
        if (!lesson.getIsPreview()) {
            throw new ForbiddenException("This lesson requires enrollment to access");
        }
        return toLessonResponse(lesson);
    }

    // ── Update Lesson ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LessonDto.LessonResponse updateLesson(Long lessonId, Long requesterId,
                                                  String role, LessonDto.LessonRequest request) {
        Lesson lesson = findLessonOrThrow(lessonId);
        if (!"ADMIN".equals(role)) {
            verifyInstructorOwnsCourse(lesson.getCourseId(), requesterId);
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            lesson.setTitle(request.getTitle());
        }
        if (request.getContentType() != null) lesson.setContentType(request.getContentType());
        if (request.getContentUrl() != null)  lesson.setContentUrl(request.getContentUrl());
        if (request.getDurationMinutes() != null) lesson.setDurationMinutes(request.getDurationMinutes());
        if (request.getDescription() != null) lesson.setDescription(request.getDescription());
        if (request.getIsPreview() != null)   lesson.setIsPreview(request.getIsPreview());

        Lesson saved = lessonRepository.save(lesson);
        log.info("Lesson updated: id={}", lessonId);
        notifyCourseDurationUpdate(lesson.getCourseId());
        return toLessonResponse(saved);
    }

    // ── Delete Lesson ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteLesson(Long lessonId, Long requesterId, String role) {
        Lesson lesson = findLessonOrThrow(lessonId);
        if (!"ADMIN".equals(role)) {
            verifyInstructorOwnsCourse(lesson.getCourseId(), requesterId);
        }

        Long courseId = lesson.getCourseId();
        int deletedOrder = lesson.getOrderIndex();

        lessonRepository.delete(lesson);
        lessonRepository.decrementOrderIndexAfter(courseId, deletedOrder);
        log.info("Lesson deleted: id={} courseId={}", lessonId, courseId);
        notifyCourseDurationUpdate(courseId);
    }

    // ── Reorder Lessons ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<LessonDto.LessonSummary> reorderLessons(Long courseId, Long requesterId,
                                                         String role,
                                                         LessonDto.ReorderRequest request) {
        if (!"ADMIN".equals(role)) {
            verifyInstructorOwnsCourse(courseId, requesterId);
        }

        List<Long> lessonIds = request.getLessonIds();
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndex(courseId);

        List<Long> existingIds = lessons.stream()
                .map(Lesson::getLessonId).collect(Collectors.toList());
        for (Long id : lessonIds) {
            if (!existingIds.contains(id)) {
                throw new LessonNotFoundException("Lesson " + id + " does not belong to course " + courseId);
            }
        }

        for (int i = 0; i < lessonIds.size(); i++) {
            final int newIndex = i + 1;
            final Long id = lessonIds.get(i);
            lessons.stream()
                    .filter(l -> l.getLessonId().equals(id))
                    .findFirst()
                    .ifPresent(l -> l.setOrderIndex(newIndex));
        }

        lessonRepository.saveAll(lessons);
        log.info("Lessons reordered for courseId={}", courseId);
        return lessonRepository.findByCourseIdOrderByOrderIndex(courseId)
                .stream().map(this::toLessonSummary).collect(Collectors.toList());
    }

    // ── Add Resource ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LessonDto.ResourceResponse addResource(Long lessonId, Long requesterId,
                                                   String role, LessonDto.ResourceRequest request) {
        Lesson lesson = findLessonOrThrow(lessonId);
        if (!"ADMIN".equals(role)) {
            verifyInstructorOwnsCourse(lesson.getCourseId(), requesterId);
        }

        Resource resource = Resource.builder()
                .lesson(lesson)
                .name(request.getName())
                .fileUrl(request.getFileUrl())
                .fileType(request.getFileType())
                .sizeKb(request.getSizeKb())
                .build();

        Resource saved = resourceRepository.save(resource);
        log.info("Resource added: id={} lessonId={}", saved.getResourceId(), lessonId);
        return toResourceResponse(saved);
    }

    // ── Remove Resource ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public void removeResource(Long lessonId, Long resourceId, Long requesterId, String role) {
        Lesson lesson = findLessonOrThrow(lessonId);
        if (!"ADMIN".equals(role)) {
            verifyInstructorOwnsCourse(lesson.getCourseId(), requesterId);
        }

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));
        resourceRepository.delete(resource);
        log.info("Resource deleted: id={}", resourceId);
    }

    // ── Get Preview Lessons (public) ──────────────────────────────────────────

    @Override
    public List<LessonDto.LessonSummary> getPreviewLessons(Long courseId) {
        return lessonRepository.findByCourseIdAndIsPreviewTrue(courseId)
                .stream().map(this::toLessonSummary).collect(Collectors.toList());
    }

    // ── Get Lessons By Content Type ───────────────────────────────────────────

    @Override
    public List<LessonDto.LessonSummary> getLessonsByContentType(Long courseId,
                                                                  Lesson.ContentType contentType) {
        return lessonRepository.findByCourseIdAndContentType(courseId, contentType)
                .stream().map(this::toLessonSummary).collect(Collectors.toList());
    }

    // ── Get Resources By Lesson ───────────────────────────────────────────────

    @Override
    public List<LessonDto.ResourceResponse> getResourcesByLesson(Long lessonId) {
        findLessonOrThrow(lessonId);
        return resourceRepository.findByLesson_LessonId(lessonId)
                .stream().map(this::toResourceResponse).collect(Collectors.toList());
    }

    // ── Count Lessons By Course ───────────────────────────────────────────────

    @Override
    public long countLessonsByCourse(Long courseId) {
        return lessonRepository.countByCourseId(courseId);
    }

    // ── Ownership Verification ────────────────────────────────────────────────

    /**
     * Verifies that the requesting instructor owns at least one lesson in this course,
     * OR that there are no lessons yet (first lesson being added).
     * For a full check, a real LMS would call course-service to verify instructorId.
     * Here we check the existing lessons' courseId context as a proxy.
     */
    private void verifyInstructorOwnsCourse(Long courseId, Long instructorId) {
        // Check if this instructor already has a lesson in the course (ownership proxy)
        boolean hasLessons = lessonRepository.countByCourseId(courseId) > 0;
        if (hasLessons) {
            boolean owns = lessonRepository.existsByCourseIdAndInstructorId(courseId, instructorId);
            if (!owns) {
                throw new ForbiddenException(
                        "You do not own this course and cannot manage its lessons");
            }
        }
        // If no lessons yet, allow — first lesson from any instructor for this courseId
    }

    // ── Inter-service: notify course-service of duration change ───────────────

    private void notifyCourseDurationUpdate(Long courseId) {
        try {
            Integer totalMinutes = lessonRepository.sumDurationByCourseId(courseId);
            String url = courseServiceUrl + "/api/v1/courses/internal/" + courseId
                    + "/duration?totalMinutes=" + (totalMinutes != null ? totalMinutes : 0);
            restTemplate.put(url, null);
            log.debug("Notified course-service: courseId={} totalMinutes={}", courseId, totalMinutes);
        } catch (Exception e) {
            log.warn("Could not notify course-service for courseId={}: {}", courseId, e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Lesson findLessonOrThrow(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LessonNotFoundException("Lesson not found: " + lessonId));
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private LessonDto.LessonResponse toLessonResponse(Lesson l) {
        List<LessonDto.ResourceResponse> resources = l.getResources() == null
                ? List.of()
                : l.getResources().stream().map(this::toResourceResponse).collect(Collectors.toList());

        return LessonDto.LessonResponse.builder()
                .lessonId(l.getLessonId())
                .courseId(l.getCourseId())
                .title(l.getTitle())
                .contentType(l.getContentType() != null ? l.getContentType().name() : null)
                .contentUrl(l.getContentUrl())
                .durationMinutes(l.getDurationMinutes())
                .orderIndex(l.getOrderIndex())
                .description(l.getDescription())
                .isPreview(l.getIsPreview())
                .resources(resources)
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }

    private LessonDto.LessonSummary toLessonSummary(Lesson l) {
        int resourceCount = (int) resourceRepository.countByLesson_LessonId(l.getLessonId());
        return LessonDto.LessonSummary.builder()
                .lessonId(l.getLessonId())
                .courseId(l.getCourseId())
                .title(l.getTitle())
                .contentType(l.getContentType() != null ? l.getContentType().name() : null)
                .durationMinutes(l.getDurationMinutes())
                .orderIndex(l.getOrderIndex())
                .isPreview(l.getIsPreview())
                .resourceCount(resourceCount)
                .build();
    }

    private LessonDto.ResourceResponse toResourceResponse(Resource r) {
        return LessonDto.ResourceResponse.builder()
                .resourceId(r.getResourceId())
                .lessonId(r.getLesson() != null ? r.getLesson().getLessonId() : null)
                .name(r.getName())
                .fileUrl(r.getFileUrl())
                .fileType(r.getFileType() != null ? r.getFileType().name() : null)
                .sizeKb(r.getSizeKb())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
