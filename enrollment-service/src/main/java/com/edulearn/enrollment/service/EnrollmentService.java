package com.edulearn.enrollment.service;

import com.edulearn.enrollment.dto.EnrollmentDto;
import com.edulearn.enrollment.entity.Enrollment;

import java.util.List;

public interface EnrollmentService {
    EnrollmentDto.EnrollmentResponse enroll(Long studentId, Long courseId);
    void unenroll(Long studentId, Long courseId);
    List<EnrollmentDto.EnrollmentResponse> getEnrollmentsByStudent(Long studentId);
    List<EnrollmentDto.EnrollmentResponse> getEnrollmentsByCourse(Long courseId);
    EnrollmentDto.EnrollmentResponse getEnrollment(Long studentId, Long courseId);
    EnrollmentDto.EnrollmentResponse updateProgress(Long studentId, Long courseId, Double progressPercent);
    EnrollmentDto.EnrollmentResponse markComplete(Long studentId, Long courseId);
    EnrollmentDto.IsEnrolledResponse isEnrolled(Long studentId, Long courseId);
    EnrollmentDto.EnrollmentResponse issueCertificate(Long studentId, Long courseId);
    long getEnrollmentCount(Long courseId);
    EnrollmentDto.EnrollmentStatsResponse getStats(Long courseId);
}
