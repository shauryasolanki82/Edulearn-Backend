package com.edulearn.assessment.service.impl;

import com.edulearn.assessment.dto.AssessmentDto;
import com.edulearn.assessment.entity.Attempt;
import com.edulearn.assessment.entity.Question;
import com.edulearn.assessment.entity.Quiz;
import com.edulearn.assessment.exception.AttemptLimitExceededException;
import com.edulearn.assessment.exception.QuizNotFoundException;
import com.edulearn.assessment.exception.QuestionNotFoundException;
import com.edulearn.assessment.repository.AttemptRepository;
import com.edulearn.assessment.repository.QuizRepository;
import com.edulearn.assessment.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class AssessmentServiceImpl implements AssessmentService {

    private final QuizRepository    quizRepository;
    private final AttemptRepository attemptRepository;

    // ── Quiz CRUD ─────────────────────────────────────────────────────────────

    @Override @Transactional
    public AssessmentDto.QuizResponse createQuiz(AssessmentDto.QuizRequest req) {
        Quiz quiz = Quiz.builder()
                .courseId(req.getCourseId()).lessonId(req.getLessonId()).title(req.getTitle())
                .description(req.getDescription()).timeLimitMinutes(req.getTimeLimitMinutes())
                .passingScore(req.getPassingScore()).maxAttempts(req.getMaxAttempts())
                .isPublished(false).build();
        Quiz saved = quizRepository.save(quiz);
        log.info("Quiz created: id={} course={}", saved.getQuizId(), saved.getCourseId());
        return toQuizResponse(saved);
    }

    @Override
    public AssessmentDto.QuizResponse getQuizById(Long quizId) {
        return toQuizResponse(findQuizOrThrow(quizId));
    }

    @Override
    public List<AssessmentDto.QuizResponse> getQuizzesByCourse(Long courseId) {
        return quizRepository.findByCourseId(courseId).stream()
                .map(this::toQuizResponse).collect(Collectors.toList());
    }

    @Override @Transactional
    public AssessmentDto.QuizResponse updateQuiz(Long quizId, AssessmentDto.QuizRequest req) {
        Quiz quiz = findQuizOrThrow(quizId);
        if (req.getTitle() != null)         quiz.setTitle(req.getTitle());
        if (req.getDescription() != null)   quiz.setDescription(req.getDescription());
        if (req.getTimeLimitMinutes() != null) quiz.setTimeLimitMinutes(req.getTimeLimitMinutes());
        if (req.getPassingScore() != null)  quiz.setPassingScore(req.getPassingScore());
        if (req.getMaxAttempts() != null)   quiz.setMaxAttempts(req.getMaxAttempts());
        return toQuizResponse(quizRepository.save(quiz));
    }

    @Override @Transactional
    public AssessmentDto.QuizResponse publishQuiz(Long quizId, boolean publish) {
        Quiz quiz = findQuizOrThrow(quizId);
        quiz.setIsPublished(publish);
        return toQuizResponse(quizRepository.save(quiz));
    }

    @Override @Transactional
    public void deleteQuiz(Long quizId) {
        quizRepository.delete(findQuizOrThrow(quizId));
        log.info("Quiz deleted: id={}", quizId);
    }

    // ── Question Management ───────────────────────────────────────────────────

    @Override @Transactional
    public AssessmentDto.QuestionResponse addQuestion(Long quizId, AssessmentDto.QuestionRequest req) {
        Quiz quiz = findQuizOrThrow(quizId);
        int order = req.getOrderIndex() != null ? req.getOrderIndex() : quiz.getQuestions().size() + 1;
        Question q = Question.builder()
                .quiz(quiz).text(req.getText()).type(req.getType())
                .options(req.getOptions() != null ? req.getOptions() : List.of())
                .correctAnswer(req.getCorrectAnswer())
                .marks(req.getMarks() != null ? req.getMarks() : 1).orderIndex(order).build();
        quiz.getQuestions().add(q);
        Quiz saved = quizRepository.save(quiz);
        Question savedQ = saved.getQuestions().stream()
                .filter(question -> question.getText().equals(req.getText())).findFirst().orElse(q);
        log.info("Question added to quiz {}", quizId);
        return toQuestionResponse(savedQ);
    }

    @Override @Transactional
    public AssessmentDto.QuestionResponse updateQuestion(Long questionId, AssessmentDto.QuestionRequest req) {
        Question q = findQuestionOrThrow(questionId);
        if (req.getText() != null)          q.setText(req.getText());
        if (req.getType() != null)          q.setType(req.getType());
        if (req.getOptions() != null)       q.setOptions(req.getOptions());
        if (req.getCorrectAnswer() != null) q.setCorrectAnswer(req.getCorrectAnswer());
        if (req.getMarks() != null)         q.setMarks(req.getMarks());
        Quiz saved = quizRepository.save(q.getQuiz());
        return toQuestionResponse(q);
    }

    @Override @Transactional
    public void deleteQuestion(Long questionId) {
        Question q = findQuestionOrThrow(questionId);
        q.getQuiz().getQuestions().remove(q);
        quizRepository.save(q.getQuiz());
    }

    @Override
    public List<AssessmentDto.QuestionForStudent> getQuestionsForStudent(Long quizId) {
        return findQuizOrThrow(quizId).getQuestions().stream()
                .map(q -> AssessmentDto.QuestionForStudent.builder()
                        .questionId(q.getQuestionId()).text(q.getText())
                        .type(q.getType().name()).options(q.getOptions())
                        .marks(q.getMarks()).orderIndex(q.getOrderIndex()).build())
                .collect(Collectors.toList());
    }

    @Override
    public List<AssessmentDto.QuestionResponse> getQuestionsForInstructor(Long quizId) {
        return findQuizOrThrow(quizId).getQuestions().stream()
                .map(this::toQuestionResponse).collect(Collectors.toList());
    }

    // ── Attempts ──────────────────────────────────────────────────────────────

    @Override @Transactional
    public AssessmentDto.AttemptResponse startAttempt(Long studentId, Long quizId) {
        Quiz quiz = findQuizOrThrow(quizId);
        long existingAttempts = attemptRepository.countByStudentIdAndQuizId(studentId, quizId);
        if (existingAttempts >= quiz.getMaxAttempts()) {
            throw new AttemptLimitExceededException(
                    "Max attempts (" + quiz.getMaxAttempts() + ") reached for quiz " + quizId);
        }
        Attempt attempt = Attempt.builder().quizId(quizId).studentId(studentId).build();
        Attempt saved = attemptRepository.save(attempt);
        log.info("Attempt started: student={} quiz={} attemptId={}", studentId, quizId, saved.getAttemptId());
        return toAttemptResponse(saved, quiz.getPassingScore());
    }

    @Override @Transactional
    public AssessmentDto.AttemptResponse submitAttempt(Long studentId, AssessmentDto.SubmitAttemptRequest req) {
        Quiz quiz = findQuizOrThrow(req.getQuizId());
        List<Question> questions = quiz.getQuestions();
        Map<Long, String> answers = req.getAnswers() != null ? req.getAnswers() : Map.of();

        // Auto-grade
        int totalMarks = questions.stream().mapToInt(Question::getMarks).sum();
        int earned = 0;
        for (Question q : questions) {
            String studentAnswer = answers.get(q.getQuestionId());
            if (studentAnswer != null && studentAnswer.trim().equalsIgnoreCase(q.getCorrectAnswer().trim())) {
                earned += q.getMarks();
            }
        }
        double score = totalMarks > 0 ? Math.round((earned * 100.0 / totalMarks) * 10.0) / 10.0 : 0.0;
        boolean passed = score >= quiz.getPassingScore();

        // Find the most recent in-progress attempt for this student
        List<Attempt> existing = attemptRepository.findByStudentIdAndQuizId(studentId, req.getQuizId());
        Attempt attempt = existing.stream()
                .filter(a -> a.getSubmittedAt() == null)
                .findFirst()
                .orElseGet(() -> Attempt.builder().quizId(req.getQuizId()).studentId(studentId).build());

        attempt.setScore(score);
        attempt.setPassed(passed);
        attempt.setAnswers(answers);
        attempt.setSubmittedAt(LocalDateTime.now());

        Attempt saved = attemptRepository.save(attempt);
        log.info("Attempt submitted: student={} quiz={} score={} passed={}", studentId, req.getQuizId(), score, passed);
        return toAttemptResponse(saved, quiz.getPassingScore());
    }

    @Override
    public List<AssessmentDto.AttemptResponse> getAttemptsByStudent(Long studentId) {
        return attemptRepository.findByStudentId(studentId).stream()
                .map(a -> toAttemptResponse(a, null)).collect(Collectors.toList());
    }

    @Override
    public List<AssessmentDto.AttemptResponse> getAttemptsByQuiz(Long quizId) {
        return attemptRepository.findByQuizId(quizId).stream()
                .map(a -> toAttemptResponse(a, null)).collect(Collectors.toList());
    }

    @Override
    public AssessmentDto.AttemptResponse getBestScore(Long studentId, Long quizId) {
        Quiz quiz = findQuizOrThrow(quizId);
        Attempt best = attemptRepository.findTopScoreByStudentAndQuiz(studentId, quizId)
                .orElseThrow(() -> new QuizNotFoundException("No attempts found for student=" + studentId + " quiz=" + quizId));
        return toAttemptResponse(best, quiz.getPassingScore());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Quiz findQuizOrThrow(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new QuizNotFoundException("Quiz not found: " + id));
    }

    private Question findQuestionOrThrow(Long id) {
        return quizRepository.findAll().stream()
                .flatMap(q -> q.getQuestions().stream())
                .filter(q -> q.getQuestionId().equals(id))
                .findFirst()
                .orElseThrow(() -> new QuestionNotFoundException("Question not found: " + id));
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private AssessmentDto.QuizResponse toQuizResponse(Quiz q) {
        return AssessmentDto.QuizResponse.builder()
                .quizId(q.getQuizId()).courseId(q.getCourseId()).lessonId(q.getLessonId())
                .title(q.getTitle()).description(q.getDescription())
                .timeLimitMinutes(q.getTimeLimitMinutes()).passingScore(q.getPassingScore())
                .maxAttempts(q.getMaxAttempts()).isPublished(q.getIsPublished())
                .questionCount(q.getQuestions() != null ? q.getQuestions().size() : 0)
                .createdAt(q.getCreatedAt()).build();
    }

    private AssessmentDto.QuestionResponse toQuestionResponse(Question q) {
        return AssessmentDto.QuestionResponse.builder()
                .questionId(q.getQuestionId())
                .quizId(q.getQuiz() != null ? q.getQuiz().getQuizId() : null)
                .text(q.getText()).type(q.getType().name()).options(q.getOptions())
                .correctAnswer(q.getCorrectAnswer()).marks(q.getMarks()).orderIndex(q.getOrderIndex()).build();
    }

    private AssessmentDto.AttemptResponse toAttemptResponse(Attempt a, Double passingScore) {
        return AssessmentDto.AttemptResponse.builder()
                .attemptId(a.getAttemptId()).quizId(a.getQuizId()).studentId(a.getStudentId())
                .score(a.getScore()).passed(a.getPassed()).passingScore(passingScore)
                .answers(a.getAnswers()).startedAt(a.getStartedAt()).submittedAt(a.getSubmittedAt()).build();
    }
}
