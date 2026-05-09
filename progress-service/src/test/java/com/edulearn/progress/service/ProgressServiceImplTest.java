package com.edulearn.progress.service;

import com.edulearn.progress.dto.ProgressDto;
import com.edulearn.progress.entity.Certificate;
import com.edulearn.progress.entity.Progress;
import com.edulearn.progress.exception.CertificateNotFoundException;
import com.edulearn.progress.exception.ProgressNotFoundException;
import com.edulearn.progress.repository.CertificateRepository;
import com.edulearn.progress.repository.ProgressRepository;
import com.edulearn.progress.service.impl.ProgressServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceImplTest {

    @Mock ProgressRepository progressRepository;
    @Mock CertificateRepository certificateRepository;
    @Mock RestTemplate restTemplate;
    @InjectMocks ProgressServiceImpl progressService;

    private Progress sampleProgress;
    private Certificate sampleCert;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(progressService, "enrollmentServiceUrl", "http://localhost:8084");

        sampleProgress = Progress.builder()
                .progressId(1L).studentId(10L).courseId(20L).lessonId(30L)
                .watchedSeconds(120).isCompleted(false).build();

        sampleCert = Certificate.builder()
                .certificateId(1L).studentId(10L).courseId(20L)
                .courseName("Spring Boot Mastery").studentName("Alice Smith")
                .instructorName("John Doe").verificationCode("uuid-test-1234")
                .certificateUrl("/certs/10/20.pdf").build();
    }

    // ── trackProgress ─────────────────────────────────────────────────────────

    @Test @DisplayName("trackProgress — creates new progress record for first access")
    void trackProgress_newRecord() {
        when(progressRepository.findByStudentIdAndLessonId(10L, 30L)).thenReturn(Optional.empty());
        when(progressRepository.save(any())).thenReturn(sampleProgress);
        when(progressRepository.findByStudentIdAndCourseId(10L, 20L)).thenReturn(List.of(sampleProgress));
        when(progressRepository.countCompletedByStudentIdAndCourseId(10L, 20L)).thenReturn(0L);

        ProgressDto.TrackRequest req = ProgressDto.TrackRequest.builder()
                .lessonId(30L).courseId(20L).watchedSeconds(120).markComplete(false).build();

        ProgressDto.ProgressResponse resp = progressService.trackProgress(10L, req);
        assertThat(resp.getLessonId()).isEqualTo(30L);
        assertThat(resp.getWatchedSeconds()).isEqualTo(120);
        verify(progressRepository).save(any());
    }

    @Test @DisplayName("trackProgress — updates watchedSeconds if new value is higher")
    void trackProgress_updateWatchTime() {
        sampleProgress.setWatchedSeconds(60);
        when(progressRepository.findByStudentIdAndLessonId(10L, 30L)).thenReturn(Optional.of(sampleProgress));
        when(progressRepository.save(any())).thenReturn(sampleProgress);
        when(progressRepository.findByStudentIdAndCourseId(10L, 20L)).thenReturn(List.of(sampleProgress));
        when(progressRepository.countCompletedByStudentIdAndCourseId(10L, 20L)).thenReturn(0L);

        ProgressDto.TrackRequest req = ProgressDto.TrackRequest.builder()
                .lessonId(30L).courseId(20L).watchedSeconds(200).markComplete(false).build();

        progressService.trackProgress(10L, req);
        assertThat(sampleProgress.getWatchedSeconds()).isEqualTo(200);
    }

    @Test @DisplayName("trackProgress — does not decrease watchedSeconds")
    void trackProgress_noDecrease() {
        sampleProgress.setWatchedSeconds(300);
        when(progressRepository.findByStudentIdAndLessonId(10L, 30L)).thenReturn(Optional.of(sampleProgress));
        when(progressRepository.save(any())).thenReturn(sampleProgress);
        when(progressRepository.findByStudentIdAndCourseId(10L, 20L)).thenReturn(List.of(sampleProgress));
        when(progressRepository.countCompletedByStudentIdAndCourseId(10L, 20L)).thenReturn(0L);

        ProgressDto.TrackRequest req = ProgressDto.TrackRequest.builder()
                .lessonId(30L).courseId(20L).watchedSeconds(100).markComplete(false).build();

        progressService.trackProgress(10L, req);
        assertThat(sampleProgress.getWatchedSeconds()).isEqualTo(300); // unchanged
    }

    @Test @DisplayName("trackProgress — markComplete=true sets isCompleted and completedAt")
    void trackProgress_markComplete() {
        when(progressRepository.findByStudentIdAndLessonId(10L, 30L)).thenReturn(Optional.of(sampleProgress));
        when(progressRepository.save(any())).thenReturn(sampleProgress);
        when(progressRepository.findByStudentIdAndCourseId(10L, 20L)).thenReturn(List.of(sampleProgress));
        when(progressRepository.countCompletedByStudentIdAndCourseId(10L, 20L)).thenReturn(1L);

        ProgressDto.TrackRequest req = ProgressDto.TrackRequest.builder()
                .lessonId(30L).courseId(20L).watchedSeconds(450).markComplete(true).build();

        progressService.trackProgress(10L, req);
        assertThat(sampleProgress.getIsCompleted()).isTrue();
        assertThat(sampleProgress.getCompletedAt()).isNotNull();
    }

    // ── markLessonComplete ────────────────────────────────────────────────────

    @Test @DisplayName("markLessonComplete — marks existing record complete")
    void markLessonComplete_existing() {
        when(progressRepository.findByStudentIdAndLessonId(10L, 30L)).thenReturn(Optional.of(sampleProgress));
        when(progressRepository.save(any())).thenReturn(sampleProgress);
        when(progressRepository.findByStudentIdAndCourseId(10L, 20L)).thenReturn(List.of(sampleProgress));
        when(progressRepository.countCompletedByStudentIdAndCourseId(10L, 20L)).thenReturn(1L);

        progressService.markLessonComplete(10L, 30L, 20L);
        assertThat(sampleProgress.getIsCompleted()).isTrue();
    }

    @Test @DisplayName("markLessonComplete — creates new record if none exists")
    void markLessonComplete_newRecord() {
        when(progressRepository.findByStudentIdAndLessonId(10L, 30L)).thenReturn(Optional.empty());
        when(progressRepository.save(any())).thenReturn(sampleProgress);
        when(progressRepository.findByStudentIdAndCourseId(10L, 20L)).thenReturn(List.of(sampleProgress));
        when(progressRepository.countCompletedByStudentIdAndCourseId(10L, 20L)).thenReturn(1L);

        progressService.markLessonComplete(10L, 30L, 20L);
        verify(progressRepository).save(any());
    }

    // ── getLessonProgress ─────────────────────────────────────────────────────

    @Test @DisplayName("getLessonProgress — found returns progress response")
    void getLessonProgress_found() {
        when(progressRepository.findByStudentIdAndLessonId(10L, 30L)).thenReturn(Optional.of(sampleProgress));
        ProgressDto.ProgressResponse resp = progressService.getLessonProgress(10L, 30L);
        assertThat(resp.getLessonId()).isEqualTo(30L);
        assertThat(resp.getWatchedSeconds()).isEqualTo(120);
    }

    @Test @DisplayName("getLessonProgress — not found throws ProgressNotFoundException")
    void getLessonProgress_notFound() {
        when(progressRepository.findByStudentIdAndLessonId(10L, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> progressService.getLessonProgress(10L, 99L))
                .isInstanceOf(ProgressNotFoundException.class);
    }

    // ── getCourseProgress ─────────────────────────────────────────────────────

    @Test @DisplayName("getCourseProgress — computes correct completion percentage")
    void getCourseProgress_percent() {
        Progress completed1 = Progress.builder().progressId(2L).studentId(10L)
                .courseId(20L).lessonId(31L).isCompleted(true)
                .completedAt(LocalDateTime.now()).watchedSeconds(300).build();
        Progress completed2 = Progress.builder().progressId(3L).studentId(10L)
                .courseId(20L).lessonId(32L).isCompleted(true)
                .completedAt(LocalDateTime.now()).watchedSeconds(250).build();

        when(progressRepository.findByStudentIdAndCourseId(10L, 20L))
                .thenReturn(List.of(sampleProgress, completed1, completed2));

        ProgressDto.CourseProgressResponse resp = progressService.getCourseProgress(10L, 20L, 3);
        assertThat(resp.getCompletedLessons()).isEqualTo(2);
        assertThat(resp.getTotalLessons()).isEqualTo(3);
        assertThat(resp.getCompletionPercent()).isEqualTo(66.7);
    }

    @Test @DisplayName("getCourseProgress — 0 total lessons returns 0%")
    void getCourseProgress_noLessons() {
        when(progressRepository.findByStudentIdAndCourseId(10L, 20L)).thenReturn(List.of());
        ProgressDto.CourseProgressResponse resp = progressService.getCourseProgress(10L, 20L, 0);
        assertThat(resp.getCompletionPercent()).isEqualTo(0.0);
    }

    // ── issueCertificate ──────────────────────────────────────────────────────

    @Test @DisplayName("issueCertificate — issues new certificate with unique verification code")
    void issueCertificate_success() {
        when(certificateRepository.existsByStudentIdAndCourseId(10L, 20L)).thenReturn(false);
        when(certificateRepository.save(any())).thenReturn(sampleCert);

        ProgressDto.IssueCertificateRequest req = ProgressDto.IssueCertificateRequest.builder()
                .courseId(20L).courseName("Spring Boot Mastery")
                .studentName("Alice Smith").instructorName("John Doe").build();

        ProgressDto.CertificateResponse resp = progressService.issueCertificate(10L, req);
        assertThat(resp.getCourseName()).isEqualTo("Spring Boot Mastery");
        assertThat(resp.getVerificationCode()).isEqualTo("uuid-test-1234");
        verify(certificateRepository).save(any());
    }

    @Test @DisplayName("issueCertificate — returns existing cert if already issued")
    void issueCertificate_alreadyIssued() {
        when(certificateRepository.existsByStudentIdAndCourseId(10L, 20L)).thenReturn(true);
        when(certificateRepository.findByStudentIdAndCourseId(10L, 20L)).thenReturn(Optional.of(sampleCert));

        ProgressDto.IssueCertificateRequest req = ProgressDto.IssueCertificateRequest.builder()
                .courseId(20L).courseName("Spring Boot Mastery")
                .studentName("Alice Smith").instructorName("John Doe").build();

        ProgressDto.CertificateResponse resp = progressService.issueCertificate(10L, req);
        assertThat(resp.getVerificationCode()).isEqualTo("uuid-test-1234");
        verify(certificateRepository, never()).save(any());
    }

    // ── verifyCertificate ─────────────────────────────────────────────────────

    @Test @DisplayName("verifyCertificate — valid code returns valid=true with details")
    void verifyCertificate_valid() {
        when(certificateRepository.findByVerificationCode("uuid-test-1234"))
                .thenReturn(Optional.of(sampleCert));

        ProgressDto.VerifyResponse resp = progressService.verifyCertificate("uuid-test-1234");
        assertThat(resp.isValid()).isTrue();
        assertThat(resp.getCourseName()).isEqualTo("Spring Boot Mastery");
        assertThat(resp.getStudentName()).isEqualTo("Alice Smith");
    }

    @Test @DisplayName("verifyCertificate — invalid code returns valid=false")
    void verifyCertificate_invalid() {
        when(certificateRepository.findByVerificationCode("bad-code")).thenReturn(Optional.empty());
        ProgressDto.VerifyResponse resp = progressService.verifyCertificate("bad-code");
        assertThat(resp.isValid()).isFalse();
    }

    // ── getCertificate ────────────────────────────────────────────────────────

    @Test @DisplayName("getCertificate — found returns certificate response")
    void getCertificate_found() {
        when(certificateRepository.findByStudentIdAndCourseId(10L, 20L)).thenReturn(Optional.of(sampleCert));
        ProgressDto.CertificateResponse resp = progressService.getCertificate(10L, 20L);
        assertThat(resp.getVerificationCode()).isEqualTo("uuid-test-1234");
    }

    @Test @DisplayName("getCertificate — not found throws CertificateNotFoundException")
    void getCertificate_notFound() {
        when(certificateRepository.findByStudentIdAndCourseId(10L, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> progressService.getCertificate(10L, 99L))
                .isInstanceOf(CertificateNotFoundException.class);
    }

    // ── getCertificatesByStudent ──────────────────────────────────────────────

    @Test @DisplayName("getCertificatesByStudent — returns all student certificates")
    void getCertificatesByStudent() {
        when(certificateRepository.findByStudentId(10L)).thenReturn(List.of(sampleCert));
        List<ProgressDto.CertificateResponse> list = progressService.getCertificatesByStudent(10L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getCourseName()).isEqualTo("Spring Boot Mastery");
    }
}
