package com.edulearn.assessment.service;

import com.edulearn.assessment.dto.AssessmentDto;
import java.util.List;

public interface AssessmentService {
    AssessmentDto.QuizResponse    createQuiz(AssessmentDto.QuizRequest request);
    AssessmentDto.QuizResponse    getQuizById(Long quizId);
    List<AssessmentDto.QuizResponse> getQuizzesByCourse(Long courseId);
    AssessmentDto.QuizResponse    updateQuiz(Long quizId, AssessmentDto.QuizRequest request);
    AssessmentDto.QuizResponse    publishQuiz(Long quizId, boolean publish);
    void                          deleteQuiz(Long quizId);
    AssessmentDto.QuestionResponse addQuestion(Long quizId, AssessmentDto.QuestionRequest request);
    AssessmentDto.QuestionResponse updateQuestion(Long questionId, AssessmentDto.QuestionRequest request);
    void                          deleteQuestion(Long questionId);
    List<AssessmentDto.QuestionForStudent> getQuestionsForStudent(Long quizId);
    List<AssessmentDto.QuestionResponse>   getQuestionsForInstructor(Long quizId);
    AssessmentDto.AttemptResponse startAttempt(Long studentId, Long quizId);
    AssessmentDto.AttemptResponse submitAttempt(Long studentId, AssessmentDto.SubmitAttemptRequest request);
    List<AssessmentDto.AttemptResponse> getAttemptsByStudent(Long studentId);
    List<AssessmentDto.AttemptResponse> getAttemptsByQuiz(Long quizId);
    AssessmentDto.AttemptResponse getBestScore(Long studentId, Long quizId);
}
