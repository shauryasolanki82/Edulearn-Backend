package com.edulearn.lesson.service;

import com.edulearn.lesson.dto.LessonDto;
import com.edulearn.lesson.entity.Lesson;
import com.edulearn.lesson.entity.Resource;
import com.edulearn.lesson.exception.ForbiddenException;
import com.edulearn.lesson.exception.LessonNotFoundException;
import com.edulearn.lesson.exception.ResourceNotFoundException;
import com.edulearn.lesson.repository.LessonRepository;
import com.edulearn.lesson.repository.ResourceRepository;
import com.edulearn.lesson.service.impl.LessonServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonServiceImplTest {

    @Mock LessonRepository lessonRepository;
    @Mock ResourceRepository resourceRepository;
    @Mock RestTemplate restTemplate;

    @InjectMocks LessonServiceImpl lessonService;

    private Lesson sampleLesson;
    private Resource sampleResource;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(lessonService, "courseServiceUrl", "http://localhost:8082");

        sampleLesson = Lesson.builder()
                .lessonId(1L)
                .courseId(10L)
                .instructorId(5L)
                .title("Introduction to Spring")
                .contentType(Lesson.ContentType.VIDEO)
                .contentUrl("https://cdn.example.com/video1.mp4")
                .durationMinutes(45)
                .orderIndex(1)
                .isPreview(false)
                .resources(new ArrayList<>())
                .build();

        sampleResource = Resource.builder()
                .resourceId(1L)
                .lesson(sampleLesson)
                .name("Slide Deck")
                .fileUrl("https://cdn.example.com/slides.pdf")
                .fileType(Resource.FileType.PDF)
                .sizeKb(512L)
                .build();
    }

    // ── addLesson ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addLesson — admin adds lesson to any course")
    void addLesson_adminSuccess() {
        when(lessonRepository.maxOrderIndexByCourseId(10L)).thenReturn(0);
        when(lessonRepository.save(any(Lesson.class))).thenReturn(sampleLesson);

        LessonDto.LessonRequest req = LessonDto.LessonRequest.builder()
                .title("Introduction to Spring")
                .contentType(Lesson.ContentType.VIDEO)
                .durationMinutes(45)
                .isPreview(false)
                .build();

        LessonDto.LessonResponse resp = lessonService.addLesson(10L, 1L, "ADMIN", req);

        assertThat(resp.getTitle()).isEqualTo("Introduction to Spring");
        assertThat(resp.getCourseId()).isEqualTo(10L);
        verify(lessonRepository).save(any(Lesson.class));
    }

    @Test
    @DisplayName("addLesson — instructor adds first lesson (no existing lessons)")
    void addLesson_instructorFirstLesson() {
        when(lessonRepository.countByCourseId(10L)).thenReturn(0L);
        when(lessonRepository.maxOrderIndexByCourseId(10L)).thenReturn(0);
        when(lessonRepository.save(any(Lesson.class))).thenReturn(sampleLesson);

        LessonDto.LessonRequest req = LessonDto.LessonRequest.builder()
                .title("Intro")
                .contentType(Lesson.ContentType.VIDEO)
                .durationMinutes(30)
                .isPreview(true)
                .build();

        LessonDto.LessonResponse resp = lessonService.addLesson(10L, 5L, "INSTRUCTOR", req);
        assertThat(resp).isNotNull();
    }

    @Test
    @DisplayName("addLesson — instructor who doesn't own course is forbidden")
    void addLesson_instructorNotOwner_forbidden() {
        when(lessonRepository.countByCourseId(10L)).thenReturn(3L);
        when(lessonRepository.existsByCourseIdAndInstructorId(10L, 99L)).thenReturn(false);

        LessonDto.LessonRequest req = LessonDto.LessonRequest.builder()
                .title("Hacked Lesson")
                .contentType(Lesson.ContentType.ARTICLE)
                .durationMinutes(10)
                .build();

        assertThatThrownBy(() -> lessonService.addLesson(10L, 99L, "INSTRUCTOR", req))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── getLessonsByCourse ────────────────────────────────────────────────────

    @Test
    @DisplayName("getLessonsByCourse — returns ordered lesson summaries")
    void getLessonsByCourse_success() {
        when(lessonRepository.findByCourseIdOrderByOrderIndex(10L))
                .thenReturn(List.of(sampleLesson));
        when(resourceRepository.countByLesson_LessonId(1L)).thenReturn(0L);

        List<LessonDto.LessonSummary> list = lessonService.getLessonsByCourse(10L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTitle()).isEqualTo("Introduction to Spring");
        assertThat(list.get(0).getOrderIndex()).isEqualTo(1);
    }

    // ── getLessonById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getLessonById — returns full lesson response")
    void getLessonById_success() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(sampleLesson));
        LessonDto.LessonResponse resp = lessonService.getLessonById(1L, 5L, "INSTRUCTOR");
        assertThat(resp.getLessonId()).isEqualTo(1L);
        assertThat(resp.getContentUrl()).isEqualTo("https://cdn.example.com/video1.mp4");
    }

    @Test
    @DisplayName("getLessonById — not found throws LessonNotFoundException")
    void getLessonById_notFound() {
        when(lessonRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> lessonService.getLessonById(99L, 5L, "INSTRUCTOR"))
                .isInstanceOf(LessonNotFoundException.class);
    }

    // ── getPreviewLesson ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getPreviewLesson — preview=true returns lesson")
    void getPreviewLesson_success() {
        sampleLesson.setIsPreview(true);
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(sampleLesson));
        LessonDto.LessonResponse resp = lessonService.getPreviewLesson(1L);
        assertThat(resp.getIsPreview()).isTrue();
    }

    @Test
    @DisplayName("getPreviewLesson — preview=false throws ForbiddenException")
    void getPreviewLesson_notPreview() {
        sampleLesson.setIsPreview(false);
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(sampleLesson));
        assertThatThrownBy(() -> lessonService.getPreviewLesson(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── updateLesson ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateLesson — owner updates title and duration")
    void updateLesson_ownerSuccess() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(sampleLesson));
        when(lessonRepository.countByCourseId(10L)).thenReturn(1L);
        when(lessonRepository.existsByCourseIdAndInstructorId(10L, 5L)).thenReturn(true);
        when(lessonRepository.save(any())).thenReturn(sampleLesson);

        LessonDto.LessonRequest req = LessonDto.LessonRequest.builder()
                .title("Updated Title")
                .contentType(Lesson.ContentType.VIDEO)
                .durationMinutes(60)
                .build();

        LessonDto.LessonResponse resp = lessonService.updateLesson(1L, 5L, "INSTRUCTOR", req);
        assertThat(sampleLesson.getTitle()).isEqualTo("Updated Title");
        assertThat(sampleLesson.getDurationMinutes()).isEqualTo(60);
    }

    @Test
    @DisplayName("updateLesson — non-owner throws ForbiddenException")
    void updateLesson_nonOwner_forbidden() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(sampleLesson));
        when(lessonRepository.countByCourseId(10L)).thenReturn(1L);
        when(lessonRepository.existsByCourseIdAndInstructorId(10L, 99L)).thenReturn(false);

        LessonDto.LessonRequest req = LessonDto.LessonRequest.builder()
                .title("Hijacked").contentType(Lesson.ContentType.ARTICLE).build();

        assertThatThrownBy(() -> lessonService.updateLesson(1L, 99L, "INSTRUCTOR", req))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── deleteLesson ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteLesson — owner deletes and decrements order")
    void deleteLesson_ownerSuccess() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(sampleLesson));
        when(lessonRepository.countByCourseId(10L)).thenReturn(1L);
        when(lessonRepository.existsByCourseIdAndInstructorId(10L, 5L)).thenReturn(true);

        lessonService.deleteLesson(1L, 5L, "INSTRUCTOR");

        verify(lessonRepository).delete(sampleLesson);
        verify(lessonRepository).decrementOrderIndexAfter(10L, 1);
    }

    @Test
    @DisplayName("deleteLesson — admin deletes any lesson")
    void deleteLesson_adminSuccess() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(sampleLesson));

        lessonService.deleteLesson(1L, 999L, "ADMIN");

        verify(lessonRepository).delete(sampleLesson);
    }

    // ── reorderLessons ────────────────────────────────────────────────────────

    @Test
    @DisplayName("reorderLessons — admin reorders successfully")
    void reorderLessons_adminSuccess() {
        Lesson l2 = Lesson.builder().lessonId(2L).courseId(10L).instructorId(5L)
                .title("Lesson 2").contentType(Lesson.ContentType.VIDEO)
                .orderIndex(2).isPreview(false).resources(new ArrayList<>()).build();

        when(lessonRepository.findByCourseIdOrderByOrderIndex(10L))
                .thenReturn(List.of(sampleLesson, l2));
        when(lessonRepository.saveAll(any())).thenReturn(List.of(l2, sampleLesson));
        when(lessonRepository.findByCourseIdOrderByOrderIndex(10L))
                .thenReturn(List.of(l2, sampleLesson));
        when(resourceRepository.countByLesson_LessonId(any())).thenReturn(0L);

        LessonDto.ReorderRequest req = new LessonDto.ReorderRequest(List.of(2L, 1L));
        List<LessonDto.LessonSummary> result =
                lessonService.reorderLessons(10L, 999L, "ADMIN", req);

        verify(lessonRepository).saveAll(any());
    }

    // ── addResource ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("addResource — owner attaches resource to lesson")
    void addResource_success() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(sampleLesson));
        when(lessonRepository.countByCourseId(10L)).thenReturn(1L);
        when(lessonRepository.existsByCourseIdAndInstructorId(10L, 5L)).thenReturn(true);
        when(resourceRepository.save(any())).thenReturn(sampleResource);

        LessonDto.ResourceRequest req = LessonDto.ResourceRequest.builder()
                .name("Slide Deck")
                .fileUrl("https://cdn.example.com/slides.pdf")
                .fileType(Resource.FileType.PDF)
                .sizeKb(512L)
                .build();

        LessonDto.ResourceResponse resp = lessonService.addResource(1L, 5L, "INSTRUCTOR", req);
        assertThat(resp.getName()).isEqualTo("Slide Deck");
        assertThat(resp.getFileType()).isEqualTo("PDF");
    }

    // ── removeResource ────────────────────────────────────────────────────────

    @Test
    @DisplayName("removeResource — owner removes resource")
    void removeResource_success() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(sampleLesson));
        when(lessonRepository.countByCourseId(10L)).thenReturn(1L);
        when(lessonRepository.existsByCourseIdAndInstructorId(10L, 5L)).thenReturn(true);
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(sampleResource));

        lessonService.removeResource(1L, 1L, 5L, "INSTRUCTOR");
        verify(resourceRepository).delete(sampleResource);
    }

    @Test
    @DisplayName("removeResource — resource not found throws ResourceNotFoundException")
    void removeResource_notFound() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(sampleLesson));
        when(lessonRepository.countByCourseId(10L)).thenReturn(1L);
        when(lessonRepository.existsByCourseIdAndInstructorId(10L, 5L)).thenReturn(true);
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.removeResource(1L, 99L, 5L, "INSTRUCTOR"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getPreviewLessons ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getPreviewLessons — returns only preview lessons")
    void getPreviewLessons_success() {
        sampleLesson.setIsPreview(true);
        when(lessonRepository.findByCourseIdAndIsPreviewTrue(10L))
                .thenReturn(List.of(sampleLesson));
        when(resourceRepository.countByLesson_LessonId(1L)).thenReturn(0L);

        List<LessonDto.LessonSummary> list = lessonService.getPreviewLessons(10L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getIsPreview()).isTrue();
    }

    // ── countLessonsByCourse ──────────────────────────────────────────────────

    @Test
    @DisplayName("countLessonsByCourse — returns correct count")
    void countLessonsByCourse_success() {
        when(lessonRepository.countByCourseId(10L)).thenReturn(5L);
        assertThat(lessonService.countLessonsByCourse(10L)).isEqualTo(5L);
    }

    // ── getResourcesByLesson ──────────────────────────────────────────────────

    @Test
    @DisplayName("getResourcesByLesson — returns resources list")
    void getResourcesByLesson_success() {
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(sampleLesson));
        when(resourceRepository.findByLesson_LessonId(1L)).thenReturn(List.of(sampleResource));

        List<LessonDto.ResourceResponse> list = lessonService.getResourcesByLesson(1L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("Slide Deck");
    }
}
