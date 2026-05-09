package com.edulearn.course.service;

import com.edulearn.course.dto.CourseDto;
import com.edulearn.course.entity.Course;
import com.edulearn.course.exception.CourseNotFoundException;
import com.edulearn.course.exception.ForbiddenException;
import com.edulearn.course.repository.CourseRepository;
import com.edulearn.course.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock CourseRepository courseRepository;
    @InjectMocks CourseServiceImpl courseService;

    private Course sampleCourse;

    @BeforeEach
    void setUp() {
        sampleCourse = Course.builder()
                .courseId(1L)
                .title("Spring Boot Mastery")
                .description("Learn Spring Boot from scratch")
                .category(Course.Category.PROGRAMMING)
                .level(Course.Level.BEGINNER)
                .price(BigDecimal.valueOf(999))
                .instructorId(10L)
                .instructorName("John Doe")
                .language(Course.Language.ENGLISH)
                .isPublished(false)
                .isFeatured(false)
                .totalEnrollments(0)
                .totalDuration(0)
                .averageRating(0.0)
                .build();
    }

    // ── createCourse ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createCourse — saves and returns course response")
    void createCourse_success() {
        when(courseRepository.save(any(Course.class))).thenReturn(sampleCourse);

        CourseDto.CourseRequest req = CourseDto.CourseRequest.builder()
                .title("Spring Boot Mastery")
                .category(Course.Category.PROGRAMMING)
                .level(Course.Level.BEGINNER)
                .price(BigDecimal.valueOf(999))
                .build();

        CourseDto.CourseResponse resp = courseService.createCourse(10L, "John Doe", req);

        assertThat(resp.getTitle()).isEqualTo("Spring Boot Mastery");
        assertThat(resp.getInstructorId()).isEqualTo(10L);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    @DisplayName("createCourse — defaults isPublished to false")
    void createCourse_defaultsToUnpublished() {
        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        when(courseRepository.save(captor.capture())).thenReturn(sampleCourse);

        courseService.createCourse(10L, "John", CourseDto.CourseRequest.builder()
                .title("Test").category(Course.Category.DESIGN).level(Course.Level.ALL_LEVELS)
                .price(BigDecimal.ZERO).build());

        assertThat(captor.getValue().getIsPublished()).isFalse();
    }

    // ── getCourseById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getCourseById — found returns full response")
    void getCourseById_found() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        CourseDto.CourseResponse resp = courseService.getCourseById(1L);
        assertThat(resp.getCourseId()).isEqualTo(1L);
        assertThat(resp.getTitle()).isEqualTo("Spring Boot Mastery");
    }

    @Test
    @DisplayName("getCourseById — not found throws CourseNotFoundException")
    void getCourseById_notFound() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> courseService.getCourseById(99L))
                .isInstanceOf(CourseNotFoundException.class);
    }

    // ── getAllCourses ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllCourses — returns paged summary")
    void getAllCourses_success() {
        Page<Course> page = new PageImpl<>(List.of(sampleCourse),
                PageRequest.of(0, 12), 1);
        when(courseRepository.findByIsPublishedTrue(any(Pageable.class))).thenReturn(page);

        CourseDto.PagedResponse<CourseDto.CourseSummary> resp =
                courseService.getAllCourses(0, 12, "newest");

        assertThat(resp.getContent()).hasSize(1);
        assertThat(resp.getTotalElements()).isEqualTo(1);
    }

    // ── getCoursesByInstructor ────────────────────────────────────────────────

    @Test
    @DisplayName("getCoursesByInstructor — returns all instructor courses")
    void getCoursesByInstructor_success() {
        when(courseRepository.findByInstructorId(10L)).thenReturn(List.of(sampleCourse));
        List<CourseDto.CourseResponse> list = courseService.getCoursesByInstructor(10L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getInstructorId()).isEqualTo(10L);
    }

    // ── updateCourse ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateCourse — owner can update")
    void updateCourse_ownerSuccess() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.save(any())).thenReturn(sampleCourse);

        CourseDto.CourseRequest req = CourseDto.CourseRequest.builder()
                .title("Updated Title")
                .category(Course.Category.PROGRAMMING)
                .level(Course.Level.INTERMEDIATE)
                .price(BigDecimal.valueOf(1299))
                .build();

        CourseDto.CourseResponse resp = courseService.updateCourse(1L, 10L, false, req);
        assertThat(sampleCourse.getTitle()).isEqualTo("Updated Title");
    }

    @Test
    @DisplayName("updateCourse — non-owner throws ForbiddenException")
    void updateCourse_nonOwnerForbidden() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));

        CourseDto.CourseRequest req = CourseDto.CourseRequest.builder()
                .title("Hacked").category(Course.Category.DESIGN)
                .level(Course.Level.ALL_LEVELS).price(BigDecimal.ZERO).build();

        assertThatThrownBy(() -> courseService.updateCourse(1L, 99L, false, req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("updateCourse — admin can update any course")
    void updateCourse_adminSuccess() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.save(any())).thenReturn(sampleCourse);

        CourseDto.CourseRequest req = CourseDto.CourseRequest.builder()
                .title("Admin Updated").category(Course.Category.DEVOPS)
                .level(Course.Level.ADVANCED).price(BigDecimal.valueOf(500)).build();

        assertThatNoException().isThrownBy(() ->
                courseService.updateCourse(1L, 999L, true, req));
    }

    // ── publishCourse ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("publishCourse — sets isPublished to true")
    void publishCourse_success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.save(any())).thenReturn(sampleCourse);

        courseService.publishCourse(1L, 10L, false, true);
        assertThat(sampleCourse.getIsPublished()).isTrue();
    }

    @Test
    @DisplayName("publishCourse — non-owner throws ForbiddenException")
    void publishCourse_nonOwnerForbidden() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        assertThatThrownBy(() -> courseService.publishCourse(1L, 99L, false, true))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── deleteCourse ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteCourse — owner deletes successfully")
    void deleteCourse_ownerSuccess() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        courseService.deleteCourse(1L, 10L, false);
        verify(courseRepository).delete(sampleCourse);
    }

    @Test
    @DisplayName("deleteCourse — non-owner throws ForbiddenException")
    void deleteCourse_nonOwnerForbidden() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        assertThatThrownBy(() -> courseService.deleteCourse(1L, 88L, false))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── getFeaturedCourses ────────────────────────────────────────────────────

    @Test
    @DisplayName("getFeaturedCourses — returns featured and published courses")
    void getFeaturedCourses_success() {
        sampleCourse.setIsFeatured(true);
        sampleCourse.setIsPublished(true);
        when(courseRepository.findByIsFeaturedTrueAndIsPublishedTrue())
                .thenReturn(List.of(sampleCourse));

        List<CourseDto.CourseSummary> list = courseService.getFeaturedCourses();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getIsFeatured()).isTrue();
    }

    // ── updateEnrollmentCount ─────────────────────────────────────────────────

    @Test
    @DisplayName("updateEnrollmentCount — increments count by delta")
    void updateEnrollmentCount_increment() {
        sampleCourse.setTotalEnrollments(5);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.save(any())).thenReturn(sampleCourse);

        courseService.updateEnrollmentCount(1L, 1);
        assertThat(sampleCourse.getTotalEnrollments()).isEqualTo(6);
    }

    @Test
    @DisplayName("updateEnrollmentCount — does not go below zero")
    void updateEnrollmentCount_noNegative() {
        sampleCourse.setTotalEnrollments(0);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.save(any())).thenReturn(sampleCourse);

        courseService.updateEnrollmentCount(1L, -5);
        assertThat(sampleCourse.getTotalEnrollments()).isEqualTo(0);
    }

    // ── updateTotalDuration ───────────────────────────────────────────────────

    @Test
    @DisplayName("updateTotalDuration — sets total minutes on course")
    void updateTotalDuration_success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.save(any())).thenReturn(sampleCourse);

        courseService.updateTotalDuration(1L, 240);
        assertThat(sampleCourse.getTotalDuration()).isEqualTo(240);
    }

    // ── toggleFeatured ────────────────────────────────────────────────────────

    @Test
    @DisplayName("toggleFeatured — sets isFeatured flag")
    void toggleFeatured_success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.save(any())).thenReturn(sampleCourse);

        courseService.toggleFeatured(1L, true);
        assertThat(sampleCourse.getIsFeatured()).isTrue();
    }
}
