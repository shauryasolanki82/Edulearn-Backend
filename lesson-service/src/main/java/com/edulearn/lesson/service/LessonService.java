package com.edulearn.lesson.service;

import com.edulearn.lesson.dto.LessonDto;
import com.edulearn.lesson.entity.Lesson;

import java.util.List;

/**
 * Business contract for all lesson and resource management operations.
 */
public interface LessonService {

    /** Add a new lesson to a course. Checks instructor ownership. */
    LessonDto.LessonResponse addLesson(Long courseId, Long requesterId,
                                       String role, LessonDto.LessonRequest request);

    /** Get all lessons for a course ordered by orderIndex (summaries). */
    List<LessonDto.LessonSummary> getLessonsByCourse(Long courseId);

    /** Get full lesson details including resources. Validates access. */
    LessonDto.LessonResponse getLessonById(Long lessonId, Long requesterId, String role);

    /** Get a single preview lesson — no auth required. */
    LessonDto.LessonResponse getPreviewLesson(Long lessonId);

    /** Update lesson metadata. Checks ownership. */
    LessonDto.LessonResponse updateLesson(Long lessonId, Long requesterId,
                                          String role, LessonDto.LessonRequest request);

    /** Delete a lesson and shift subsequent orderIndexes down. Checks ownership. */
    void deleteLesson(Long lessonId, Long requesterId, String role);

    /** Reorder all lessons in a course. Checks ownership. */
    List<LessonDto.LessonSummary> reorderLessons(Long courseId, Long requesterId,
                                                  String role, LessonDto.ReorderRequest request);

    /** Attach a resource to a lesson. Checks ownership. */
    LessonDto.ResourceResponse addResource(Long lessonId, Long requesterId,
                                           String role, LessonDto.ResourceRequest request);

    /** Remove a resource from a lesson. Checks ownership. */
    void removeResource(Long lessonId, Long resourceId, Long requesterId, String role);

    /** Return only isPreview=true lessons (public, no enrollment needed). */
    List<LessonDto.LessonSummary> getPreviewLessons(Long courseId);

    /** Return lessons by content type within a course. */
    List<LessonDto.LessonSummary> getLessonsByContentType(Long courseId, Lesson.ContentType contentType);

    /** Return all resources attached to a lesson. */
    List<LessonDto.ResourceResponse> getResourcesByLesson(Long lessonId);

    /** Total lesson count for a course (used by other services). */
    long countLessonsByCourse(Long courseId);
}
