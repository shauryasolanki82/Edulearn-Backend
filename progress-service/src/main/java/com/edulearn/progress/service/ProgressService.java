package com.edulearn.progress.service;

import com.edulearn.progress.dto.ProgressDto;

import java.util.List;

public interface ProgressService {
    ProgressDto.ProgressResponse       trackProgress(Long studentId, ProgressDto.TrackRequest request);
    ProgressDto.ProgressResponse       markLessonComplete(Long studentId, Long lessonId, Long courseId);
    ProgressDto.ProgressResponse       getLessonProgress(Long studentId, Long lessonId);
    ProgressDto.CourseProgressResponse getCourseProgress(Long studentId, Long courseId, long totalLessons);
    List<ProgressDto.ProgressResponse> getAllProgressByStudent(Long studentId);
    ProgressDto.CertificateResponse    issueCertificate(Long studentId, ProgressDto.IssueCertificateRequest request);
    ProgressDto.CertificateResponse    getCertificate(Long studentId, Long courseId);
    List<ProgressDto.CertificateResponse> getCertificatesByStudent(Long studentId);
    ProgressDto.VerifyResponse         verifyCertificate(String verificationCode);
}
