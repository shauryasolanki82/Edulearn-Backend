package com.edulearn.enrollment.service;

import com.edulearn.enrollment.dto.EnrollmentDto;
import com.edulearn.enrollment.entity.Enrollment;
import com.edulearn.enrollment.exception.AlreadyEnrolledException;
import com.edulearn.enrollment.exception.EnrollmentNotFoundException;
import com.edulearn.enrollment.repository.EnrollmentRepository;
import com.edulearn.enrollment.service.impl.EnrollmentServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceImplTest {

    @Mock EnrollmentRepository enrollmentRepository;
    @Mock RestTemplate restTemplate;
    @InjectMocks EnrollmentServiceImpl enrollmentService;

    private Enrollment sample;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(enrollmentService, "courseServiceUrl", "http://localhost:8082");
        ReflectionTestUtils.setField(enrollmentService, "progressServiceUrl", "http://localhost:8086");
        sample = Enrollment.builder()
                .enrollmentId(1L).studentId(10L).courseId(20L)
                .status(Enrollment.EnrollmentStatus.ACTIVE)
                .progressPercent(0.0).certificateIssued(false).build();
    }

    @Test @DisplayName("enroll — creates enrollment and returns response")
    void enroll_success() {
        when(enrollmentRepository.existsByStudentIdAndCourseId(10L, 20L)).thenReturn(false);
        when(enrollmentRepository.save(any())).thenReturn(sample);
        EnrollmentDto.EnrollmentResponse r = enrollmentService.enroll(10L, 20L);
        assertThat(r.getStudentId()).isEqualTo(10L);
        assertThat(r.getCourseId()).isEqualTo(20L);
        assertThat(r.getStatus()).isEqualTo("ACTIVE");
    }

    @Test @DisplayName("enroll — duplicate throws AlreadyEnrolledException")
    void enroll_duplicate() {
        when(enrollmentRepository.existsByStudentIdAndCourseId(10L, 20L)).thenReturn(true);
        assertThatThrownBy(() -> enrollmentService.enroll(10L, 20L))
                .isInstanceOf(AlreadyEnrolledException.class);
    }

    @Test @DisplayName("unenroll — sets status CANCELLED")
    void unenroll_success() {
        when(enrollmentRepository.findByStudentIdAndCourseId(10L, 20L)).thenReturn(Optional.of(sample));
        when(enrollmentRepository.save(any())).thenReturn(sample);
        enrollmentService.unenroll(10L, 20L);
        assertThat(sample.getStatus()).isEqualTo(Enrollment.EnrollmentStatus.CANCELLED);
    }

    @Test @DisplayName("unenroll — not enrolled throws EnrollmentNotFoundException")
    void unenroll_notFound() {
        when(enrollmentRepository.findByStudentIdAndCourseId(10L, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> enrollmentService.unenroll(10L, 99L))
                .isInstanceOf(EnrollmentNotFoundException.class);
    }

    @Test @DisplayName("isEnrolled — returns true for enrolled student")
    void isEnrolled_true() {
        when(enrollmentRepository.findByStudentIdAndCourseId(10L, 20L)).thenReturn(Optional.of(sample));
        EnrollmentDto.IsEnrolledResponse r = enrollmentService.isEnrolled(10L, 20L);
        assertThat(r.isEnrolled()).isTrue();
        assertThat(r.getStatus()).isEqualTo("ACTIVE");
    }

    @Test @DisplayName("isEnrolled — returns false for non-enrolled student")
    void isEnrolled_false() {
        when(enrollmentRepository.findByStudentIdAndCourseId(10L, 99L)).thenReturn(Optional.empty());
        EnrollmentDto.IsEnrolledResponse r = enrollmentService.isEnrolled(10L, 99L);
        assertThat(r.isEnrolled()).isFalse();
    }

    @Test @DisplayName("updateProgress — clamps to 100 and auto-completes")
    void updateProgress_autoComplete() {
        when(enrollmentRepository.findByStudentIdAndCourseId(10L, 20L)).thenReturn(Optional.of(sample));
        when(enrollmentRepository.save(any())).thenReturn(sample);
        enrollmentService.updateProgress(10L, 20L, 100.0);
        assertThat(sample.getStatus()).isEqualTo(Enrollment.EnrollmentStatus.COMPLETED);
        assertThat(sample.getCompletedAt()).isNotNull();
    }

    @Test @DisplayName("updateProgress — partial progress stays ACTIVE")
    void updateProgress_partial() {
        when(enrollmentRepository.findByStudentIdAndCourseId(10L, 20L)).thenReturn(Optional.of(sample));
        when(enrollmentRepository.save(any())).thenReturn(sample);
        enrollmentService.updateProgress(10L, 20L, 55.0);
        assertThat(sample.getProgressPercent()).isEqualTo(55.0);
        assertThat(sample.getStatus()).isEqualTo(Enrollment.EnrollmentStatus.ACTIVE);
    }

    @Test @DisplayName("issueCertificate — fails when progress < 100")
    void issueCertificate_notComplete() {
        sample.setProgressPercent(80.0);
        when(enrollmentRepository.findByStudentIdAndCourseId(10L, 20L)).thenReturn(Optional.of(sample));
        assertThatThrownBy(() -> enrollmentService.issueCertificate(10L, 20L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test @DisplayName("issueCertificate — succeeds when progress = 100")
    void issueCertificate_success() {
        sample.setProgressPercent(100.0);
        when(enrollmentRepository.findByStudentIdAndCourseId(10L, 20L)).thenReturn(Optional.of(sample));
        when(enrollmentRepository.save(any())).thenReturn(sample);
        enrollmentService.issueCertificate(10L, 20L);
        assertThat(sample.getCertificateIssued()).isTrue();
    }

    @Test @DisplayName("getEnrollmentsByStudent — returns list")
    void getByStudent() {
        when(enrollmentRepository.findByStudentId(10L)).thenReturn(List.of(sample));
        assertThat(enrollmentService.getEnrollmentsByStudent(10L)).hasSize(1);
    }

    @Test @DisplayName("getEnrollmentCount — returns count")
    void getCount() {
        when(enrollmentRepository.countByCourseId(20L)).thenReturn(42L);
        assertThat(enrollmentService.getEnrollmentCount(20L)).isEqualTo(42L);
    }
}
