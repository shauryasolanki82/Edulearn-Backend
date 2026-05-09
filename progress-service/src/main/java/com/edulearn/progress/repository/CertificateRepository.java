package com.edulearn.progress.repository;

import com.edulearn.progress.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate>      findByStudentId(Long studentId);
    Optional<Certificate>  findByStudentIdAndCourseId(Long studentId, Long courseId);
    Optional<Certificate>  findByVerificationCode(String verificationCode);
    boolean                existsByStudentIdAndCourseId(Long studentId, Long courseId);
}
