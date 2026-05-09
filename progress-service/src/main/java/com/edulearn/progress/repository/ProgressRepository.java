package com.edulearn.progress.repository;

import com.edulearn.progress.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, Long> {
    Optional<Progress> findByStudentIdAndLessonId(Long studentId, Long lessonId);
    List<Progress>     findByStudentIdAndCourseId(Long studentId, Long courseId);
    List<Progress>     findByStudentId(Long studentId);

    @Query("SELECT COUNT(p) FROM Progress p WHERE p.studentId=:sid AND p.courseId=:cid AND p.isCompleted=true")
    long countCompletedByStudentIdAndCourseId(@Param("sid") Long studentId, @Param("cid") Long courseId);

    boolean existsByStudentIdAndLessonId(Long studentId, Long lessonId);
}
