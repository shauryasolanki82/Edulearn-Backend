package com.edulearn.assessment.repository;

import com.edulearn.assessment.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    List<Attempt> findByStudentId(Long studentId);
    List<Attempt> findByQuizId(Long quizId);
    List<Attempt> findByStudentIdAndQuizId(Long studentId, Long quizId);
    long countByStudentIdAndQuizId(Long studentId, Long quizId);

    @Query("SELECT a FROM Attempt a WHERE a.studentId=:sid AND a.quizId=:qid ORDER BY a.score DESC")
    Optional<Attempt> findTopScoreByStudentAndQuiz(@Param("sid") Long studentId, @Param("qid") Long quizId);
}
