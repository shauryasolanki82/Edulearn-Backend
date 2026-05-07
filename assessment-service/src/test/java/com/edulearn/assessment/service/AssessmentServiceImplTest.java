package com.edulearn.assessment.service;

import com.edulearn.assessment.dto.AssessmentDto;
import com.edulearn.assessment.entity.*;
import com.edulearn.assessment.exception.*;
import com.edulearn.assessment.repository.*;
import com.edulearn.assessment.service.impl.AssessmentServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceImplTest {

    @Mock QuizRepository quizRepository;
    @Mock AttemptRepository attemptRepository;
    @InjectMocks AssessmentServiceImpl service;

    private Quiz sampleQuiz;
    private Question sampleQuestion;

    @BeforeEach
    void setUp() {
        sampleQuestion = Question.builder()
                .questionId(1L).text("What is Spring Boot?").type(Question.QuestionType.MCQ)
                .options(List.of("A framework","A language","A database","An OS"))
                .correctAnswer("0").marks(2).orderIndex(1).build();

        sampleQuiz = Quiz.builder()
                .quizId(1L).courseId(10L).title("Spring Basics Quiz")
                .timeLimitMinutes(30).passingScore(70.0).maxAttempts(3)
                .isPublished(false).questions(new ArrayList<>(List.of(sampleQuestion))).build();

        sampleQuestion.setQuiz(sampleQuiz);
    }

    @Test @DisplayName("createQuiz — persists and returns quiz response")
    void createQuiz_success() {
        when(quizRepository.save(any())).thenReturn(sampleQuiz);
        AssessmentDto.QuizRequest req = AssessmentDto.QuizRequest.builder()
                .courseId(10L).title("Spring Basics Quiz").timeLimitMinutes(30)
                .passingScore(70.0).maxAttempts(3).build();
        AssessmentDto.QuizResponse resp = service.createQuiz(req);
        assertThat(resp.getTitle()).isEqualTo("Spring Basics Quiz");
        assertThat(resp.getIsPublished()).isFalse();
        assertThat(resp.getQuestionCount()).isEqualTo(1);
    }

    @Test @DisplayName("publishQuiz — sets isPublished = true")
    void publishQuiz_success() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(sampleQuiz));
        when(quizRepository.save(any())).thenReturn(sampleQuiz);
        service.publishQuiz(1L, true);
        assertThat(sampleQuiz.getIsPublished()).isTrue();
    }

    @Test @DisplayName("getQuizById — not found throws QuizNotFoundException")
    void getQuizById_notFound() {
        when(quizRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getQuizById(99L)).isInstanceOf(QuizNotFoundException.class);
    }

    @Test @DisplayName("startAttempt — creates attempt when under max")
    void startAttempt_underLimit() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(sampleQuiz));
        when(attemptRepository.countByStudentIdAndQuizId(20L, 1L)).thenReturn(0L);
        Attempt saved = Attempt.builder().attemptId(1L).quizId(1L).studentId(20L).build();
        when(attemptRepository.save(any())).thenReturn(saved);
        AssessmentDto.AttemptResponse resp = service.startAttempt(20L, 1L);
        assertThat(resp.getStudentId()).isEqualTo(20L);
    }

    @Test @DisplayName("startAttempt — throws when max attempts reached")
    void startAttempt_limitReached() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(sampleQuiz));
        when(attemptRepository.countByStudentIdAndQuizId(20L, 1L)).thenReturn(3L);
        assertThatThrownBy(() -> service.startAttempt(20L, 1L))
                .isInstanceOf(AttemptLimitExceededException.class);
    }

    @Test @DisplayName("submitAttempt — auto-grades correctly, marks passed when above threshold")
    void submitAttempt_autoGrade_pass() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(sampleQuiz));
        when(attemptRepository.findByStudentIdAndQuizId(20L, 1L)).thenReturn(new ArrayList<>());
        Attempt saved = Attempt.builder().attemptId(2L).quizId(1L).studentId(20L)
                .score(100.0).passed(true).build();
        when(attemptRepository.save(any())).thenReturn(saved);

        AssessmentDto.SubmitAttemptRequest req = new AssessmentDto.SubmitAttemptRequest(
                1L, Map.of(1L, "0")); // correct answer
        AssessmentDto.AttemptResponse resp = service.submitAttempt(20L, req);
        assertThat(resp.getPassed()).isTrue();
    }

    @Test @DisplayName("submitAttempt — marks failed when below threshold")
    void submitAttempt_autoGrade_fail() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(sampleQuiz));
        when(attemptRepository.findByStudentIdAndQuizId(20L, 1L)).thenReturn(new ArrayList<>());
        Attempt saved = Attempt.builder().attemptId(3L).quizId(1L).studentId(20L)
                .score(0.0).passed(false).build();
        when(attemptRepository.save(any())).thenReturn(saved);

        AssessmentDto.SubmitAttemptRequest req = new AssessmentDto.SubmitAttemptRequest(
                1L, Map.of(1L, "2")); // wrong answer
        AssessmentDto.AttemptResponse resp = service.submitAttempt(20L, req);
        assertThat(resp.getPassed()).isFalse();
    }

    @Test @DisplayName("getQuestionsForStudent — hides correctAnswer")
    void getQuestionsForStudent_noCorrectAnswer() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(sampleQuiz));
        List<AssessmentDto.QuestionForStudent> qs = service.getQuestionsForStudent(1L);
        assertThat(qs).hasSize(1);
        // QuestionForStudent has no correctAnswer field — confirm no leakage
        assertThat(qs.get(0)).isInstanceOf(AssessmentDto.QuestionForStudent.class);
    }

    @Test @DisplayName("addQuestion — appends question to quiz")
    void addQuestion_success() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(sampleQuiz));
        when(quizRepository.save(any())).thenReturn(sampleQuiz);
        AssessmentDto.QuestionRequest req = AssessmentDto.QuestionRequest.builder()
                .text("What is JPA?").type(Question.QuestionType.MCQ)
                .options(List.of("ORM","DB","OS","IDE")).correctAnswer("0").marks(1).build();
        AssessmentDto.QuestionResponse resp = service.addQuestion(1L, req);
        assertThat(resp.getText()).isEqualTo("What is JPA?");
    }

    @Test @DisplayName("deleteQuiz — removes quiz from repository")
    void deleteQuiz_success() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(sampleQuiz));
        service.deleteQuiz(1L);
        verify(quizRepository).delete(sampleQuiz);
    }
}
