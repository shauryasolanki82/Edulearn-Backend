package com.edulearn.discussion.repository;

import com.edulearn.discussion.entity.DiscussionThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ThreadRepository extends JpaRepository<DiscussionThread, Long> {
    List<DiscussionThread> findByCourseIdOrderByIsPinnedDescCreatedAtDesc(Long courseId);
    List<DiscussionThread> findByLessonIdOrderByCreatedAtDesc(Long lessonId);
    List<DiscussionThread> findByAuthorId(Long authorId);
    List<DiscussionThread> findByIsPinnedTrue();

    @Query("SELECT t FROM DiscussionThread t WHERE t.courseId=:cid AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%',:kw,'%')) OR LOWER(t.body) LIKE LOWER(CONCAT('%',:kw,'%')))")
    List<DiscussionThread> searchByKeyword(@Param("cid") Long courseId, @Param("kw") String keyword);
}
