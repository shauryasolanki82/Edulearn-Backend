package com.edulearn.progress.service.impl;

import com.edulearn.progress.dto.ProgressDto;
import com.edulearn.progress.entity.Certificate;
import com.edulearn.progress.entity.Progress;
import com.edulearn.progress.exception.CertificateNotFoundException;
import com.edulearn.progress.exception.ProgressNotFoundException;
import com.edulearn.progress.repository.CertificateRepository;
import com.edulearn.progress.repository.ProgressRepository;
import com.edulearn.progress.service.ProgressService;
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
public class ProgressServiceImpl implements ProgressService {

    private final ProgressRepository    progressRepository;
    private final CertificateRepository certificateRepository;
    private final RestTemplate          restTemplate;

    @Value("${app.enrollment-service.url}") private String enrollmentServiceUrl;

    // ── Track Progress ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProgressDto.ProgressResponse trackProgress(Long studentId, ProgressDto.TrackRequest req) {
        Progress progress = progressRepository
                .findByStudentIdAndLessonId(studentId, req.getLessonId())
                .orElseGet(() -> Progress.builder()
                        .studentId(studentId).courseId(req.getCourseId())
                        .lessonId(req.getLessonId()).watchedSeconds(0).isCompleted(false).build());

        if (req.getWatchedSeconds() != null && req.getWatchedSeconds() > progress.getWatchedSeconds()) {
            progress.setWatchedSeconds(req.getWatchedSeconds());
        }
        if (Boolean.TRUE.equals(req.getMarkComplete()) && !progress.getIsCompleted()) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
        }

        Progress saved = progressRepository.save(progress);
        log.debug("Progress tracked: student={} lesson={} watched={}s completed={}",
                studentId, req.getLessonId(), saved.getWatchedSeconds(), saved.getIsCompleted());

        // Sync progress percent to enrollment-service
        syncEnrollmentProgress(studentId, req.getCourseId());
        return toProgressResponse(saved);
    }

    // ── Mark Lesson Complete ──────────────────────────────────────────────────

    @Override
    @Transactional
    public ProgressDto.ProgressResponse markLessonComplete(Long studentId, Long lessonId, Long courseId) {
        Progress progress = progressRepository.findByStudentIdAndLessonId(studentId, lessonId)
                .orElseGet(() -> Progress.builder()
                        .studentId(studentId).courseId(courseId).lessonId(lessonId)
                        .watchedSeconds(0).isCompleted(false).build());

        progress.setIsCompleted(true);
        progress.setCompletedAt(LocalDateTime.now());
        Progress saved = progressRepository.save(progress);
        syncEnrollmentProgress(studentId, courseId);
        return toProgressResponse(saved);
    }

    // ── Get Lesson Progress ───────────────────────────────────────────────────

    @Override
    public ProgressDto.ProgressResponse getLessonProgress(Long studentId, Long lessonId) {
        return progressRepository.findByStudentIdAndLessonId(studentId, lessonId)
                .map(this::toProgressResponse)
                .orElseThrow(() -> new ProgressNotFoundException(
                        "No progress found for student=" + studentId + " lesson=" + lessonId));
    }

    // ── Get Course Progress ───────────────────────────────────────────────────

    @Override
    public ProgressDto.CourseProgressResponse getCourseProgress(Long studentId, Long courseId, long totalLessons) {
        List<Progress> list = progressRepository.findByStudentIdAndCourseId(studentId, courseId);
        long completed = list.stream().filter(Progress::getIsCompleted).count();
        double pct = totalLessons > 0 ? Math.round((completed * 100.0 / totalLessons) * 10.0) / 10.0 : 0.0;
        return ProgressDto.CourseProgressResponse.builder()
                .studentId(studentId).courseId(courseId)
                .completionPercent(pct).completedLessons(completed).totalLessons(totalLessons)
                .lessonProgresses(list.stream().map(this::toProgressResponse).collect(Collectors.toList()))
                .build();
    }

    // ── Get All Progress By Student ───────────────────────────────────────────

    @Override
    public List<ProgressDto.ProgressResponse> getAllProgressByStudent(Long studentId) {
        return progressRepository.findByStudentId(studentId)
                .stream().map(this::toProgressResponse).collect(Collectors.toList());
    }

    // ── Issue Certificate ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProgressDto.CertificateResponse issueCertificate(Long studentId, ProgressDto.IssueCertificateRequest req) {
        if (certificateRepository.existsByStudentIdAndCourseId(studentId, req.getCourseId())) {
            return toCertResponse(certificateRepository
                    .findByStudentIdAndCourseId(studentId, req.getCourseId()).orElseThrow());
        }
        Certificate cert = Certificate.builder()
                .studentId(studentId).courseId(req.getCourseId())
                .courseName(req.getCourseName()).studentName(req.getStudentName())
                .instructorName(req.getInstructorName())
                .certificateUrl("/certificates/" + studentId + "/" + req.getCourseId() + ".pdf")
                .build();
        Certificate saved = certificateRepository.save(cert);
        log.info("Certificate issued: student={} course={} code={}", studentId, req.getCourseId(), saved.getVerificationCode());
        return toCertResponse(saved);
    }

    // ── Get Certificate ───────────────────────────────────────────────────────

    @Override
    public ProgressDto.CertificateResponse getCertificate(Long studentId, Long courseId) {
        return toCertResponse(certificateRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new CertificateNotFoundException(
                        "Certificate not found for student=" + studentId + " course=" + courseId)));
    }

    // ── Get Certificates By Student ───────────────────────────────────────────

    @Override
    public List<ProgressDto.CertificateResponse> getCertificatesByStudent(Long studentId) {
        return certificateRepository.findByStudentId(studentId)
                .stream().map(this::toCertResponse).collect(Collectors.toList());
    }

    // ── Verify Certificate ────────────────────────────────────────────────────

    @Override
    public ProgressDto.VerifyResponse verifyCertificate(String verificationCode) {
        return certificateRepository.findByVerificationCode(verificationCode)
                .map(c -> ProgressDto.VerifyResponse.builder().valid(true)
                        .courseName(c.getCourseName()).studentName(c.getStudentName())
                        .instructorName(c.getInstructorName()).issuedAt(c.getIssuedAt()).build())
                .orElse(ProgressDto.VerifyResponse.builder().valid(false).build());
    }

    // ── Sync progress percentage to enrollment-service ────────────────────────

    private void syncEnrollmentProgress(Long studentId, Long courseId) {
        try {
            // Fetch total lesson count from lesson-service would be ideal;
            // Here we use completed count to compute a relative progress.
            // A full implementation would call lesson-service for total.
            long completed = progressRepository.countCompletedByStudentIdAndCourseId(studentId, courseId);
            long total = progressRepository.findByStudentIdAndCourseId(studentId, courseId).size();
            if (total == 0) return;
            double pct = Math.min(100.0, (completed * 100.0 / total));
            String url = enrollmentServiceUrl + "/api/v1/enrollments/" + courseId
                    + "/progress?studentId=" + studentId;
            restTemplate.put(url, java.util.Map.of("progressPercent", pct));
        } catch (Exception e) {
            log.warn("Could not sync progress to enrollment-service: {}", e.getMessage());
        }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private ProgressDto.ProgressResponse toProgressResponse(Progress p) {
        return ProgressDto.ProgressResponse.builder()
                .progressId(p.getProgressId()).studentId(p.getStudentId()).courseId(p.getCourseId())
                .lessonId(p.getLessonId()).watchedSeconds(p.getWatchedSeconds())
                .isCompleted(p.getIsCompleted()).completedAt(p.getCompletedAt())
                .lastAccessedAt(p.getLastAccessedAt()).build();
    }

    private ProgressDto.CertificateResponse toCertResponse(Certificate c) {
        return ProgressDto.CertificateResponse.builder()
                .certificateId(c.getCertificateId()).studentId(c.getStudentId()).courseId(c.getCourseId())
                .courseName(c.getCourseName()).studentName(c.getStudentName())
                .instructorName(c.getInstructorName()).verificationCode(c.getVerificationCode())
                .certificateUrl(c.getCertificateUrl()).issuedAt(c.getIssuedAt()).build();
    }
}
