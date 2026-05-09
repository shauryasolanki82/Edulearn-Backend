package com.edulearn.lesson.repository;

import com.edulearn.lesson.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseIdOrderByOrderIndex(Long courseId);

    List<Lesson> findByCourseId(Long courseId);

    Optional<Lesson> findByLessonId(Long lessonId);

    List<Lesson> findByCourseIdAndContentType(Long courseId, Lesson.ContentType contentType);

    long countByCourseId(Long courseId);

    /** Preview-accessible lessons for a course */
    List<Lesson> findByCourseIdAndIsPreviewTrue(Long courseId);

    /** Sum of all lesson durations for a given course */
    @Query("SELECT COALESCE(SUM(l.durationMinutes), 0) FROM Lesson l WHERE l.courseId = :courseId")
    Integer sumDurationByCourseId(@Param("courseId") Long courseId);

    /** Shift orderIndex up for all lessons after the deleted position */
    @Modifying
    @Query("UPDATE Lesson l SET l.orderIndex = l.orderIndex - 1 " +
           "WHERE l.courseId = :courseId AND l.orderIndex > :orderIndex")
    void decrementOrderIndexAfter(@Param("courseId") Long courseId,
                                  @Param("orderIndex") int orderIndex);

    /** Max orderIndex for a course (used when appending a new lesson) */
    @Query("SELECT COALESCE(MAX(l.orderIndex), 0) FROM Lesson l WHERE l.courseId = :courseId")
    Integer maxOrderIndexByCourseId(@Param("courseId") Long courseId);

    /** Check whether a lesson belongs to a given course */
    boolean existsByLessonIdAndCourseId(Long lessonId, Long courseId);

    /** Check whether the instructor owns any lesson in this course (ownership proxy) */
    boolean existsByCourseIdAndInstructorId(Long courseId, Long instructorId);
}
