package com.edulearn.enrollment.repository;

import com.edulearn.enrollment.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
    List<Enrollment> findByStudentId(Long studentId);
    List<Enrollment> findByCourseId(Long courseId);
    List<Enrollment> findByStatus(Enrollment.EnrollmentStatus status);
    long countByCourseId(Long courseId);
    long countByStudentId(Long studentId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.status = 'ACTIVE'")
    long countActiveEnrollments();

    @Query("SELECT COUNT(DISTINCT e.studentId) FROM Enrollment e WHERE e.courseId = :courseId AND e.status = 'ACTIVE'")
    long countActiveStudentsByCourseId(@Param("courseId") Long courseId);
}
