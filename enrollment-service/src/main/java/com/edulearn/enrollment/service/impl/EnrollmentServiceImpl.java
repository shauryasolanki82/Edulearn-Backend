package com.edulearn.enrollment.service.impl;

import com.edulearn.enrollment.dto.EnrollmentDto;
import com.edulearn.enrollment.entity.Enrollment;
import com.edulearn.enrollment.exception.AlreadyEnrolledException;
import com.edulearn.enrollment.exception.EnrollmentNotFoundException;
import com.edulearn.enrollment.repository.EnrollmentRepository;
import com.edulearn.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final RestTemplate restTemplate;

    @Value("${app.course-service.url}") private String courseServiceUrl;
    @Value("${app.progress-service.url:http://localhost:8086}") private String progressServiceUrl;

    @Override
    @Transactional
    public EnrollmentDto.EnrollmentResponse enroll(Long studentId, Long courseId) {
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new AlreadyEnrolledException("Student " + studentId + " is already enrolled in course " + courseId);
        }
        Enrollment enrollment = Enrollment.builder()
                .studentId(studentId).courseId(courseId)
                .status(Enrollment.EnrollmentStatus.ACTIVE)
                .progressPercent(0.0).certificateIssued(false).build();
        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Student {} enrolled in course {}", studentId, courseId);
        notifyCourseService(courseId, 1);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void unenroll(Long studentId, Long courseId) {
        Enrollment e = findOrThrow(studentId, courseId);
        e.setStatus(Enrollment.EnrollmentStatus.CANCELLED);
        enrollmentRepository.save(e);
        log.info("Student {} unenrolled from course {}", studentId, courseId);
        notifyCourseService(courseId, -1);
    }

    @Override
    public List<EnrollmentDto.EnrollmentResponse> getEnrollmentsByStudent(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<EnrollmentDto.EnrollmentResponse> getEnrollmentsByCourse(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public EnrollmentDto.EnrollmentResponse getEnrollment(Long studentId, Long courseId) {
        return toResponse(findOrThrow(studentId, courseId));
    }

    @Override
    @Transactional
    public EnrollmentDto.EnrollmentResponse updateProgress(Long studentId, Long courseId, Double progressPercent) {
        Enrollment e = findOrThrow(studentId, courseId);
        e.setProgressPercent(Math.min(100.0, Math.max(0.0, progressPercent)));
        if (e.getProgressPercent() >= 100.0) {
            e.setStatus(Enrollment.EnrollmentStatus.COMPLETED);
            e.setCompletedAt(LocalDateTime.now());
        }
        return toResponse(enrollmentRepository.save(e));
    }

    @Override
    @Transactional
    public EnrollmentDto.EnrollmentResponse markComplete(Long studentId, Long courseId) {
        Enrollment e = findOrThrow(studentId, courseId);
        e.setProgressPercent(100.0);
        e.setStatus(Enrollment.EnrollmentStatus.COMPLETED);
        e.setCompletedAt(LocalDateTime.now());
        return toResponse(enrollmentRepository.save(e));
    }

    @Override
    public EnrollmentDto.IsEnrolledResponse isEnrolled(Long studentId, Long courseId) {
        return enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .map(e -> EnrollmentDto.IsEnrolledResponse.builder()
                        .studentId(studentId).courseId(courseId).enrolled(true)
                        .status(e.getStatus().name()).build())
                .orElse(EnrollmentDto.IsEnrolledResponse.builder()
                        .studentId(studentId).courseId(courseId).enrolled(false).build());
    }

    @Override
    @Transactional
    public EnrollmentDto.EnrollmentResponse issueCertificate(Long studentId, Long courseId) {
        Enrollment e = findOrThrow(studentId, courseId);
        if (e.getProgressPercent() < 100.0) {
            throw new IllegalStateException("Cannot issue certificate: course not 100% complete");
        }
        e.setCertificateIssued(true);
        return toResponse(enrollmentRepository.save(e));
    }

    @Override
    public long getEnrollmentCount(Long courseId) {
        return enrollmentRepository.countByCourseId(courseId);
    }

    @Override
    public EnrollmentDto.EnrollmentStatsResponse getStats(Long courseId) {
        long total = enrollmentRepository.countByCourseId(courseId);
        long active = enrollmentRepository.countActiveStudentsByCourseId(courseId);
        long completed = enrollmentRepository.findByCourseId(courseId).stream()
                .filter(e -> e.getStatus() == Enrollment.EnrollmentStatus.COMPLETED).count();
        return EnrollmentDto.EnrollmentStatsResponse.builder()
                .courseId(courseId).totalEnrollments(total)
                .activeEnrollments(active).completedEnrollments(completed).build();
    }

    private Enrollment findOrThrow(Long studentId, Long courseId) {
        return enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new EnrollmentNotFoundException(
                        "Enrollment not found for student=" + studentId + " course=" + courseId));
    }

    private void notifyCourseService(Long courseId, int delta) {
        try {
            String url = courseServiceUrl + "/api/v1/courses/internal/enrollment-count";
            restTemplate.postForEntity(url,
                    java.util.Map.of("courseId", courseId, "delta", delta), Void.class);
        } catch (Exception e) {
            log.warn("Could not notify course-service for enrollment count update: {}", e.getMessage());
        }
    }

    private EnrollmentDto.EnrollmentResponse toResponse(Enrollment e) {
        return EnrollmentDto.EnrollmentResponse.builder()
                .enrollmentId(e.getEnrollmentId()).studentId(e.getStudentId()).courseId(e.getCourseId())
                .status(e.getStatus().name()).progressPercent(e.getProgressPercent())
                .certificateIssued(e.getCertificateIssued()).enrolledAt(e.getEnrolledAt())
                .completedAt(e.getCompletedAt()).build();
    }
}
